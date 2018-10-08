(ns discourje.twoBuyerProtocol.pipes.proto
  (:require [clojure.core.async :as async :refer :all]))

(defrecord twoBuyerProt [buyer1 buyer2 seller]
  )