(ns discourje.core.async.buffers
  (:refer-clojure :exclude [type]))

(deftype Buffer [type n])

(defn buffer? [x]
  (= (clojure.core/type x) Buffer))

(defn fixed-buffer [n]
  {:pre [(> n 0)]}
  (->Buffer :fixed-buffer n))

(defn dropping-buffer [n]
  {:pre [(> n 0)]}
  (->Buffer :dropping-buffer n))

(defn sliding-buffer [n]
  {:pre [(> n 0)]}
  (->Buffer :sliding-buffer n))

;(defn promise-buffer []
;  {:pre [true]}
;  (->Buffer :promise-buffer 0))

(defn n [buffer]
  {:pre [(buffer? buffer)]}
  (.-n buffer))

(defn type [buffer]
  {:pre [(buffer? buffer)]}
  (.-type buffer))

;(defn unblocking-buffer? [buffer]
;  {:pre [(buffer? buffer)]}
;  (contains? #{:dropping-buffer :sliding-buffer :promise-buffer} (.-type buffer)))