(ns discourje.apiTests
  (:require [clojure.test :refer :all][discourje.api.api :refer :all][discourje.core.dataStructures :refer :all]))

(def participant(generateParticipant "buyer1" []))

(macroexpand '(s! "title" "helloWorld" participant "seller"))

(defn generateBook[x]
  (format "Hi " x))

(defmacro >s!!>
  "chained-send macro -> WIP!"
  ([action function sender receiver f]
   `(fn [~'callback-value]
      (send-to ~sender ~action (~function ~'callback-value) ~receiver))
   `~f))

(macroexpand '(>s!!> "title" generateBook participant "seller" (fn [x] (format "hi "x))))

(fn [x] (format "hi "x))