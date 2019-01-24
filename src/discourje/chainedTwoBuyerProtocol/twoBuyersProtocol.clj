(ns discourje.chainedTwoBuyerProtocol.twoBuyersProtocol
  (require [discourje.api.api :refer :all]
           [discourje.core.protocol :refer :all]))

(defn- defineRecurringProtocol []
  [(monitor-recursion :x
                      [
                       (monitor-send "title" "buyer1" "seller")
                       (monitor-receive "title" "seller" "buyer1")
                       (monitor-send "quote" "seller" ["buyer1" "buyer2"])
                       (monitor-receive "quote" ["buyer1" "buyer2"] "seller")
                       (monitor-send "quoteDiv" "buyer1" "buyer2")
                       (monitor-receive "quoteDiv" "buyer2" "buyer1")
                       (monitor-choice [
                                        (monitor-send "ok" "buyer2" "seller")
                                        (monitor-receive "ok" "seller" "buyer2")
                                        (monitor-send "address" "buyer2" "seller")
                                        (monitor-receive "address" "seller" "buyer2")
                                        (monitor-send "date" "seller" "buyer2");
                                        (monitor-receive "date" "buyer2" "seller")
                                        (monitor-send "repeat" "buyer2" ["seller" "buyer1"])
                                        (monitor-receive "repeat" ["seller" "buyer1"] "buyer2")
                                        (do-recur :x)
                                        ]
                                       [
                                        (monitor-send "quit" "buyer2" "seller")
                                        (monitor-receive "quit" "seller" "buyer2")
                                        (do-end-recur :x)
                                        ])])])

(defn getProtocol
  "generate the protocol, channels and set the first monitor active"
  []
  (generateProtocol (defineRecurringProtocol)))