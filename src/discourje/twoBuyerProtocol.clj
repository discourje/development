(ns discourje.twoBuyerProtocol
  (:require [discourje.core :as async :refer :all]
            [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]))

(def buyer1 (->participant (async/thread) (chan) (chan)))
(def buyer2 (->participant (async/thread) (chan) (chan)))
(def seller (->participant (async/thread) (chan) (chan)))

(defn twoBuyers [b1 b2 s]
  )