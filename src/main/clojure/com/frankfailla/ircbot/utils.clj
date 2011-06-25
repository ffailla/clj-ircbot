(ns com.frankfailla.ircbot.utils
  (:require [clojure.contrib.logging :as logging]))

(def *date-format*
  (doto (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    (.setTimeZone (java.util.TimeZone/getTimeZone "UTC"))))

(def *datekey-format*
  (doto (java.text.SimpleDateFormat. "yyyyMMddHHmmssSSS")
    (.setTimeZone (java.util.TimeZone/getTimeZone "UTC"))))

(def *dateday-format*
  (doto (java.text.SimpleDateFormat. "yyyyMMdd")
    (.setTimeZone (java.util.TimeZone/getTimeZone "UTC"))))

(defn format-date
  ([d] (str (.format *date-format* d)))
  ([d f] (str (.format f d))))

(defn parse-date
  ([s] (.parse *date-format* s))
  ([s f] (.parse f s)))

(defn- get-all-exceptions
  [exc next-func]
  (loop [e exc acc []]
    (if e
      (recur (next-func e) (conj acc e))
      acc)))

(defn- get-exc-msg
  [exc]
  (.getMessage exc))

(defn- get-exc-stacktrace
  [exc]
  (let [sw (java.io.StringWriter.)
        err (java.io.PrintWriter. sw true)]
    (.printStackTrace exc err)
    (flush)
    (str sw)))

(defn- get-exc-info
  [exc next-func]
  (str (get-exc-stacktrace exc) 
       (apply str (map #(str (get-exc-msg %) "\n")
		       (get-all-exceptions exc next-func)))))

(defn publish
  [exc]
  (logging/log :error (get-exc-info exc (memfn getCause))))

(defn load-props
  [prop-uri]
  (let [props (java.util.Properties.)
        res (.getResourceAsStream (.getContextClassLoader (Thread/currentThread)) prop-uri)]
    (if res
      (do
	(.load props res)
	props)
      (println (str "The properties file [" prop-uri "] was not found on classpath")))))

(defn props-to-system
  [props]
  (doall (map (fn [[k v]] (System/setProperty k v)) props)))