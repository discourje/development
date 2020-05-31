(ns discourje.core.util
  (:require [discourje.core.async :as a]))

;;;;
;;;; Networks
;;;;

(defn- network
  [chs meta]
  (with-meta (fn
               ([] chs)
               ([i j] (get chs [i j])))
             (merge {:network true} meta)))

(defn ring [fn-chan ids]
  {:pre [(>= (count ids) 2)]}
  (network (loop [ids ids
                  m {[(last ids) (first ids)] (fn-chan)
                     [(first ids) (last ids)] (fn-chan)}]
             (if (or (empty? ids) (empty? (rest ids)))
               m
               (recur (rest ids) (merge m {[(first ids) (first (rest ids))] (fn-chan)
                                           [(first (rest ids)) (first ids)] (fn-chan)}))))
           {:network-type :ring}))

(defn star [fn-chan root-id leaf-ids]
  {:pre [(>= (count leaf-ids) 2)]}
  (network (reduce merge (for [i leaf-ids]
                           {[root-id i] (fn-chan)
                            [i root-id] (fn-chan)}))
           {:network-type :star
            :root-id      root-id}))

(defn mesh [fn-chan ids]
  {:pre [(>= (count ids) 2)]}
  (network (reduce merge (for [i ids
                               j (remove #{i} ids)]
                           {[i j] (fn-chan)}))
           {:network-type :mesh}))

;;;;
;;;; Operations on networks
;;;;

(defn putter-id [network c]
  {:pre [(contains? (meta network) :network)]}
  (first (first (first (filter #(= (second %) c) (network))))))

(defn taker-id [network c]
  {:pre [(contains? (meta network) :network)]}
  (second (first (first (filter #(= (second %) c) (network))))))

(defn puts [network [putter-id v] taker-ids]
  {:pre [(contains? (meta network) :network)]}
  (map (fn [taker-id] [(network putter-id taker-id) v]) taker-ids))

(defn takes [network putter-ids taker-id]
  {:pre [(contains? (meta network) :network)]}
  (map (fn [putter-id] (network putter-id taker-id)) putter-ids))

;;;;
;;;; Monitors
;;;;

(defn link-ring [network fn-role monitor]
  {:pre [(contains? #{:ring :mesh} (:network-type (meta network)))]}
  (doseq [[[i j] c] (network)]
    (a/link c (fn-role i) (fn-role j) monitor)))

(defn link-star [network fn-role-root fn-role-leaf monitor]
  {:pre [(contains? #{:star} (:network-type (meta network)))]}
  (let [root-id (:root-id (meta network))
        fn-role-root (if (= root-id nil)
                       (fn [_] (fn-role-root))
                       fn-role-root)]
    (doseq [[[i j] c] (network)]
      (if (= i root-id)
        (a/link c (fn-role-root i) (fn-role-leaf j) monitor))
      (if (= j root-id)
        (a/link c (fn-role-leaf i) (fn-role-root j) monitor)))))

(defn link-mesh [network fn-role monitor]
  {:pre [(contains? #{:ring :mesh} (:network-type (meta network)))]}
  (doseq [[[i j] c] (network)]
    (a/link c (fn-role i) (fn-role j) monitor)))
