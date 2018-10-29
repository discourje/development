(ns discourje.twoBuyerProtocolTests
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]
            [discourje.core :refer :all]
            [discourje.twoBuyerProtocol :refer :all]))

(deftest generateQuoteTest
  (is (< 31 (generateQuote))))