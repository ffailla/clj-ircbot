(ns com.frankfailla.ircbot.campfire
  (:import [java.util.concurrent LinkedBlockingQueue TimeUnit CountDownLatch])
  (:require [clojure.data.json :as json]
            [clj-http.client :as client]
            [clojure.tools.logging :as logging]
            [com.frankfailla.ircbot.utils :as utils]))

;; https://DOMAIN.campfirenow.com/room/ROOMID/transcript.json
;; https://DOMAIN.campfirenow.com/rooms.json
;; https://DOMAIN.campfirenow.com/room/ROOMID.json

(def ^{:private true :dynamic true} *message-latch* (ref (CountDownLatch. 1)))
(def ^{:private true :dynamic true} *message-latch-timeout* 5000)

(def ^:dynamic *me* (ref {}))
(def ^:dynamic *rooms* (ref {}))
(def ^:dynamic *users* (ref {}))
(def ^:dynamic *lastmessageid* (ref nil))

(defn- message-latch
  []
  (.await @*message-latch* *message-latch-timeout* TimeUnit/MILLISECONDS))

(defn user
  [apikey domain]
  (client/get
   (format "https://%s.campfirenow.com/users/me.json" domain)
   {:basic-auth [apikey]}))

(defn post
  [apikey domain roomid msg]
  (client/post
   (format "https://%s.campfirenow.com/room/%s/speak.json" domain roomid)
   {:basic-auth [apikey]
    :body (json/json-str {:message msg})
    :content-type :json}))

(defn room-info
  [apikey domain roomid]
  (client/get (format "https://%s.campfirenow.com/room/%s.json" domain roomid)
              {:basic-auth [apikey]}))

(defn transcript
  [apikey domain roomid]
  (client/get (format "https://%s.campfirenow.com/room/%s/transcript.json" domain roomid)
              {:basic-auth [apikey]}))

(defn recent
  [apikey domain roomid lastid]
  (client/get (format "https://%s.campfirenow.com/room/%s/recent.json?since_message_id=%s" domain roomid lastid)
              {:basic-auth [apikey]}))

(defn rooms
  [apikey domain]
  (client/get (format "https://%s.campfirenow.com/rooms.json" domain)
              {:basic-auth [apikey]}))

(defn live
  [apikey roomid]
  (client/get (format "https://streaming.campfirenow.com/room/%s/live.json" roomid)
              {:basic-auth [apikey]}))

(defn extract-json
  [res]
  (json/read-json (:body res)))

(defn users->id
  [room-info]
  (reduce #(assoc %1 (:id %2) %2)
          {} (-> room-info :room :users)))

(defn rooms->name
  [rooms]
  (reduce #(assoc %1 (:name %2) %2)
          {} (:rooms rooms)))

(defn sync-rooms
  [apikey domain]
  (dosync (alter *rooms* merge (-> (rooms apikey domain) extract-json rooms->name))))

(defn sync-users
  [apikey domain]
  (let [ri (reduce #(merge %1 (-> %2 extract-json users->id))
                   {} (map (partial room-info apikey domain)
                           (map :id (vals @*rooms*))))]
    (dosync (alter *users* merge ri))))

(defn sync-me
  [apikey domain]
  (dosync (alter *me* (fn [_] (extract-json (user apikey domain))))))

(defn send-message
  [apikey domain room msg]
  (let [roomid (:id (@*rooms* room))]
    (post apikey domain roomid msg)))

(defn process-messages
  [apikey domain room processor]
  (let [roomid (:id (@*rooms* room))
        msgs (:messages (extract-json (recent apikey domain roomid @*lastmessageid*)))
        maxid (when-not (empty? msgs) (apply max (map :id msgs)))]
    (when-not (nil? @*lastmessageid*)
      (doall (map processor msgs)))
    (dosync (alter *lastmessageid* (fn [oldid] (if maxid maxid oldid))))))

(defn start-message-poll
  [apikey domain room processor]
  (try
    (sync-me apikey domain)
    (catch Exception e
      (utils/publish e)
      (throw e)))
  (loop [exit? (message-latch)]
    (try
      (sync-rooms apikey domain)
      (sync-users apikey domain)
      (process-messages apikey domain room processor)
      (catch Exception e (utils/publish e)))
    (recur (message-latch))))

(defn stop-message-poll
  []
  (.countDown @*message-latch*))

(defn format-message
  [msg]
  (format "<%s>\t%s" (:name (@*users* (:user_id msg))) (:body msg)))