#!/usr/bin/env bb

(ns rss-syndicate
  (:require [babashka.http-client :as http]
            [clojure.data.xml :as xml]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [cheshire.core :as json]))

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

;; Load configuration
(defn load-config []
  (if (.exists (io/file config-file))
    (read-string (slurp config-file))
    {}))

;; Save configuration
(defn save-config [config]
  (spit config-file (pr-str config)))

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
        feed-xml (xml/parse-str (:body response))]
    (cond
      ;; RSS 2.0
      (= :rss (local-name (:tag feed-xml)))
      (let [channel (first (filter #(= :channel (local-name (:tag %))) (:content feed-xml)))
            items (filter #(= :item (local-name (:tag %))) (:content channel))]
        {:type :rss
         :items (map parse-rss-item items)})

      ;; Atom
      (= :feed (local-name (:tag feed-xml)))
      {:type :atom
       :items (map parse-atom-entry
                   (filter #(= :entry (local-name (:tag %))) (:content feed-xml)))}

      :else
      (throw (Exception. "Unknown feed format")))))

;; Strip HTML tags
(defn strip-html [s]
  (-> s
      (str/replace #"<[^>]*>" "")
      (str/replace #"&nbsp;" " ")
      (str/replace #"&amp;" "&")
      (str/replace #"&lt;" "<")
      (str/replace #"&gt;" ">")
      (str/replace #"&quot;" "\"")))

(defn split-into-threads [text link max-chars]
  (let [thread-marker " [→]"
        link-length (count link)
        first-thread-max (- max-chars link-length 10)
        other-thread-max (- max-chars (count thread-marker) 5)
        words (str/split text #"\s+")]
    (loop [remaining words
           current-thread []
           threads []
           is-first true]
      (if (empty? remaining)
        (conj threads
              (if is-first
                (str (str/join " " current-thread) "\n\n" link)
                (str (str/join " " current-thread) thread-marker)))
        (let [word (first remaining)
              current-length (count (str/join " " current-thread))
              max-for-thread (if is-first first-thread-max other-thread-max)]
          (if (> (+ current-length (count word) 1) max-for-thread)
            (recur remaining
                   []
                   (conj threads
                         (if is-first
                           (str (str/join " " current-thread) "\n\n" link)
                           (str (str/join " " current-thread) thread-marker)))
                   false)
            (recur (rest remaining)
                   (conj current-thread word)
                   threads
                   is-first)))))))

;; Format content for different platforms
(defn format-content [content link platform]
  (let [clean-content (strip-html content)
        max-chars (get char-limits platform)]
    (case platform
      (:x :bluesky :mastodon)
      (let [link-suffix (str "\n\n" link)
            available-chars (- max-chars (count link-suffix))
            threads (if (<= (count clean-content) available-chars)
                      [(str clean-content link-suffix)]
                      (split-into-threads clean-content link max-chars))]
        {:threads threads})

      (:linkedin :facebook)
      {:content (str clean-content "\n\n" link)}

      :instagram
      {:content (if (> (count clean-content) (:instagram char-limits))
                  (str (subs clean-content 0 (- (:instagram char-limits) 3)) "...")
                  clean-content)
       :link link})))

;; Platform-specific posting functions
(defn percent-encode [s]
  (-> s
      str
      (java.net.URLEncoder/encode "UTF-8")
      (str/replace "+" "%20")
      (str/replace "*" "%2A")
      (str/replace "%7E" "~")))

(defn generate-oauth-signature [method url oauth-params consumer-secret token-secret]
  (let [sorted-params (sort-by first oauth-params)
        param-string (str/join "&"
                               (map (fn [[k v]]
                                      (str (percent-encode k) "=" (percent-encode v)))
                                    sorted-params))
        base-string (str (str/upper-case method) "&"
                         (percent-encode url) "&"
                         (percent-encode param-string))
        signing-key (str (percent-encode consumer-secret) "&" (percent-encode token-secret))
        mac (javax.crypto.Mac/getInstance "HmacSHA1")
        secret-key (javax.crypto.spec.SecretKeySpec. (.getBytes signing-key "UTF-8") "HmacSHA1")]
    (.init mac secret-key)
    (.encodeToString (java.util.Base64/getEncoder)
                     (.doFinal mac (.getBytes base-string "UTF-8")))))

;; Platform-specific posting functions
(defn post-to-x [content config]
  (println "Posting to X...")
  (let [{:keys [api-key api-secret access-token access-token-secret]} (get config :x)]
    (if (and api-key api-secret access-token access-token-secret)
      (try
        (let [url "https://api.twitter.com/2/tweets"
              timestamp (str (quot (System/currentTimeMillis) 1000))
              nonce (str/replace (str (java.util.UUID/randomUUID)) "-" "")
              oauth-params {"oauth_consumer_key" api-key
                            "oauth_nonce" nonce
                            "oauth_signature_method" "HMAC-SHA1"
                            "oauth_timestamp" timestamp
                            "oauth_token" access-token
                            "oauth_version" "1.0"}
              signature (generate-oauth-signature "POST" url oauth-params api-secret access-token-secret)
              all-oauth-params (assoc oauth-params "oauth_signature" signature)
              sorted-oauth (sort-by first all-oauth-params)
              auth-header (str "OAuth "
                               (str/join ", "
                                         (map (fn [[k v]]
                                                (str k "=\"" (percent-encode v) "\""))
                                              sorted-oauth)))
              response (http/post url
                                  {:headers {"Authorization" auth-header
                                             "Content-Type" "application/json"}
                                   :body (json/generate-string
                                          {:text (first (:threads content))})})]
          (if (= 201 (:status response))
            (println "✓ Posted to X successfully")
            (println (str "Failed to post to X: " (:status response) " - " (:body response)))))
        (catch Exception e
          (println (str "Error posting to X: " (.getMessage e)))))
      (println "X credentials not configured (need api-key, api-secret, access-token, access-token-secret)"))))

(defn post-to-bluesky [content config]
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
          ;; Post content
          (let [response (http/post "https://bsky.social/xrpc/com.atproto.repo.createRecord"
                                    {:headers {"Authorization" (str "Bearer " (:accessJwt session))
                                               "Content-Type" "application/json"}
                                     :body (json/generate-string
                                            {:repo (:did session)
                                             :collection "app.bsky.feed.post"
                                             :record {:text (first (:threads content))
                                                      :createdAt (str (java.time.Instant/now))}})})]
            (when (= 200 (:status response))
              (println "✓ Posted to Bluesky successfully")))))
      (println "Bluesky credentials not configured"))))

(defn post-to-mastodon [content config]
  (let [{:keys [instance access-token]} (get config :mastodon)]
    (if (and instance access-token)
      (let [response (http/post (str instance "/api/v1/statuses")
                                {:headers {"Authorization" (str "Bearer " access-token)
                                           "Content-Type" "application/json"}
                                 :body (json/generate-string
                                        {:status (first (:threads content))})})]
        (when (= 200 (:status response))
          (println "✓ Posted to Mastodon successfully")))
      (println "Mastodon credentials not configured"))))

(defn post-to-linkedin [content config]
  (println "LinkedIn posting requires OAuth 2.0 flow")
  (println "Visit: https://www.linkedin.com/developers/apps to create an app")
  (println "Content prepared but not posted (requires OAuth implementation)"))

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
(defn display-draft [platform content]
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
      (println (str "Link: " (:link content))))))

(defn get-approval [platform]
  (print (str "\nPost to " (name platform) "? (y/n): "))
  (flush)
  (= "y" (str/lower-case (str/trim (read-line)))))

;; Setup instructions
(defn show-setup-instructions []
  (println "
=== Social Media Platform Setup Instructions ===

1. X (Twitter):
   - Go to https://developer.twitter.com/
   - Create a project and app
   - Generate Bearer Token (requires $100/month API access)
   - Add to config: {:x {:bearer-token \"your-token\"}}

2. Bluesky:
   - Use your regular Bluesky handle and password
   - Add to config: {:bluesky {:handle \"you.bsky.social\" :password \"app-password\"}}
   - Recommended: Create an App Password in Settings > Privacy and Security > App Passwords

3. Mastodon:
   - Go to your instance's Settings > Development
   - Create a new application
   - Copy the access token
   - Add to config: {:mastodon {:instance \"https://mastodon.social\" :access-token \"your-token\"}}

4. LinkedIn:
   - Go to https://www.linkedin.com/developers/
   - Create an app
   - Requires OAuth 2.0 flow (not implemented in this script)

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

    (empty? args)
    (do
      (println "Usage: bb rss-syndicate.clj <feed-url> [platforms...]")
      (println "       bb rss-syndicate.clj setup")
      (println "\nPlatforms: x, bluesky, mastodon, linkedin, facebook, instagram")
      (println "Example: bb rss-syndicate.clj https://example.com/feed.xml x bluesky mastodon"))

    :else
    (let [feed-url (first args)
          platforms (if (> (count args) 1)
                      (map keyword (rest args))
                      [:x :bluesky :mastodon :linkedin :facebook :instagram])
          config (load-config)]

      (println (str "Fetching feed: " feed-url))
      (let [feed (parse-feed feed-url)
            latest-item (first (:items feed))
            title (:title latest-item)
            link (:link latest-item)
            description (:description latest-item)
            media-url (:url (:enclosure latest-item))]

        (println (str "\n📰 Latest post: " title))
        (println (str "🔗 Link: " link))
        (when media-url
          (println (str "📷 Media: " media-url)))

        (doseq [platform platforms]
          (let [formatted (format-content description link platform)]
            (display-draft platform formatted)
            (when (get-approval platform)
              (case platform
                :x (post-to-x formatted config)
                :bluesky (post-to-bluesky formatted config)
                :mastodon (post-to-mastodon formatted config)
                :linkedin (post-to-linkedin formatted config)
                :facebook (post-to-facebook formatted config)
                :instagram (post-to-instagram formatted media-url config)))))))))

(apply -main *command-line-args*)

(comment
  (xml/parse-str (:body (http/get "https://www.stevenoxley.com/mb/atom.xml")))
  )
