(ns discourje.core.async.impl.buffers
  (:require [clojure.core.async :as a]))

(deftype Buffer [type n])

(defn buffer? [x]
  (= (type x) Buffer))

(defn fixed-buffer [n]
  {:pre [(>= n 0)]}
  (->Buffer :fixed-buffer n))

(defn dropping-buffer [n]
  {:pre [(> n 0)]}
  (->Buffer :dropping-buffer n))

(defn sliding-buffer [n]
  {:pre [(> n 0)]}
  (->Buffer :sliding-buffer n))

(defn promise-buffer
  {:pre [true]}
  (->Buffer :promise-buffer 0))

(defn capacity [buffer]
  {:pre [(buffer? buffer)]}
  (.-n buffer))

(defn unblocking-buffer? [buffer]
  {:pre [(buffer? buffer)]}
  (contains? #{:dropping-buffer :sliding-buffer :promise-buffer} (.-type x)))

(defn clojure-core-async-chan [buffer]
  {:pre [(buffer? buffer)]}
  (let [type (.-type buffer)
        n (.-n buffer)]
    (case type
      :fixed-buffer (if (= n 0) (a/chan) (a/chan n))
      :dropping-buffer (a/dropping-buffer n)
      :sliding-buffer (a/sliding-buffer n)
      :promise-buffer (throw (IllegalArgumentException.)))))