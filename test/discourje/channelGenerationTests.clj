(ns discourje.channelGenerationTests
  (:require [clojure.test :refer :all]
            [discourje.core.core :refer :all]))

(let [participants [1 2 3]
      uniques (discourje.core.core/uniqueCartesianProduct participants participants)]
  (println uniques)
  )
(let [channels (generateChannels ["a" "b" "c"])]
  (doseq [chan channels]
    (clojure.core.async/close! (:channel chan))))