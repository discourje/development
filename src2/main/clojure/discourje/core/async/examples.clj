(ns discourje.core.async.examples
  (:gen-class)
  (:refer-clojure :exclude [compare])
  (:require [clojure.string :refer [join]]
            [discourje.core.async.examples.config :as config])
  (:import (java.time LocalDateTime)))

(defn configs
  ([m]
   (configs (:lib m) (:program m) (:input m)))
  ([libs programs inputs]
   {:pre  [(vector? libs)
           (vector? programs)
           (map? inputs) (every? vector? (vals inputs))]
    :post [(vec %) (every? map? %)]}
   (let [f (fn [k vals m] (mapv #(merge m {k %}) vals))
         inputs (loop [inputs inputs
                       result [{}]]
                  (if (empty? inputs)
                    result
                    (let [[k vals] (first inputs)]
                      (recur (rest inputs) (reduce into (mapv (partial f k vals) result))))))
         configs [{}]
         configs (reduce into (mapv (partial f :lib libs) configs))
         configs (reduce into (mapv (partial f :program programs) configs))
         configs (reduce into (mapv (partial f :input inputs) configs))]
     configs)))

(defn run
  ([config]
   (run (:lib config) (:program config) (:input config)))
  ([lib program input]
   (binding [config/*lib* lib
             config/*input* (merge {:resolution 1} input)
             config/*output* nil
             config/*time* nil]
     (try
       (require program :reload)
       {:lib     lib
        :program program
        :input   config/*input*
        :output  config/*output*
        :time    config/*time*}
       (catch Throwable t (.printStackTrace t))))))

(defn run-all
  ([configs]
   (mapv #(run %) configs))
  ([libs programs inputs]
   (run-all (configs libs programs inputs))))

(defn start [lib program input]
  (.start (Thread. ^Runnable (fn [] (println (run lib program input))))))

(defmacro version []
  (str (LocalDateTime/now)))

(defn -main [& args]
  (try
    (case (first args)
      "run"
      (let [args (rest args)]
        (if (< (count args) 2)
          (throw (ex-info "" {::message "Not enough arguments"})))

        (let [lib (keyword (first args))
              program (symbol (str "discourje.core.async.examples" "." (second args)))
              input (read-string (join " " (rest (rest args))))]

          (if (not (contains? #{:clj :dcj :dcj-nil} lib))
            (throw (ex-info "" {::message "Unknown lib"})))

          (if (not (contains? #{'discourje.core.async.examples.micro.ring
                                'discourje.core.async.examples.micro.mesh
                                'discourje.core.async.examples.micro.star}
                              program))
            (throw (ex-info "" {::message "Unknown program"})))

          (prn (run lib program input))))

      "configs"
      (let [args (rest args)]
        (doseq [config (configs (read-string (join " " args)))]
          (prn config)))

      (throw (ex-info "" {::message "Unknown command"})))

    (catch Throwable t
      (let [m (ex-data t)]
        (println)
        (if (contains? m ::message)
          (do (println (str "Discourje Examples (" (version) ")"))
              (println (str "Error: " (::message m)))
              (println (str "Usage 1: java -jar discourje-examples.jar run <lib> <program> <input>"))
              (println (str "  <lib>     \u2208 {clj, dcj, dcj-nil}"))
              (println (str "  <program> \u2208 {micro.ring, micro.mesh, micro.star}"))
              (println (str "Usage 2: java -jar discourje-examples.jar configs <m>")))
          (.printStackTrace t))
        (println)))))
