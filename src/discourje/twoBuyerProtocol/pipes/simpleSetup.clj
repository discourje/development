(ns discourje.twoBuyerProtocol.pipes.simpleSetup
  (:require [clojure.core.async :as async :refer :all]))

(def buyer1 (chan))
(def buyer2 (chan))
(def seller (chan))

(pipeline 1 seller (filter string?) buyer1)

(go (>! buyer1 "hello from buyer1 to seller"))
(go (println (<! seller)))

(defn messageTo [message to filter from operation]
  (pipeline 1 to (filter filter) from)
  (go (>! from message))
  (go-loop []
    (operation (<! to))))

(def greet (fn [x] (print x)))

(messageTo "heya" seller string? buyer1 greet)