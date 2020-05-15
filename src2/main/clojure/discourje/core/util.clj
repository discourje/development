(ns discourje.core.util
  (:require [discourje.core.async :as a]))

;;;;
;;;; Networks
;;;;

(defn- network [type m]
  (with-meta (fn
               ([] m)
               ([i j] (get m [i j])))
             {:network-type type}))

(defn ring [f ids]
  {:pre [(>= (count ids) 2)]}
  (network :ring (loop [ids ids
                        m {[(last ids) (first ids)] (f)
                           [(first ids) (last ids)] (f)}]
                   (if (or (empty? ids) (empty? (rest ids)))
                     m
                     (recur (rest ids) (merge m {[(first ids) (first (rest ids))] (f)
                                                 [(first (rest ids)) (first ids)] (f)}))))))

(defn star [f ids]
  {:pre [(>= (count ids) 2)]}
  (network :star (reduce merge (for [i (rest ids)]
                                 {[(first ids) i] (f)
                                  [i (first ids)] (f)}))))

(defn mesh [f ids]
  {:pre [(>= (count ids) 2)]}
  (network :mesh (reduce merge (for [i ids
                                     j (remove #{i} ids)]
                                 {[i j] (f)}))))

(defn putter-id [network ch]
  {:pre [(contains? (meta network) :network-type)]}
  (first (first (first (filter #(= (second %) ch) (network))))))

(defn getter-id [network ch]
  {:pre [(contains? (meta network) :network-type)]}
  (second (first (first (filter #(= (second %) ch) (network))))))

(defn puts [network [putter-id v] taker-ids]
  {:pre [(contains? (meta network) :network-type)]}
  (map (fn [taker-id] [(network putter-id taker-id) v]) taker-ids))

(defn takes [network putter-ids taker-id]
  {:pre [(contains? (meta network) :network-type)]}
  (map (fn [putter-id] (network putter-id taker-id)) putter-ids))

;;;;
;;;; Monitors
;;;;

(defn link-all [network f monitor]
  {:pre [(contains? (meta network) :network-type)]}
  (doseq [[[i j] c] (network)]
    (a/link c (f i) (f j) monitor)))