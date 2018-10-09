(ns discourje.twoBuyerProtocol.pipes.proto
  (:require [clojure.core.async :as async :refer :all]
            [discourje.twoBuyerProtocol.pipes.util :refer :all]))


(defrecord proto [pipelines])
(def twoBuyerProto (->proto []))

(defn createTwoBuyerProto [buyer1 buyer2 seller]
  (cons (:pipelines twoBuyerProto) (->setupPipeLineSequencing buyer1 seller (->stringFilter)))
  (cons (:pipelines twoBuyerProto) (->setupPipeLineSequencing buyer1 seller (->stringFilter))))