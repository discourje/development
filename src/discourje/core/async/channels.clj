(ns discourje.core.async.channels)

(defn- get-transitions [id key transitions]
  (filter (fn [trans]
              (= (key trans) id))
          transitions))

(defn get-transitions-by-source [id transitions]
  (get-transitions id :source transitions))

(defn get-transitions-by-sink [id transitions]
  (get-transitions id :sink transitions))