(ns discourje.TwoBuyerProtocol.twoBuyersProtocol
  (require [discourje.core.monitor :refer :all]
           [discourje.core.protocol :refer :all]
           [discourje.core.dataStructures :refer :all]))

;(defn- defineRecurringProtocol []
;  (vector (->recursion :x
;                       (vector
;                         (->sendM "title" "buyer1" "seller")
;                         (->receiveM "title" "seller" "buyer1")
;                         (->sendM "quote" "seller" ["buyer1" "buyer2"])
;                         (->receiveM "quote" ["buyer1" "buyer2"] "seller")
;                         (->sendM "quoteDiv" "buyer1" "buyer2")
;                         (->receiveM "quoteDiv" "buyer2" "buyer1")
;                         (->choice [
;                                    (->sendM "ok" "buyer2" "seller")
;                                    (->sendM "address" "buyer2" "seller")
;                                    (->receiveM "ok" "seller" "buyer2")
;                                    (->receiveM "address" "seller" "buyer2")
;                                    (->sendM "date" "seller" "buyer2")
;                                    (->sendM "repeat" "seller" ["buyer2" "buyer1"])
;                                    (->receiveM "date" "buyer2" "seller")
;                                    (->receiveM "repeat" ["buyer2" "buyer1"] "seller")
;                                    (generateRecur :x)
;                                    ]
;                                   [
;                                    (->sendM "quit" "buyer2" "seller")
;                                    (->receiveM "quit" "seller" "buyer2")
;                                    (generateRecurStop :x)
;                                    ])))))

(defn- defineRecurringProtocol []
  (vector (->recursion :x
               (vector
                 (->sendM "title" "buyer1" "seller")
                 (->receiveM "title" "seller" "buyer1")
                 (->sendM "quote" "seller" ["buyer1" "buyer2"])
                 (->receiveM "quote" ["buyer1" "buyer2"] "seller")
                 (->sendM "quoteDiv" "buyer1" "buyer2")
                 (->receiveM "quoteDiv" "buyer2" "buyer1")
                 (->choice [
                            (->sendM "ok" "buyer2" "seller")
                            (->choice [
                                       (->sendM "address" "buyer2" "seller")
                                       (->receiveM "ok" "seller" "buyer2")
                                       (->receiveM "address" "seller" "buyer2")
                                       ]
                                      [
                                       (->receiveM "ok" "seller" "buyer2")
                                       (->sendM "address" "buyer2" "seller")
                                       (->receiveM "address" "seller" "buyer2")
                                       ])
                            (->sendM "date" "seller" "buyer2")
                            (->receiveM "date" "buyer2" "seller")
                            (->sendM "repeat" "buyer2" ["seller" "buyer1"])
                            (->receiveM "repeat" ["seller" "buyer1"] "buyer2")
                            (generateRecur :x)
                            ]
                           [
                            (->sendM "quit" "buyer2" "seller")
                            (->receiveM "quit" "seller" "buyer2")
                            (generateRecurStop :x)
                            ])))))

(defn getProtocol
  "generate the protocol, channels and set the first monitor active"
  []
  (generateProtocol ["buyer1" "buyer2" "seller"] (defineRecurringProtocol)))