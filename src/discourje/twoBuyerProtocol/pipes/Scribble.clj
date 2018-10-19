(ns discourje.twoBuyerProtocol.pipes.Scribble
  (:require [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]))

(def b1 (chan))
(def b2 (chan))

(defrecord message [content tag])
(def validMessage (->message "valid" :valid))
(def invalidMessage (->message "invalid" :invalid))

(def messageFilter (fn [x] (filter (:tag x))))
(def messageFilterT (filter (fn [x] (= (:tag x) :valid))))


;(def validFilter? (fn [x] (= (:tag x) :valid)))

(defmacro filterM [tag]
  `(fn [~'x] (= (:tag ~'x) ~tag)))

;(filterM :valid)
(macroexpand `(filterM :valid))

(= true (filterM :valid) validMessage)



(defn createPipeline
  "Test for pipeline to use transducer as operation"
  [from to operation]
  (pipeline 1 to operation from))

(go (>! b1 invalidMessage))
(go (>! b1 validMessage))


(createPipeline b1 b2 messageFilterT)

(go (println :content (<! b2)))




