(ns discourje.core.async.deadlocks
  (:require [clojure.set :refer [intersection]]))

(defn- runtime-exception []
  (ex-info (str "[SESSION FAILURE] Deadlock!")
           {}))

(def n (atom -1))

(defn set-n! [newval]
  (reset! n newval))

(defn dec-n! []
  (swap! n dec))

(def pending (atom #{}))

(defn clear-pending! []
  (reset! pending {}))

(defn add-pending! [c]
  (when (<= 0 @n)
    (swap! pending (fn [m] (assoc m (Thread/currentThread) c)))))

(defn remove-pending! []
  (when (<= 0 @n)
    (swap! pending (fn [m] (dissoc m (Thread/currentThread))))))

(defn check []
  (if (= (count (distinct (vals @pending))) @n)
    (throw (runtime-exception))))