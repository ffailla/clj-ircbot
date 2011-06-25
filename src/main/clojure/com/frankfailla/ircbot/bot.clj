(ns com.frankfailla.ircbot.bot
  (:require [com.frankfailla.ircbot.utils :as utils])
  (:import [org.jibble.pircbot PircBot]))

(def *bot* (atom nil))

(defn hash-channel [c] (str "#" c))

(defn pircbot
  [processor]
  (proxy [PircBot] []
    (onMessage [channel sender login hostname messsage]
      (processor channel sender login hostname messsage))))

(defn join-channel
  [bot name server channel key]
  (doto bot
    (.connect server)
    (.changeNick name))
  (if key
      (.joinChannel bot channel key)
      (.joinChannel bot channel)))

(defn format-message
  [{:keys [datetime channel sender login hostname message] :as msg}]
  (format "id: %d\ndatetime: %s\nchannel: %s\nsender: %s\nlogin: %s\nhostname: %s\nmessage: %s\n"
	  (:cemerick.rummage/id msg) datetime channel sender login hostname message))

(defn make-message
  [channel sender login hostname message]
  (let [dt (java.util.Date.)]
    (array-map :cemerick.rummage/id (System/nanoTime)
	       :datetime (utils/format-date dt)
	       :datetime-day (utils/format-date dt utils/*dateday-format*)
	       :datetime-key (utils/format-date dt utils/*datekey-format*)
	       :channel channel
	       :sender sender
	       :login login
	       :hostname hostname
	       :message message)))

(defn start-bot
  ([name server channel processor]
     (start-bot name server channel nil processor))
  ([name server channel channel-key processor]
     (swap! *bot* (fn [_]
		    (pircbot
		     (fn [channel sender login hostname message]
		       (try
			 (processor (make-message channel sender login hostname message))
			 (catch Exception e (utils/publish e)))))))
     (join-channel @*bot* name server channel channel-key)))

(defn stop-bot
  []
  (.disconnect @*bot*))
