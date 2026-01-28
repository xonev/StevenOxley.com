
#!/usr/bin/env bb


(ns rss-syndicate
  (:require [babashka.http-client :as http]
            [babashka.process :as process]
            [org.httpkit.server :as server]
            [clojure.data.xml :as xml]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.browse :as browse]
            [cheshire.core :as json])
  (:import [java.security MessageDigest SecureRandom]
           [java.util Base64]))

;; Configuration file path
(def config-file (str (System/getProperty "user.home") "/.rss-syndicate-config.edn"))

;; Platform character limits
(def char-limits
  {:x 280
   :bluesky 300
   :mastodon 500
   :linkedin 3000
   :facebook 63206
   :instagram 2200})

;; OAuth callback port
(def callback-port 8080)
(def callback-url (str "http://localhost:" callback-port "/callback"))

;; Load configuration
(defn load-config []
  (if (.exists (io/file config-file))
    (read-string (slurp config-file))
    {}))

;; Save configuration
(defn save-config [config]
  (spit config-file (pr-str config)))

;; OAuth 2.0 PKCE helpers
(defn generate-random-string [length]
  (let [chars "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        random (SecureRandom.)
        sb (StringBuilder.)]
    (dotimes [_ length]
      (.append sb (.charAt chars (.nextInt random (count chars)))))
    (.toString sb)))

(defn base64-url-encode [bytes]
  (-> (Base64/getUrlEncoder)
      (.withoutPadding)
      (.encodeToString bytes)))

(defn sha256 [text]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (.digest digest (.getBytes text "UTF-8"))))

(defn generate-pkce-pair []
  (let [verifier (generate-random-string 128)
        challenge (base64-url-encode (sha256 verifier))]
    {:verifier verifier
     :challenge challenge}))

;; OAuth 2.0 callback server
;; OAuth 2.0 callback server

(defn start-callback-server [promise-atom]
  (server/run-server
    (fn [request]
      (if (= (:uri request) "/callback")
        (let [query-params (when-let [query (:query-string request)]
                             (into {} (map (fn [pair]
                                            (let [[k v] (str/split pair #"=" 2)]
                                              [k (when v (java.net.URLDecoder/decode v "UTF-8"))]))
                                           (str/split query #"&"))))
              code (get query-params "code")
              state (get query-params "state")
              error (get query-params "error")]
          (deliver promise-atom {:code code :state state :error error})
          {:status 200
           :headers {"Content-Type" "text/html"}
           :body (if error
                   (str
                    "<html><body><h1>Authorization Failed</h1><p>Error: " error "</p>"
                    "<p>You can close this window.</p></body></html>")
                   (str
                    "<html><body><h1>Authorization Successful!</h1>"
                    "<p>You can close this window and return to the terminal.</p></body></html>"))})
        {:status 404 :body "Not found"}))
    {:port callback-port}))

;; X OAuth 2.0 authentication
(defn authenticate-x-oauth2 [config]
  (println "\n=== X OAuth 2.0 Authentication ===")
  (let [client-id (or (get-in config [:x :client-id])
                      (do (print "Enter X Client ID: ") (flush) (str/trim (read-line))))
        client-secret (or (get-in config [:x :client-secret])
                          (do (print "Enter X Client Secret (optional for public clients): ") (flush) (str/trim (read-line))))
        pkce (generate-pkce-pair)
        state (generate-random-string 32)
        auth-url (str "https://twitter.com/i/oauth2/authorize?"
                      "response_type=code&"
                      "client_id=" client-id "&"
                      "redirect_uri=" (java.net.URLEncoder/encode callback-url "UTF-8") "&"
                      "scope=" (java.net.URLEncoder/encode "tweet.read tweet.write users.read offline.access media.write" "UTF-8") "&"
                      "state=" state "&"
                      "code_challenge=" (:challenge pkce) "&"
                      "code_challenge_method=S256")
        promise-atom (promise)]

    (println "Opening browser for authorization...")
    (println "If browser doesn't open, visit: " auth-url)

    ;; Start callback server
    (let [server (start-callback-server promise-atom)]
      (try
        ;; Open browser
        (browse/browse-url auth-url)

        ;; Wait for callback
        (println "Waiting for authorization callback...")
        (let [callback-result (deref promise-atom 300000 :timeout)] ; 5 minute timeout

          (server) ; stop the server

          (cond
            (= callback-result :timeout)
            (do (println "Authorization timed out")
                nil)

            (:error callback-result)
            (do (println "Authorization error: " (:error callback-result))
                nil)

            (not= (:state callback-result) state)
            (do (println "State mismatch - possible CSRF attack")
                nil)

            :else
            (let [code (:code callback-result)
                  ;; Exchange code for tokens
                  token-params {:grant_type "authorization_code"
                                :code code
                                :redirect_uri callback-url
                                :code_verifier (:verifier pkce)}
                  token-params (if (not-empty client-secret)
                                 token-params
                                 (assoc token-params :client_id client-id))
                  auth-header (if (not-empty client-secret)
                                {"Authorization" (str "Basic "
                                                      (.encodeToString (Base64/getEncoder)
                                                                       (.getBytes (str client-id ":" client-secret) "UTF-8")))}
                                {})
                  response (http/post "https://api.twitter.com/2/oauth2/token"
                                      {:headers (merge {"Content-Type" "application/x-www-form-urlencoded"}
                                                       auth-header)
                                       :body (str/join "&"
                                                       (map (fn [[k v]]
                                                              (str (name k) "="
                                                                   (java.net.URLEncoder/encode (str v) "UTF-8")))
                                                            token-params))})]
              (if (= 200 (:status response))
                (let [tokens (json/parse-string (:body response) true)]
                  (println "✓ X OAuth 2.0 authentication successful!")
                  {:client-id client-id
                   :client-secret client-secret
                   :access-token (:access_token tokens)
                   :refresh-token (:refresh_token tokens)
                   :expires-at (+ (System/currentTimeMillis) (* 1000 (:expires_in tokens)))})
                (do
                  (println "\n=== Token Exchange Failed ===")
                  (println "Status Code:" (:status response))
                  (println "Response Headers:" (:headers response))
                  (println "Response Body:" (:body response))
                  nil)))))
        (catch Exception e
          (server)
          (println "\n=== Authentication Error ===")
          (println "Error message:" (.getMessage e))
          (println "Error type:" (type e))
          (when (instance? clojure.lang.ExceptionInfo e)
            (println "Error data:" (ex-data e)))
          nil)))))

;; LinkedIn OAuth 2.0 authentication
(defn authenticate-linkedin-oauth2 [config]
  (println "\n=== LinkedIn OAuth 2.0 Authentication ===")
  (let [client-id (or (get-in config [:linkedin :client-id])
                      (do (print "Enter LinkedIn Client ID: ") (flush) (str/trim (read-line))))
        client-secret (or (get-in config [:linkedin :client-secret])
                          (do (print "Enter LinkedIn Client Secret: ") (flush) (str/trim (read-line))))
        state (generate-random-string 32)
        auth-url (str "https://www.linkedin.com/oauth/v2/authorization?"
                      "response_type=code&"
                      "client_id=" client-id "&"
                      "redirect_uri=" (java.net.URLEncoder/encode callback-url "UTF-8") "&"
                      "state=" state "&"
                      "scope=" (java.net.URLEncoder/encode "openid profile w_member_social" "UTF-8"))
        promise-atom (promise)]

    (println "Opening browser for authorization...")
    (println "If browser doesn't open, visit: " auth-url)

    ;; Start callback server
    (let [server (start-callback-server promise-atom)]
      (try
        ;; Open browser
        (browse/browse-url auth-url)

        ;; Wait for callback
        (println "Waiting for authorization callback...")
        (let [callback-result (deref promise-atom 300000 :timeout)] ; 5 minute timeout

          (server)

          (cond
            (= callback-result :timeout)
            (do (println "Authorization timed out")
                nil)

            (:error callback-result)
            (do (println "Authorization error: " (:error callback-result))
                nil)

            (not= (:state callback-result) state)
            (do (println "State mismatch - possible CSRF attack")
                nil)

            :else
            (let [code (:code callback-result)
                  ;; Exchange code for tokens
                  response (http/post "https://www.linkedin.com/oauth/v2/accessToken"
                                      {:headers {"Content-Type" "application/x-www-form-urlencoded"}
                                       :body (str/join "&"
                                                       [(str "grant_type=authorization_code")
                                                        (str "code=" (java.net.URLEncoder/encode code "UTF-8"))
                                                        (str "redirect_uri=" (java.net.URLEncoder/encode callback-url "UTF-8"))
                                                        (str "client_id=" (java.net.URLEncoder/encode client-id "UTF-8"))
                                                        (str "client_secret=" (java.net.URLEncoder/encode client-secret "UTF-8"))])})]
              (if (= 200 (:status response))
                (let [tokens (json/parse-string (:body response) true)
                      access-token (:access_token tokens)
                      ;; Fetch user profile to get member ID
                      profile-response (http/get "https://api.linkedin.com/v2/userinfo"
                                                 {:headers {"Authorization" (str "Bearer " access-token)}})
                      profile (when (= 200 (:status profile-response))
                                (json/parse-string (:body profile-response) true))
                      member-id (:sub profile)]
                  (if member-id
                    (do
                      (println "✓ LinkedIn OAuth 2.0 authentication successful!")
                      {:client-id client-id
                       :client-secret client-secret
                       :access-token access-token
                       :member-id member-id
                       :expires-at (+ (System/currentTimeMillis) (* 1000 (:expires_in tokens)))})
                    (do
                      (println "Failed to fetch LinkedIn member ID")
                      nil)))
                (do
                  (println "Token exchange failed: " (:status response) " - " (:body response))
                  nil)))))
        (catch Exception e
          (server)
          (println "\n=== Authentication Error ===")
          (println "Error message:" (.getMessage e))
          (println "Error type:" (type e))
          (when (instance? clojure.lang.ExceptionInfo e)
            (println "Error data:" (ex-data e)))
          nil)))))

;; Refresh X OAuth 2.0 token
(defn refresh-x-token [config]
  (let [{:keys [client-id client-secret refresh-token]} (get config :x)]
    (when refresh-token
      (let [auth-header (if client-secret
                          {"Authorization" (str "Basic "
                                                (.encodeToString (Base64/getEncoder)
                                                                 (.getBytes (str client-id ":" client-secret) "UTF-8")))}
                          {})
            response (http/post "https://api.twitter.com/2/oauth2/token"
                                {:headers (merge {"Content-Type" "application/x-www-form-urlencoded"}
                                                 auth-header)
                                 :body (str "grant_type=refresh_token&refresh_token="
                                            (java.net.URLEncoder/encode refresh-token "UTF-8")
                                            (when-not client-secret
                                              (str "&client_id=" client-id)))})]
        (when (= 200 (:status response))
          (let [tokens (json/parse-string (:body response) true)]
            (merge (get config :x)
                   {:access-token (:access_token tokens)
                    :refresh-token (:refresh_token tokens)
                    :expires-at (+ (System/currentTimeMillis) (* 1000 (:expires_in tokens)))})))))))

;; Helper to get local name from potentially namespaced tag
(defn local-name [tag]
  (if (keyword? tag)
    (let [tag-str (name tag)
          parts (str/split tag-str #"/")]
      (keyword (last parts)))
    tag))

(defn parse-rss-item [item]
  (let [get-text (fn [tag]
                   (some #(when (= tag (local-name (:tag %)))
                            (first (:content %)))
                         (:content item)))]
    {:title (get-text :title)
     :link (get-text :link)
     :description (get-text :description)
     :pub-date (get-text :pubDate)
     :enclosure (some #(when (= :enclosure (local-name (:tag %)))
                         (:attrs %))
                      (:content item))}))

(defn parse-atom-entry [entry]
  (let [get-text (fn [tag]
                   (some #(when (= tag (local-name (:tag %)))
                            (first (:content %)))
                         (:content entry)))
        link (some #(when (and (= :link (local-name (:tag %)))
                               (= "alternate" (get-in % [:attrs :rel])))
                      (get-in % [:attrs :href]))
                   (:content entry))]
    {:title (get-text :title)
     :link link
     :description (or (get-text :summary) (get-text :content))
     :pub-date (get-text :published)
     :enclosure nil}))

;; Parse RSS/Atom feed
(defn parse-feed [url]
  (let [response (http/get url)
        feed-xml (xml/parse-str (:body response))
        root-tag (local-name (:tag feed-xml))]
    (cond
      ;; RSS 2.0
      (= :rss root-tag)
      (let [channel (first (filter #(= :channel (local-name (:tag %))) (:content feed-xml)))
            items (filter #(= :item (local-name (:tag %))) (:content channel))]
        {:type :rss
         :items (map parse-rss-item items)})

      ;; Atom
      (= :feed root-tag)
      {:type :atom
       :items (map parse-atom-entry
                   (filter #(= :entry (local-name (:tag %))) (:content feed-xml)))}

      :else
      (throw (Exception. "Unknown feed format")))))

;; Superscript digits for footnotes
(def superscript-digits [\u2070 \u00B9 \u00B2 \u00B3 \u2074 \u2075 \u2076 \u2077 \u2078 \u2079])

(defn number-to-superscript [n]
  (apply str (map #(nth superscript-digits (Character/digit % 10)) (str n))))

;; Extract links and convert to footnote format
(defn process-html-with-footnotes [s]
  (let [;; First decode HTML entities
        decoded (-> s
                    (str/replace #"&nbsp;" " ")
                    (str/replace #"&amp;" "&")
                    (str/replace #"&lt;" "<")
                    (str/replace #"&gt;" ">")
                    (str/replace #"&quot;" "\""))
        ;; Find all anchor tags with href and text
        link-pattern #"<a\s+[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>"
        links (atom [])
        footnote-counter (atom 0)
        ;; Replace links with text and footnote markers
        processed (str/replace decoded link-pattern
                               (fn [[_ href text]]
                                 (let [clean-text (str/replace text #"<[^>]+>" "")] ;; Strip any inner tags
                                   (if (= clean-text href)
                                     ;; Link text is same as href, just use the text
                                     clean-text
                                     ;; Link text differs from href, add footnote
                                     (do
                                       (swap! footnote-counter inc)
                                       (swap! links conj {:num @footnote-counter :href href})
                                       (str clean-text (number-to-superscript @footnote-counter)))))))
        ;; Strip remaining HTML tags
        cleaned (str/replace processed #"</?[a-zA-Z][^>]*>" "")
        ;; Build footnotes section
        footnotes (when (seq @links)
                    (str/join "\n" (map (fn [{:keys [num href]}]
                                          (str (number-to-superscript num) href))
                                        @links)))]
    {:text cleaned
     :footnotes footnotes}))

;; Strip HTML tags (legacy function for compatibility)
(defn strip-html [s]
  (:text (process-html-with-footnotes s)))

;; Image extraction and handling
(def supported-image-types
  #{"image/jpeg" "image/jpg" "image/png" "image/gif" "image/webp"})

(defn extract-images-from-html
  "Extracts image URLs from HTML content <img> tags.
   Returns a vector of {:url ... :alt ...} maps."
  [html-content]
  (when html-content
    (let [img-pattern #"<img\s+[^>]*src=[\"']([^\"']+)[\"'][^>]*>"
          alt-pattern #"alt=[\"']([^\"']*)[\"']"
          matches (re-seq img-pattern html-content)]
      (->> matches
           (map (fn [[full-match src]]
                  (let [alt (second (re-find alt-pattern full-match))]
                    {:url src :alt (or alt "")})))
           ;; Filter out likely tracking pixels (1x1 images, spacer.gif, etc.)
           (filter #(not (re-find #"(?i)(spacer|pixel|tracking|1x1|blank\.)" (:url %))))
           vec))))

(defn resolve-url
  "Resolves a potentially relative URL against a base URL.
   Returns the resolved absolute URL string."
  [base-url relative-url]
  (try
    (str (.resolve (java.net.URI. base-url) relative-url))
    (catch Exception _
      relative-url)))

(defn extract-all-images
  "Extracts images from an RSS item, combining enclosures and HTML images.
   base-url is used to resolve relative image URLs.
   Returns a vector of {:url ... :alt ... :type ...} maps."
  [rss-item base-url]
  (let [;; Get enclosure image if present and is image type
        enclosure-images (when-let [enc (:enclosure rss-item)]
                           (when (and (:url enc)
                                      (or (nil? (:type enc))
                                          (str/starts-with? (or (:type enc) "") "image/")))
                             [{:url (:url enc) :alt "" :type (:type enc)}]))
        ;; Extract images from HTML description
        html-images (extract-images-from-html (:description rss-item))
        ;; Combine and deduplicate by URL
        all-images (concat enclosure-images html-images)
        seen-urls (atom #{})]
    (->> all-images
         (filter (fn [{:keys [url]}]
                   (when-not (@seen-urls url)
                     (swap! seen-urls conj url)
                     true)))
         ;; Resolve relative URLs against the base URL
         (mapv (fn [img]
                 (update img :url #(resolve-url base-url %)))))))

(defn download-image
  "Downloads an image from URL and returns local file info.
   Returns {:path <temp-file-path> :type <mime-type> :size <bytes>} or nil on failure."
  [image-url]
  (try
    (println (str "  Downloading: " image-url))
    (let [response (http/get image-url
                             {:headers {"User-Agent" "Mozilla/5.0 RSS-Syndicate/1.0"
                                        "Accept" "image/*"}
                              :as :bytes})
          content-type (get-in response [:headers "content-type"])
          ;; Handle content-type with charset suffix
          mime-type (first (str/split (or content-type "image/jpeg") #";"))
          body (:body response)
          ;; Determine file extension
          ext (case mime-type
                "image/jpeg" ".jpg"
                "image/jpg" ".jpg"
                "image/png" ".png"
                "image/gif" ".gif"
                "image/webp" ".webp"
                ".jpg")
          temp-file (java.io.File/createTempFile "rss-syndicate-" ext)]
      (with-open [out (io/output-stream temp-file)]
        (.write out body))
      {:path (.getAbsolutePath temp-file)
       :type mime-type
       :size (count body)
       :url image-url})
    (catch Exception e
      (println (str "  Warning: Failed to download image: " image-url))
      (println (str "  Reason: " (.getMessage e)))
      nil)))

(defn download-images
  "Downloads multiple images, returning successfully downloaded ones.
   Respects max-count limit."
  [image-infos max-count]
  (when (seq image-infos)
    (->> image-infos
         (take max-count)
         (map #(download-image (:url %)))
         (filter some?)
         vec)))

(defn cleanup-temp-images
  "Removes temporary image files after posting."
  [image-files]
  (doseq [{:keys [path]} image-files]
    (try
      (io/delete-file path true)
      (catch Exception _ nil))))

;; Bluesky has a 1MB limit per image - this function resizes images that are too large
(def bluesky-max-size (* 1000 1000)) ;; 1MB in bytes

(defn- find-resize-tool
  "Finds an available image resize tool. Returns :sips (macOS) or :convert (ImageMagick) or nil."
  []
  (cond
    (= 0 (:exit (process/process ["which" "sips"] {:out :string :err :string})))
    :sips
    (= 0 (:exit (process/process ["which" "convert"] {:out :string :err :string})))
    :convert
    :else nil))

(defn resize-image-for-bluesky
  "Resizes an image to fit within Bluesky's 1MB limit using external tools (sips on macOS, ImageMagick convert).
   Returns updated image info with new path and size, or original if already small enough."
  [{:keys [path type size] :as image-info}]
  (if (<= size bluesky-max-size)
    image-info
    (let [tool (find-resize-tool)]
      (if-not tool
        (do (println "  Warning: No image resize tool found (sips or ImageMagick convert). Skipping resize.")
            image-info)
        (try
          (println (str "  Resizing image for Bluesky (was " (format "%.1f" (/ size 1024.0)) " KB)..."))
          ;; Try progressively smaller dimensions until under 1MB
          (let [temp-path (.getAbsolutePath (java.io.File/createTempFile "rss-syndicate-resized-" ".jpg"))]
            (loop [scale-pct 85]
              (let [resize-result
                    (case tool
                      :sips
                      ;; sips copies first, then resizes in place
                      (do (io/copy (io/file path) (io/file temp-path))
                          (process/process
                           ["sips" "--resampleHeightWidthMax"
                            (str (int (* (/ scale-pct 100.0) 2048)))
                            "-s" "formatOptions" "80"
                            temp-path]
                           {:out :string :err :string}))
                      :convert
                      (process/process
                       ["convert" path
                        "-resize" (str scale-pct "%")
                        "-quality" "80"
                        temp-path]
                       {:out :string :err :string}))
                    new-size (.length (io/file temp-path))]
                (cond
                  (<= new-size bluesky-max-size)
                  (do (println (str "  ✓ Resized to " (format "%.1f" (/ new-size 1024.0)) " KB"))
                      {:path temp-path
                       :type "image/jpeg"
                       :size new-size
                       :url (:url image-info)})

                  (> scale-pct 20)
                  (recur (- scale-pct 15))

                  :else
                  (do (println "  Warning: Could not resize image under 1MB, using as-is")
                      (io/delete-file temp-path true)
                      image-info)))))
          (catch Exception e
            (println (str "  Warning: Failed to resize image: " (.getMessage e)))
            image-info))))))

(defn split-into-threads [text link max-chars]
  (let [thread-marker " [→]"
        link-suffix (str "\n\n" link)
        marker-length (count thread-marker)
        ;; Split text into paragraphs, preserving line breaks
        paragraphs (str/split text #"\n+")

        ;; Helper function to split a single paragraph into words if needed
        split-paragraph (fn [paragraph max-available]
                          (let [words (str/split paragraph #"\s+")]
                            (loop [remaining words
                                   current []
                                   chunks []]
                              (if (empty? remaining)
                                (if (empty? current)
                                  chunks
                                  (conj chunks (str/join " " current)))
                                (let [word (first remaining)
                                      current-text (str/join " " current)
                                      new-text (if (empty? current) word (str current-text " " word))]
                                  (if (> (count new-text) max-available)
                                    (recur (rest remaining)
                                           [word]
                                           (conj chunks current-text))
                                    (recur (rest remaining)
                                           (conj current word)
                                           chunks)))))))

        ;; Build threads from paragraphs
        build-threads (fn []
                        (loop [remaining paragraphs
                               current-parts []
                               threads []]
                          (if (empty? remaining)
                            ;; Flush any remaining content
                            (if (empty? current-parts)
                              threads
                              (conj threads (str/join "\n\n" current-parts)))
                            (let [para (first remaining)
                                  current-text (str/join "\n\n" current-parts)
                                  ;; Reserve space for marker on all but last thread
                                  max-available (- max-chars marker-length 5)
                                  new-text (if (empty? current-parts)
                                            para
                                            (str current-text "\n\n" para))]
                              (cond
                                ;; Paragraph fits in current thread
                                (<= (count new-text) max-available)
                                (recur (rest remaining)
                                       (conj current-parts para)
                                       threads)

                                ;; Start new thread with this paragraph if current is not empty
                                (and (not (empty? current-parts)) (<= (count para) max-available))
                                (recur (rest remaining)
                                       [para]
                                       (conj threads current-text))

                                ;; Paragraph is too long, need to split it
                                :else
                                (let [chunks (split-paragraph para max-available)]
                                  (if (empty? current-parts)
                                    ;; Add first chunk to current, rest become new paragraphs
                                    (recur (concat (rest chunks) (rest remaining))
                                           [(first chunks)]
                                           threads)
                                    ;; Flush current thread, then process chunks
                                    (recur (concat chunks (rest remaining))
                                           []
                                           (conj threads current-text)))))))))]

    ;; Add markers and link
    (let [all-threads (build-threads)
          thread-count (count all-threads)]
      (if (= thread-count 1)
        ;; Single thread - just add link
        [(str (first all-threads) link-suffix)]
        ;; Multiple threads - add markers to all but last, link to last
        (vec (concat
               (map #(str % thread-marker) (take (dec thread-count) all-threads))
               [(str (last all-threads) link-suffix)]))))))

;; Format content for different platforms
(defn format-content [content link platform]
  (let [{:keys [text footnotes]} (process-html-with-footnotes content)
        ;; Combine text with footnotes if present
        full-content (if footnotes
                       (str text "\n" footnotes)
                       text)
        max-chars (get char-limits platform)]
    (case platform
      (:x :bluesky :mastodon)
      (let [link-suffix (str "\n\n" link)
            available-chars (- max-chars (count link-suffix))
            threads (if (<= (count full-content) available-chars)
                      [(str full-content link-suffix)]
                      (split-into-threads full-content link max-chars))]
        {:threads threads})

      (:linkedin :facebook)
      {:content (str full-content "\n\n" link)}

      :instagram
      {:content (if (> (count full-content) (:instagram char-limits))
                  (str (subs full-content 0 (- (:instagram char-limits) 3)) "...")
                  full-content)
       :link link})))

;; Platform media upload functions

;; X (Twitter) media upload
(defn upload-media-to-x
  "Uploads an image to X using the v2 media upload API.
   Returns media_id_string or nil on failure."
  [{:keys [path type size]} x-config]
  (try
    (println (str "  Uploading image to X (" (format "%.1f" (/ size 1024.0)) " KB)..."))
    (let [;; Use simple upload for images (works for < 5MB)
          response (http/post "https://api.x.com/2/media/upload"
                              {:headers {"Authorization" (str "Bearer " (:access-token x-config))}
                               :multipart [{:name "media"
                                            :content (io/file path)
                                            :content-type type
                                            :file-name (last (str/split path #"/"))}
                                           {:name "media_category"
                                            :content "tweet_image"}]
                               :throw false})]
      (if (#{200 201 202} (:status response))
        (let [result (json/parse-string (:body response) true)
              ;; v2 nests under :data with field :id
              data (or (:data result) result)
              media-id (or (:media_id_string data)
                           (:id data)
                           (when (:media_id data) (str (:media_id data))))]
          (if media-id
            (do (println (str "  ✓ Uploaded to X: " media-id))
                media-id)
            (do (println (str "  Warning: Could not extract media ID from response: " (:body response)))
                nil)))
        (do
          (println (str "  Failed to upload to X: " (:status response) " - " (:body response)))
          nil)))
    (catch Exception e
      (println (str "  Error uploading to X: " (.getMessage e)))
      nil)))

(defn upload-images-to-x
  "Uploads up to 4 images to X, returns vector of media_ids."
  [images x-config]
  (when (and (seq images) (:access-token x-config))
    (->> images
         (take 4)
         (map #(upload-media-to-x % x-config))
         (filter #(and (some? %) (not= "" %) (not= "null" %)))
         vec)))

;; LinkedIn media upload
(defn upload-image-to-linkedin
  "Uploads an image to LinkedIn using the Images API.
   Returns image URN or nil on failure."
  [{:keys [path type size]} linkedin-config]
  (try
    (println (str "  Uploading image to LinkedIn (" (format "%.1f" (/ size 1024.0)) " KB)..."))
    (let [member-id (:member-id linkedin-config)
          linkedin-version "202511"
          ;; Step 1: Initialize upload
          init-response (http/post "https://api.linkedin.com/rest/images?action=initializeUpload"
                                   {:headers {"Authorization" (str "Bearer " (:access-token linkedin-config))
                                              "Content-Type" "application/json"
                                              "LinkedIn-Version" linkedin-version
                                              "X-Restli-Protocol-Version" "2.0.0"}
                                    :body (json/generate-string
                                           {:initializeUploadRequest
                                            {:owner (str "urn:li:person:" member-id)}})
                                    :throw false})]
      (if (= 200 (:status init-response))
        (let [init-result (json/parse-string (:body init-response) true)
              upload-url (get-in init-result [:value :uploadUrl])
              image-urn (get-in init-result [:value :image])
              ;; Step 2: Upload the image bytes
              file-bytes (with-open [in (io/input-stream path)]
                           (.readAllBytes in))
              upload-response (http/put upload-url
                                        {:headers {"Authorization" (str "Bearer " (:access-token linkedin-config))
                                                   "Content-Type" type}
                                         :body file-bytes
                                         :throw false})]
          (if (#{200 201 204} (:status upload-response))
            (do
              (println (str "  ✓ Uploaded to LinkedIn: " image-urn))
              image-urn)
            (do
              (println (str "  Failed to upload image to LinkedIn: " (:status upload-response)))
              nil)))
        (do
          (println (str "  Failed to initialize LinkedIn upload: " (:status init-response) " - " (:body init-response)))
          nil)))
    (catch Exception e
      (println (str "  Error uploading to LinkedIn: " (.getMessage e)))
      nil)))

(defn upload-images-to-linkedin
  "Uploads images to LinkedIn, returns vector of image URNs."
  [images linkedin-config]
  (when (and (seq images) (:access-token linkedin-config))
    (->> images
         (take 20) ;; LinkedIn MultiImage supports up to 20
         (map #(upload-image-to-linkedin % linkedin-config))
         (filter some?)
         vec)))

;; Bluesky media upload
(defn upload-blob-to-bluesky
  "Uploads an image blob to Bluesky.
   Returns blob metadata for embedding or nil on failure."
  [{:keys [path type size] :as image-info} session]
  (try
    ;; First resize if needed for Bluesky's 1MB limit
    (let [{:keys [path type size]} (resize-image-for-bluesky image-info)]
      (println (str "  Uploading blob to Bluesky (" (format "%.1f" (/ size 1024.0)) " KB)..."))
      (let [file-bytes (with-open [in (io/input-stream path)]
                         (.readAllBytes in))
            response (http/post "https://bsky.social/xrpc/com.atproto.repo.uploadBlob"
                                {:headers {"Authorization" (str "Bearer " (:accessJwt session))
                                           "Content-Type" type}
                                 :body file-bytes
                                 :throw false})]
        (if (= 200 (:status response))
          (let [result (json/parse-string (:body response) true)
                blob (:blob result)]
            (println "  ✓ Uploaded blob to Bluesky")
            blob)
          (do
            (println (str "  Failed to upload blob to Bluesky: " (:status response) " - " (:body response)))
            nil))))
    (catch Exception e
      (println (str "  Error uploading to Bluesky: " (.getMessage e)))
      nil)))

(defn prepare-bluesky-image-embed
  "Prepares embed structure for Bluesky post with images."
  [blobs]
  (when (seq blobs)
    {:$type "app.bsky.embed.images"
     :images (mapv (fn [blob]
                     {:alt ""
                      :image blob})
                   blobs)}))

;; Mastodon media upload
(defn upload-media-to-mastodon
  "Uploads an image to Mastodon.
   Returns media attachment ID or nil on failure."
  [{:keys [path type size]} mastodon-config]
  (try
    (println (str "  Uploading media to Mastodon (" (format "%.1f" (/ size 1024.0)) " KB)..."))
    (let [{:keys [instance access-token]} mastodon-config
          response (http/post (str instance "/api/v2/media")
                              {:headers {"Authorization" (str "Bearer " access-token)}
                               :multipart [{:name "file"
                                            :content (io/file path)
                                            :content-type type
                                            :file-name (last (str/split path #"/"))}]
                               :throw false})]
      (if (#{200 202} (:status response))
        (let [result (json/parse-string (:body response) true)
              media-id (:id result)]
          (println (str "  ✓ Uploaded to Mastodon: " media-id))
          media-id)
        (do
          (println (str "  Failed to upload to Mastodon: " (:status response) " - " (:body response)))
          nil)))
    (catch Exception e
      (println (str "  Error uploading to Mastodon: " (.getMessage e)))
      nil)))

(defn upload-images-to-mastodon
  "Uploads up to 4 images to Mastodon, returns vector of media_ids."
  [images mastodon-config]
  (when (and (seq images) (:access-token mastodon-config))
    (->> images
         (take 4)
         (map #(upload-media-to-mastodon % mastodon-config))
         (filter some?)
         vec)))

;; Platform-specific posting functions
(defn post-to-x [content config images]
  (println "Posting to X...")
  (let [x-config (get config :x)
        ;; Check if token needs refresh
        x-config (if (and (:expires-at x-config)
                          (< (:expires-at x-config) (System/currentTimeMillis)))
                   (do
                     (println "Refreshing X access token...")
                     (refresh-x-token config))
                   x-config)]
    (if (:access-token x-config)
      (try
        ;; Upload images first (only for first tweet)
        (let [media-ids (when (seq images)
                          (upload-images-to-x images x-config))
              threads (:threads content)]
          (loop [remaining threads
                 previous-tweet-id nil
                 index 1]
            (when-let [tweet-text (first remaining)]
              (let [;; Only attach media to first tweet
                    tweet-data (cond-> {:text tweet-text}
                                 previous-tweet-id
                                 (assoc :reply {:in_reply_to_tweet_id previous-tweet-id})
                                 (and (= index 1) (seq media-ids))
                                 (assoc :media {:media_ids media-ids}))
                    response (http/post "https://api.twitter.com/2/tweets"
                                        {:headers {"Authorization" (str "Bearer " (:access-token x-config))
                                                   "Content-Type" "application/json"}
                                         :body (json/generate-string tweet-data)
                                         :throw false})]
                (if (= 201 (:status response))
                  (let [tweet-id (get-in (json/parse-string (:body response) true) [:data :id])]
                    (println (str "✓ Posted tweet " index " of " (count threads)))
                    (recur (rest remaining) tweet-id (inc index)))
                  (println (str "Failed to post tweet " index ": " (:status response) " - " (:body response))))))))
        (catch Exception e
          (println (str "Error posting to X: " (.getMessage e)))))
      (println "X OAuth 2.0 access token not configured. Run 'auth x' to authenticate."))))

(defn post-to-linkedin [content config images]
  (println "Posting to LinkedIn...")
  (let [linkedin-config (get config :linkedin)]
    (if (:access-token linkedin-config)
      (try
        ;; Use the stored member ID from authentication
        (let [member-id (:member-id linkedin-config)
              _ (when-not member-id
                  (println "LinkedIn member ID not found. Please re-authenticate with: bb rss-syndicate.clj auth linkedin"))
              person-urn (when member-id (str "urn:li:person:" member-id))
              ;; Use a known active LinkedIn API version (202511 = November 2025)
              ;; LinkedIn releases new versions monthly and supports them for 1 year
              linkedin-version "202511"
              ;; Upload images first
              image-urns (when (seq images)
                           (upload-images-to-linkedin images linkedin-config))
              ;; Build post body based on number of images
              post-body (cond-> {:author person-urn
                                 :commentary (:content content)
                                 :visibility "PUBLIC"
                                 :distribution {:feedDistribution "MAIN_FEED"
                                               :targetEntities []
                                               :thirdPartyDistributionChannels []}
                                 :lifecycleState "PUBLISHED"
                                 :isReshareDisabledByAuthor false}
                          ;; Single image: use media content
                          (= 1 (count image-urns))
                          (assoc :content {:media {:id (first image-urns)
                                                   :altText ""}})
                          ;; Multiple images (2-20): use multiImage
                          (> (count image-urns) 1)
                          (assoc :content {:multiImage
                                           {:images (mapv (fn [urn] {:id urn :altText ""})
                                                          image-urns)}}))
              response (when person-urn
                         (http/post "https://api.linkedin.com/rest/posts"
                                      {:headers {"Authorization" (str "Bearer " (:access-token linkedin-config))
                                                 "Content-Type" "application/json"
                                                 "X-Restli-Protocol-Version" "2.0.0"
                                                 "LinkedIn-Version" linkedin-version}
                                       :body (json/generate-string post-body)}))]
          (when response
            (if (= 201 (:status response))
              (println "✓ Posted to LinkedIn successfully")
              (do
                (println "\n=== LinkedIn Post Failed ===")
                (println "Status:" (:status response))
                (println "Response Headers:" (:headers response))
                (println "Response Body:" (:body response))))))
        (catch Exception e
          (println "\n=== LinkedIn Error ===")
          (println "Error message:" (.getMessage e))
          (println "Error type:" (type e))
          (when (instance? clojure.lang.ExceptionInfo e)
            (println "Error data:" (ex-data e)))))
      (println "LinkedIn OAuth 2.0 access token not configured. Run 'auth linkedin' to authenticate."))))

(defn post-to-bluesky [content config images]
  (let [{:keys [handle password]} (get config :bluesky)]
    (if (and handle password)
      ;; First, create session
      (let [session-resp (http/post "https://bsky.social/xrpc/com.atproto.server.createSession"
                                    {:headers {"Content-Type" "application/json"}
                                     :body (json/generate-string
                                            {:identifier handle
                                             :password password})})
            session (json/parse-string (:body session-resp) true)]
        (when (:accessJwt session)
          ;; Upload images as blobs (up to 4, with resize if needed)
          (let [blobs (when (seq images)
                        (->> images
                             (take 4)
                             (map #(upload-blob-to-bluesky % session))
                             (filter some?)
                             vec))
                embed (prepare-bluesky-image-embed blobs)
                threads (:threads content)]
            (loop [remaining threads
                   previous-post nil
                   index 1]
              (when-let [post-text (first remaining)]
                (let [;; Build base record
                      base-record {:text post-text
                                   :createdAt (str (java.time.Instant/now))}
                      ;; Add reply info if this is a reply
                      record-with-reply (if previous-post
                                          (assoc base-record :reply
                                                 {:root {:uri (:root-uri previous-post)
                                                        :cid (:root-cid previous-post)}
                                                  :parent {:uri (:uri previous-post)
                                                          :cid (:cid previous-post)}})
                                          base-record)
                      ;; Add embed only to first post
                      record (if (and (= index 1) embed)
                               (assoc record-with-reply :embed embed)
                               record-with-reply)
                      response (http/post "https://bsky.social/xrpc/com.atproto.repo.createRecord"
                                          {:headers {"Authorization" (str "Bearer " (:accessJwt session))
                                                     "Content-Type" "application/json"}
                                           :body (json/generate-string
                                                  {:repo (:did session)
                                                   :collection "app.bsky.feed.post"
                                                   :record record})})]
                  (if (= 200 (:status response))
                    (let [result (json/parse-string (:body response) true)
                          post-data {:uri (:uri result)
                                    :cid (:cid result)
                                    :root-uri (or (:root-uri previous-post) (:uri result))
                                    :root-cid (or (:root-cid previous-post) (:cid result))}]
                      (println (str "✓ Posted Bluesky post " index " of " (count threads)))
                      (recur (rest remaining) post-data (inc index)))
                    (println (str "Failed to post to Bluesky: " (:status response) " - " (:body response))))))))))
      (println "Bluesky credentials not configured"))))

(defn post-to-mastodon [content config images]
  (let [{:keys [instance access-token]} (get config :mastodon)]
    (if (and instance access-token)
      ;; Upload images first (up to 4)
      (let [mastodon-config {:instance instance :access-token access-token}
            media-ids (when (seq images)
                        (upload-images-to-mastodon images mastodon-config))
            threads (:threads content)]
        (loop [remaining threads
               previous-status-id nil
               index 1]
          (when-let [status-text (first remaining)]
            (let [;; Only attach media to first status
                  status-data (cond-> {:status status-text}
                                previous-status-id
                                (assoc :in_reply_to_id previous-status-id)
                                (and (= index 1) (seq media-ids))
                                (assoc :media_ids media-ids))
                  response (http/post (str instance "/api/v1/statuses")
                                      {:headers {"Authorization" (str "Bearer " access-token)
                                                 "Content-Type" "application/json"}
                                       :body (json/generate-string status-data)})]
              (if (= 200 (:status response))
                (let [result (json/parse-string (:body response) true)
                      status-id (:id result)]
                  (println (str "✓ Posted Mastodon status " index " of " (count threads)))
                  (recur (rest remaining) status-id (inc index)))
                (println (str "Failed to post to Mastodon: " (:status response) " - " (:body response))))))))
      (println "Mastodon credentials not configured"))))

(defn post-to-facebook [content config]
  (let [page-access-token (get-in config [:facebook :page-access-token])
        page-id (get-in config [:facebook :page-id])]
    (if (and page-access-token page-id)
      (let [response (http/post (str "https://graph.facebook.com/" page-id "/feed")
                                {:query-params {:access_token page-access-token}
                                 :headers {"Content-Type" "application/json"}
                                 :body (json/generate-string
                                        {:message (:content content)})})]
        (when (= 200 (:status response))
          (println "✓ Posted to Facebook successfully")))
      (println "Facebook credentials not configured"))))

(defn post-to-instagram [content config media-url]
  (if media-url
    (do
      (println "Instagram posting requires Facebook Graph API")
      (println "Note: Instagram requires Business/Creator account linked to Facebook")
      (println "Content prepared but not posted (requires complex OAuth + media upload)"))
    (println "ERROR: Instagram requires an image or video")))

;; Display draft and get approval
(defn display-draft [platform content images]
  (println (str "\n=== " (name platform) " Draft ==="))
  (case platform
    (:x :bluesky :mastodon)
    (do
      (println (str "Thread with " (count (:threads content)) " post(s):"))
      (doseq [[idx post] (map-indexed vector (:threads content))]
        (println (str "\nPost " (inc idx) " (" (count post) " chars):"))
        (println post)))

    (:linkedin :facebook)
    (do
      (println (str "Post (" (count (:content content)) " chars):"))
      (println (:content content)))

    :instagram
    (do
      (println (str "Caption (" (count (:content content)) " chars):"))
      (println (:content content))
      (println (str "Link: " (:link content)))))

  ;; Display image information
  (when (seq images)
    (println (str "\n📷 Images (" (count images) "):"))
    (doseq [[idx img] (map-indexed vector images)]
      (println (str "  " (inc idx) ". " (last (str/split (:path img) #"/"))
                    " (" (format "%.1f" (/ (:size img) 1024.0)) " KB, "
                    (:type img) ")")))))

(defn get-approval [platform]
  (print (str "\nPost to " (name platform) "? (y/n): "))
  (flush)
  (= "y" (str/lower-case (str/trim (read-line)))))

;; Setup instructions
(defn show-setup-instructions []
  (println "
=== Social Media Platform Setup Instructions ===

1. X (Twitter) - OAuth 2.0:
   - Go to https://developer.twitter.com/
   - Create a project and app
   - Enable OAuth 2.0 in app settings
   - Set callback URL to: http://localhost:8080/callback
   - Note Client ID and Client Secret (if confidential client)
   - Run: bb rss-syndicate.clj auth x
   - Requires $100/month API access

2. LinkedIn - OAuth 2.0:
   - Go to https://www.linkedin.com/developers/
   - Create an app
   - Add http://localhost:8080/callback to redirect URLs
   - Add required products: \"Share on LinkedIn\"
   - Note Client ID and Client Secret
   - Run: bb rss-syndicate.clj auth linkedin

3. Bluesky:
   - Use your regular Bluesky handle and password
   - Add to config: {:bluesky {:handle \"you.bsky.social\" :password \"app-password\"}}
   - Recommended: Create an App Password in Settings > Advanced > App Passwords

4. Mastodon:
   - Go to your instance's Settings > Development
   - Create a new application
   - Copy the access token
   - Add to config: {:mastodon {:instance \"https://mastodon.social\" :access-token \"your-token\"}}

5. Facebook:
   - Go to https://developers.facebook.com/
   - Create an app
   - Get Page Access Token for your page
   - Add to config: {:facebook {:page-id \"your-page-id\" :page-access-token \"your-token\"}}

6. Instagram:
   - Requires Facebook Business/Creator account
   - Link Instagram to Facebook Page
   - Use Facebook Graph API (complex setup)

Configuration file location: ~/.rss-syndicate-config.edn
"))

;; Main function
(defn -main [& args]
  (cond
    (= (first args) "setup")
    (show-setup-instructions)

    (= (first args) "auth")
    (let [platform (keyword (second args))
          config (load-config)]
      (case platform
        :x (when-let [x-config (authenticate-x-oauth2 config)]
             (save-config (assoc config :x x-config))
             (println "X configuration saved!"))
        :linkedin (when-let [linkedin-config (authenticate-linkedin-oauth2 config)]
                    (save-config (assoc config :linkedin linkedin-config))
                    (println "LinkedIn configuration saved!"))
        (println "Usage: bb rss-syndicate.clj auth [x|linkedin]")))

    (empty? args)
    (do
      (println "Usage: bb rss-syndicate.clj <feed-url> [platforms...]")
      (println "       bb rss-syndicate.clj setup")
      (println "       bb rss-syndicate.clj auth [x|linkedin]")
      (println "\nPlatforms: x, bluesky, mastodon, linkedin, facebook, instagram")
      (println "Example: bb rss-syndicate.clj https://example.com/feed.xml x bluesky mastodon"))

    :else
    (let [feed-url (first args)
          platforms (if (> (count args) 1)
                      (map keyword (rest args))
                      [:x :bluesky :mastodon :linkedin :facebook :instagram])
          config (load-config)
          ;; Platform image limits
          platform-max-images {:x 4 :bluesky 4 :mastodon 4 :linkedin 20 :facebook 0 :instagram 1}]

      (println (str "Fetching feed: " feed-url))
      (try
        (let [feed (parse-feed feed-url)
              latest-item (first (:items feed))
              title (:title latest-item)
              link (:link latest-item)
              description (:description latest-item)
              media-url (:url (:enclosure latest-item))
              ;; Extract all images from RSS item (resolve relative URLs against feed URL)
              all-images (extract-all-images latest-item feed-url)
              ;; Download images once (max 20 to cover LinkedIn's MultiImage)
              downloaded-images (when (seq all-images)
                                  (println "\nDownloading images...")
                                  (download-images all-images 20))]

          (println (str "\n📰 Latest post: " title))
          (println (str "🔗 Link: " link))
          (when (seq downloaded-images)
            (println (str "📷 Downloaded " (count downloaded-images) " image(s)")))

          (try
            (doseq [platform platforms]
              (let [;; Get max images for this platform
                    max-images (get platform-max-images platform 0)
                    ;; Take appropriate number of images for this platform
                    platform-images (vec (take max-images downloaded-images))
                    formatted (format-content description link platform)]
                (display-draft platform formatted platform-images)
                (when (get-approval platform)
                  (case platform
                    :x (post-to-x formatted config platform-images)
                    :bluesky (post-to-bluesky formatted config platform-images)
                    :mastodon (post-to-mastodon formatted config platform-images)
                    :linkedin (post-to-linkedin formatted config platform-images)
                    :facebook (post-to-facebook formatted config)
                    :instagram (post-to-instagram formatted media-url config)))))
            (finally
              ;; Cleanup temp image files
              (when (seq downloaded-images)
                (println "\nCleaning up temporary files...")
                (cleanup-temp-images downloaded-images)))))
        (catch Exception e
          (println (str "Error: " (.getMessage e))))))))


;; Always run main with *command-line-args* when script is executed
(apply -main *command-line-args*)
