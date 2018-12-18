(ns discourje.multiparty.monitoringTests
  (:require [clojure.test :refer :all]
            [clojure.core :refer :all]))

(def protocol (atom (discourje.multiparty.TwoBuyersProtocol/getProtocol)))
