(ns discourje.twoBuyerProtocol.pipes.participant
  (:require [clojure.core.async :as async :refer :all]))


(defn participant [name]
  (def name (chan)))