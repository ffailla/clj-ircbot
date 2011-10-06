(ns com.frankfailla.ircbot.app
  (:require [clojure.tools.logging :as logging]
	    [com.frankfailla.ircbot.web :as web]
	    [com.frankfailla.ircbot.bot :as bot]
	    [com.frankfailla.ircbot.sdb :as sdb]
	    [com.frankfailla.ircbot.utils :as utils]
            [com.frankfailla.ircbot.campfire :as campfire])
  (:import [org.jibble.pircbot PircBot])
  (:gen-class))
 
(defn -main
  [& args]
  (utils/props-to-system (utils/load-props "clj-ircbot.properties"))
  (web/start-jetty (Integer/parseInt (System/getProperty "jetty.port")))
  (.start (Thread. (partial campfire/start-message-poll
                            (System/getProperty "campfire.api-key")
                            (System/getProperty "campfire.domain")
                            (System/getProperty "campfire.room")
                            #(try
                               (logging/log :trace (pr-str %))
                               (when-not (= (-> @campfire/*me* :user :id) (:user_id %))
                                 (bot/send-message (System/getProperty "ircbot.channel")
                                                   (campfire/format-message %))
                                 (let [client-config (sdb/make-client-config)
                                       domain (sdb/make-domain-name (System/getProperty "ircbot.channel"))]
                                   (sdb/put-sdb client-config
                                                domain
                                                (bot/make-message (System/getProperty "ircbot.channel")
                                                                  (-> @campfire/*me* :user :id)
                                                                  (-> @campfire/*me* :user :id)
                                                                  (System/getProperty "ircbot.server")
                                                                  (campfire/format-message %)))))
                               (catch Exception e (utils/publish e))))))
  (.start (Thread. (partial bot/start-bot
			    (System/getProperty "ircbot.name")
			    (System/getProperty "ircbot.server")
			    (System/getProperty "ircbot.channel")
			    (when (pos? (count (System/getProperty "ircbot.channel-key")))
			      (System/getProperty "ircbot.channel-key"))
			    #(try
                               (logging/log :trace (pr-str %))
                               (let [{:keys [datetime channel sender login hostname message] :as msg} %]
                                 (campfire/send-message (System/getProperty "campfire.api-key")
                                                        (System/getProperty "campfire.domain")
                                                        (System/getProperty "campfire.room")
                                                        message))
                               (let [client-config (sdb/make-client-config)
                                     domain (sdb/make-domain-name (System/getProperty "ircbot.channel"))]
                                 (sdb/put-sdb client-config domain %))
			       (catch Exception e (utils/publish e)))))))