(ns research.tbpTests.pipesTests.utilTests
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]
            [research.twoBuyerProtocol.pipes.util :refer :all]))

(def channel1 (chan))
(def channel2 (chan))
(def stringFilter (map->stringFilter {}))
(def tagFilt (->tagFilter :h))
(def testPipeline
  (->setupPipeLineSequencing channel1 channel2 (filt stringFilter (go (<! channel2)))))

(deftest stringFilterTest
  (is (= true (filt stringFilter "test"))))

(deftest tagFilterOkTest
  (is (= true (filt tagFilt (->message :h "hello")))))
