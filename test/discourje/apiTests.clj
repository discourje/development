(ns discourje.apiTests
  (:require [clojure.test :refer :all][discourje.api.api :refer :all]))

(def participant(generateParticipant "buyer1" []))

(macroexpand '(s! "title" "helloWorld" participant "seller"))
(s! "title" "helloWorld" participant "seller")