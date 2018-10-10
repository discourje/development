(ns discourje.twoBuyerProtocol.pipes.proto
  (:require [clojure.core.async :as async :refer :all]
            [discourje.twoBuyerProtocol.pipes.util :refer :all]))

;defire a prot(col) record
(defrecord proto [pipelines])
;define a proto(col) as a record with a vector of pipelines
(def twoBuyerProto (->proto []))
;create a method to setup the pipelines, uses a tagfiler to make sure only the necessary messages come through
(defn createTwoBuyerProto [buyer1 buyer2 seller]
  (cons (:pipelines twoBuyerProto) (->setupPipeLineSequencing buyer1 seller (->tagFilter :title))) ; pipe for title to seller
  (cons (:pipelines twoBuyerProto) (->setupPipeLineSequencing seller buyer1 (->tagFilter :quote))); pipe for quote to buyer 1
  (cons (:pipelines twoBuyerProto) (->setupPipeLineSequencing seller buyer2 (->tagFilter :quote))) ; pipe for quote to buyer 2
  (cons (:pipelines twoBuyerProto) (->setupPipeLineSequencing buyer1 buyer2 (->tagFilter :quoteDiv))) ;pipe for quote div to buyer2
  (cons (:pipelines twoBuyerProto) (->setupPipeLineSequencing buyer2 seller (->tagFilter :ok))) ; pipe for ok to seller 2
  (cons (:pipelines twoBuyerProto) (->setupPipeLineSequencing buyer2 seller (->tagFilter :address))) ; pipe for address to seller
  (cons (:pipelines twoBuyerProto) (->setupPipeLineSequencing seller buyer2 (->tagFilter :date))) ; pipe for date to buyer2
  (cons (:pipelines twoBuyerProto) (->setupPipeLineSequencing buyer2 seller (->tagFilter :quit)))) ; pipe for quite to seller