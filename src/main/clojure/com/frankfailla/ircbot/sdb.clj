(ns com.frankfailla.ircbot.sdb
 (:require [cemerick.rummage :as sdb]
	   [cemerick.rummage.encoding :as enc]))

(defn make-client-config
  ([] (make-client-config (System/getProperty "aws.access-key") (System/getProperty "aws.secret-key")))
  ([access-key secret-key]
     (let [client (sdb/create-client access-key secret-key)]
       (assoc enc/keyword-strings :client client))))

(defn make-domain-name
  [channel]
  (str "ircbot.log." (.substring channel 1)))

(defn put-sdb
  [client-config domain msg]
  (sdb/create-domain client-config domain)
  (sdb/put-attrs client-config domain msg))

(defn wipe-sdb
  [client-config domain]
  (let [all-data (sdb/query-all client-config `{select * from ~domain})]
    (doall
     (map #(sdb/delete-attrs client-config domain %)
	  (map :cemerick.rummage/id all-data)))))

(defn days-in-log
  [client-config channel]
  (let [domain (make-domain-name channel)]
    (set (map :datetime-day (sdb/query-all client-config `{select [:datetime-day] from ~domain})))))

(defn query-log
  ([client-config channel]
     (let [domain (make-domain-name channel)]
       (sdb/query-all client-config `{select * from ~domain})))
  ([client-config channel day]
     (let [domain (make-domain-name channel)]
       (sdb/query-all client-config
		      `{select * from ~domain where
			(= :datetime-day ~day)})))
  ([client-config channel start-day end-day]
     (let [domain (make-domain-name channel)]
       (sdb/query-all client-config
		      `{select * from ~domain where
			(and (>= :datetime-day ~start-day)
			     (<= :datetime-day ~end-day))}))))