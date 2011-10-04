(ns com.frankfailla.ircbot.web
  (:require [clojure.data.json :as json]
	    [ring.adapter.jetty :as jetty]
	    [ring.middleware.file :as ring-file]
	    [compojure.core :as compojure]
	    [com.frankfailla.ircbot.bot :as bot]
	    [com.frankfailla.ircbot.sdb :as sdb]
	    [com.frankfailla.ircbot.utils :as utils]))

(defn wrap-exception-logging
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e
        (utils/publish e)
        (throw e)))))

(def ^:dynamic *jetty-server* (atom nil))
(def ^:dynamic *web-root* (str (System/getProperty "user.dir") "/src/main/webapp"))

(def ^:dynamic *web-app*
  (when (not *compile-files*)
    (-> (compojure/routes 
	 (compojure/GET "/botchannel" []
			(json/json-str {:channel (System/getProperty "ircbot.channel")}))
	 (compojure/GET "/irclog/:channel" [channel]
			(let [client (sdb/make-client-config)]
			  (json/json-str
			   (assoc {} :records (sdb/query-log client (bot/hash-channel channel))))))
	 (compojure/GET "/irclog/:channel/:day" [channel day]
			(let [client (sdb/make-client-config)]
			  (json/json-str
			   (assoc {} :records (sdb/query-log client (bot/hash-channel channel) day)))))
	 (compojure/GET "/irclog/:channel/:sd/:ed" [channel sd ed]
			(let [client (sdb/make-client-config)]
			  (json/json-str
			   (assoc {} :records (sdb/query-log client (bot/hash-channel channel) sd ed)))))
	 (compojure/GET "/ircindex/:channel" [channel]
			(let [client (sdb/make-client-config)]
			  (json/json-str
			   (assoc {} :days-in-log (sdb/days-in-log client (bot/hash-channel channel)))))))
	(ring-file/wrap-file *web-root*)
	wrap-exception-logging)))

(defn start-jetty
  [port]
  (swap! *jetty-server*
	 (fn [instance]
	   (when instance (.stop instance))
	   (jetty/run-jetty (var *web-app*) {:port port :join? false}))))

(defn stop-jetty []
  (swap! *jetty-server* #(when % (.stop %))))
