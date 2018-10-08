(ns discourje.twoBuyerProtocol.pipes.participant
  (:require [clojure.core.async :as async :refer :all]))

(defrecord participant [name]
  (chan))