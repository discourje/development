(ns discourje.twoBuyerProtocol.pipes.ScribbleReplicate
  (:require [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]))

(defrecord participant [channel internalState])
(defn generateParticipant
  "Generates a new participant, trying to simulate a constructor.
  Creates a participant with a new channel and an empty map for internalState"
  [] (->participant (chan) {}))


(def buyer1 (generateParticipant))
(def buyer2 (generateParticipant))
(def seller (generateParticipant))

(defrecord pipeAndOp [pipe operation])
(def a (chan))
(def b (chan))
(def c (chan))

(defn getPipe [to from operation]
  (->pipeAndOp (pipeline 1 to (filter string?) from false)
               (go-loop []
                  (>! from operation)
                 (recur))))

(defn testerPipe []
  (getPipe a b (str "test-a-b"))
  (getPipe b c (str "test-b-c")))

(go (>! c "haha"))
(testerPipe)

(go (<! c))
(close! a)
(close! b)
(close! c)
(defn sequence
  "Creates a pipeline between from and to, calling operation when filter is true"
  [operation filter from to]
  ())


(defn twobuyers
  "Generates two buyer protocol as prescribed by documentation"
  [b1 b2 s])
