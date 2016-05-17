(ns greenyet.core
  (:require [clj-yaml.core :as yaml]
            [clojure.core.async :refer [<! go-loop timeout]]
            [clojure.java.io :as io]
            [greenyet
             [status :as status]
             [view :as view]]))

(def ^:private hosts-with-status (atom {}))

(def ^:private config-dir (System/getenv "CONFIG_DIR"))

(def ^:private timeout-in-ms (or (some-> (System/getenv "TIMEOUT")
                                         Integer/parseInt)
                                 5000))

(def ^:private config-params [["CONFIG_DIR" (or config-dir
                                                "")]
                              ["TIMEOUT" timeout-in-ms]
                              ["PORT" (or (System/getenv "PORT")
                                          3000)]])

(defn- poll-status [host status-url-config]
  (go-loop []
    (let [status (status/with-status host status-url-config)]
      (swap! hosts-with-status assoc host status)
      (<! (timeout timeout-in-ms))
      (recur))))


(defn- read-host-list [config-dir]
  (let [host-list-file (io/file config-dir "hosts.yaml")]
    (yaml/parse-string (slurp host-list-file))))

(defn- read-status-url-config [config-dir]
  (let [config-file (io/file config-dir "status_url.yaml")]
    (yaml/parse-string (slurp config-file))))


(defn help []
  (println "Starting greenyet with config")
  (->> config-params
       (map (fn [[env-var value]]
              (format "  %s: '%s'" env-var value)))
       (map println)
       doall))

(defn- show [x]
  {:body (view/render (vals @hosts-with-status))})

(def handler
  (let [host-list (read-host-list config-dir)
        status-url-config (read-status-url-config config-dir)]
    (doall (for [host host-list]
             (poll-status host status-url-config)))
    show))
