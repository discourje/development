(ns discourje.putTests
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer :all :as async]))
(def c (chan))

;; We can wait until the put is finished by passing a callback

(put! c "Hello World" (fn [v] (println "Done putting")))


(take! c (fn [v] (println "Got " v)))