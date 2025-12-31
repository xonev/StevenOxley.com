
#!/usr/bin/env bb


(ns rss-syndicate
  (:require [babashka.http-client :as http]
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
                      "scope=" (java.net.URLEncoder/encode "tweet.read tweet.write users.read offline.access" "UTF-8") "&"
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
                      "scope=" (java.net.URLEncoder/encode "w_member_social" "UTF-8"))
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
                (let [tokens (json/parse-string (:body response) true)]
                  (println "✓ LinkedIn OAuth 2.0 authentication successful!")
                  {:client-id client-id
                   :client-secret client-secret
                   :access-token (:access_token tokens)
                   :expires-at (+ (System/currentTimeMillis) (* 1000 (:expires_in tokens)))})
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
              current-length (reduce + (count (str/join " " current-thread)))
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
(defn post-to-x [content config]
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
        (let [response (http/post "https://api.twitter.com/2/tweets"
                                  {:headers {"Authorization" (str "Bearer " (:access-token x-config))
                                             "Content-Type" "application/json"}
                                   :body (json/generate-string
                                          {:text (first (:threads content))})})]
          (if (= 201 (:status response))
            (println "✓ Posted to X successfully")
            (println (str "Failed to post to X: " (:status response) " - " (:body response)))))
        (catch Exception e
          (println (str "Error posting to X: " (.getMessage e)))))
      (println "X OAuth 2.0 access token not configured. Run 'auth x' to authenticate."))))

(defn post-to-linkedin [content config]
  (println "Posting to LinkedIn...")
  (let [linkedin-config (get config :linkedin)]
    (if (:access-token linkedin-config)
      (try
        ;; First, get the user's profile to get the person URN
        (let [profile-response (http/get "https://api.linkedin.com/v2/userinfo"
                                         {:headers {"Authorization" (str "Bearer " (:access-token linkedin-config))}})
              profile (when (= 200 (:status profile-response))
                        (json/parse-string (:body profile-response) true))
              person-urn (str "urn:li:person:" (:sub profile))]

          (if person-urn
            (let [response (http/post "https://api.linkedin.com/v2/ugcPosts"
                                      {:headers {"Authorization" (str "Bearer " (:access-token linkedin-config))
                                                 "Content-Type" "application/json"
                                                 "X-Restli-Protocol-Version" "2.0.0"}
                                       :body (json/generate-string
                                              {:author person-urn
                                               :lifecycleState "PUBLISHED"
                                               :specificContent {"com.linkedin.ugc.ShareContent"
                                                                 {:shareCommentary {:text (:content content)}
                                                                  :shareMediaCategory "NONE"}}
                                               :visibility {"com.linkedin.ugc.MemberNetworkVisibility" "PUBLIC"}})})]
              (if (= 201 (:status response))
                (println "✓ Posted to LinkedIn successfully")
                (println (str "Failed to post to LinkedIn: " (:status response) " - " (:body response)))))
            (println "Failed to get LinkedIn user profile")))
        (catch Exception e
          (println (str "Error posting to LinkedIn: " (.getMessage e)))))
      (println "LinkedIn OAuth 2.0 access token not configured. Run 'auth linkedin' to authenticate."))))

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
          config (load-config)]

      (println (str "Fetching feed: " feed-url))
      (try
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
                  :instagram (post-to-instagram formatted media-url config))))))
        (catch Exception e
          (println (str "Error: " (.getMessage e))))))))


;; Always run main with *command-line-args* when script is executed
(apply -main *command-line-args*)
