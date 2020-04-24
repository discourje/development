(ns discourje.core.async.examples
  (:gen-class)
  (:refer-clojure :exclude [compare])
  (:require [clojure.string :refer [join]]
            [discourje.core.async.examples.config :as config])
  (:import (java.time LocalDateTime)))

(defn run [ns lib input]
  (binding [config/*lib* lib
            config/*input* input
            config/*output* nil
            config/*time* nil]
    (try
      (require ns :reload)
      {:ns     ns
       :lib    lib
       :input  config/*input*
       :output config/*output*
       :time   config/*time*}
      (catch Throwable t (.printStackTrace t)))))

(defn compare [ns libs input]
  (mapv #(run ns % input) libs))

(defn start [ns lib input]
  (.start (Thread. ^Runnable (fn [] (prn (run ns lib input))))))

(defmacro version []
  (str (LocalDateTime/now)))

(defn -main [& args]
  (try
    (if (< (count args) 2)
      (throw (ex-info "" {::message "Not enough arguments"})))

    (let [lib (keyword (first args))
          ns (symbol (str "discourje.core.async.examples" "." (second args)))
          input (read-string (join " " (rest (rest args))))]

      (if (not (contains? #{:clj :dcj :dcj-nil} lib))
        (throw (ex-info "" {::message "Unknown lib"})))

      (if (not (contains? #{'discourje.core.async.examples.micro.ring
                            'discourje.core.async.examples.micro.mesh
                            'discourje.core.async.examples.micro.star}
                          ns))
        (throw (ex-info "" {::message "Unknown program"})))

      (println (run ns lib input)))

    (catch Throwable t
      (let [m (ex-data t)]
        (println)
        (if (contains? m ::message)
          (do (println (str "Discourje Examples (" (version) ")"))
              (println (str "Error: " (::message m)))
              (println (str "Usage: java -jar discourje-examples.jar <lib> <program> <input>"))
              (println (str "  <lib>     \u2208 {clj, dcj, dcj-nil}"))
              (println (str "  <program> \u2208 {micro.ring, micro.mesh, micro.star}")))
          (.printStackTrace t))
        (println)))))