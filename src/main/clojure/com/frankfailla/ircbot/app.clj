(ns com.frankfailla.ircbot.app
  (:require [clojure.contrib.logging :as logging]
	    [com.frankfailla.ircbot.web :as web]
	    [com.frankfailla.ircbot.bot :as bot]
	    [com.frankfailla.ircbot.sdb :as sdb]
	    [com.frankfailla.ircbot.utils :as utils])
  (:import [org.jibble.pircbot PircBot])
  (:gen-class))

(defn -main
  [& args]
  (utils/props-to-system (utils/load-props "clj-ircbot.properties"))
  (web/start-jetty (Integer/parseInt (System/getProperty "jetty.port")))
  (.start (Thread. (partial bot/start-bot
			    (System/getProperty "ircbot.name")
			    (System/getProperty "ircbot.server")
			    (System/getProperty "ircbot.channel")
			    (when (pos? (count (System/getProperty "ircbot.channel-key")))
			      (System/getProperty "ircbot.channel-key"))
			    #(try
			       (let [client-config (sdb/make-client-config)
				     domain (sdb/make-domain-name (System/getProperty "ircbot.channel"))]
				 (logging/log :trace (bot/format-message %))
				 (sdb/put-sdb client-config domain %))
			       (catch Exception e (utils/publish e)))))))
