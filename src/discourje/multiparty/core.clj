(ns discourje.multiparty.core
  (:require [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]))

(def ^{:dynamic true} *debug-enabled* false)

(defn- log [level s]
  (println (apply str (cons (str "[" level "] ") s))))

(defn debug [ & s ]
  (when *debug-enabled*
    (log "Logging: " s)))

(defrecord communicationChannel [sender receiver channel])

(defn- generateChannel [sender receiver]
  (debug (format "generating channel between %s %s" sender receiver))
  (->communicationChannel sender receiver (chan)))

(defn- uniqueCartesianProduct [x y]
  (filter some?
          (for [x x y y]
            (when (not (identical? x y))
              (vector x y)))))

(defn generateChannels
  "Generates communication channels between all participants"
  [participants]
  (map #(apply generateChannel %) (uniqueCartesianProduct participants participants)))

