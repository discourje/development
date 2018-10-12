(ns discourje.twoBuyerProtocol.pipes.proto
  (:require [clojure.core.async :as async :refer :all]
            [discourje.twoBuyerProtocol.pipes.util :refer :all]))

; from buyer1 to seller, send a book title
; from seller to buyer1 and buyer2, send quote
; from buyer1 to buyer2, send amount willing to pay (quoteDiv)
; BRANCH quoteDiv
;-confirmed:
; from buyer2 to seller, send ok
; from buyer2 to seller, send address
; from seller to buyer2, send date when address received
;-declined:
; from buyer2 to seller, send quit
(def buyer1 (chan))
(def buyer2 (chan))
(def seller (chan))

(defn generatePipeline [from to filter operation]
  (pipeline 1 to filter from)
  (go-loop []
    (operation (<! to))))

(generatePipeline
  buyer1
  seller
  (filter (fn [m] (= (:tag m) :title)))
  (print))
(go (>! buyer1 (->message :title "yo")))

(defn printt [message]
  (print "ok"))

;define a proto(col) record
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

(defn generateQuote [a])