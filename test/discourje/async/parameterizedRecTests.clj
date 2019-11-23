(ns discourje.async.parameterizedRecTests
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]
            [discourje.async.protocolTestData :refer :all]))

(defn single-recur-protocol-params [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker "1") "A" "B")
                                    (make-recursion [:test {:r1 "A" :r2 "B" :r3 "C"}]
                                                    [(make-interaction (message-checker "1") :r2 :r1)
                                                     (make-choice [
                                                                   [(make-interaction (message-checker "2") :r1 :r3)
                                                                    (make-interaction (message-checker "2") :r3 :r1)
                                                                    (do-recur [:test {:r1 "A" :r2 "B" :r3 "C"}])]
                                                                   [(make-interaction (message-checker "3") :r1 :r2)
                                                                    ]
                                                                   ])
                                                     ])
                                    (make-interaction (message-checker "end") "A" ["B" "C"])
                                    ])
                  (create-protocol [(->interaction nil nil "A" "B" #{} nil)
                                    (->recursion nil [:test {:r1 "A" :r2 "B" :r3 "C"}]
                                                 [(->interaction nil nil :r2 :r1 #{} nil)
                                                  (->branch nil [
                                                                 [(->interaction nil nil :r1 :r3 #{} nil)
                                                                  (->interaction nil nil :r3 :r1 #{} nil)
                                                                  (->recur-identifier nil [:test {:r1 "A" :r2 "B" :r3 "C"}] :recur nil)]
                                                                 [(->interaction nil nil :r1 :r2 #{} nil)]] nil)
                                                  ] nil)
                                    (->interaction nil nil "A" ["B" "C"] #{} nil)
                                    ])))

(def single-recur-protocol-paramsControl
  (->interaction nil nil "A" "B" #{}
                 (->interaction nil nil :r2 :r1 #{}
                                (->branch nil [
                                               (->interaction nil nil :r1 :r3 #{}
                                                              (->interaction nil nil :r3 :r1 #{}
                                                                             (->recur-identifier nil [:test {:r1 "A" :r2 "B" :r3 "C"}] :recur nil)))
                                               (->interaction nil nil :r1 :r2 #{} (->interaction nil nil "A" ["B" "C"] #{} nil))

                                               ] nil))
                 )
  )

(deftest parallel-after-rec-with-after-test
  (let [mon (generate-monitor (single-recur-protocol-params false))]
    (is (= (get-active-interaction mon) single-recur-protocol-paramsControl))))