(ns discourje.multi.channelGenerationTests
  (:require [clojure.test :refer :all]
            [discourje.multi.core :refer :all]))

(let [participants [1 2 3]
      uniques (discourje.multi.core/uniqueCartesianProduct participants participants)]
uniques
  )
