;(ns discourje.core.async.impl.dsl.syntax-tests
;  (:require [clojure.test :refer :all]
;            [discourje.core.async.impl.test-data :refer :all]
;            [discourje.core.async.impl.dsl.abstract :refer :all]))
;
;;;;;
;;;;; monitorTests.clj
;;;;;
;
;(deftest get-active-interaction-test
;  (let [mon (generate-spec (testDualProtocol false))]
;    (println mon)
;    (is (= (get-active-interaction mon) testDualProtocolControl))
;    (is (= "A" (get-sender (get-active-interaction mon))))
;    (is (= "B" (get-receivers (get-active-interaction mon))))))
;
;(deftest triple-protocol-ids-test
;  (let [mon (generate-spec (testTripleProtocol false))]
;    (is (= (get-active-interaction mon) testTripleProtocolControl))))
;
;(deftest parallel-protocol-ids-test
;  (let [mon (generate-spec (testMulticastProtocol false))]
;    (is (= (get-active-interaction mon) testMulticastProtocolControl))))
;
;(deftest quad-protocol-ids-test
;  (let [mon (generate-spec (testQuadProtocol false))]
;    (is (= (get-active-interaction mon) testQuadProtocolControl))))
;
;(deftest single-choice-in-middle-protocol-ids-test
;  (let [mon (generate-spec (single-choice-in-middle-protocol false))]
;    (is (= (get-active-interaction mon) single-choice-in-middle-protocolControl))))
;
;(deftest single-choice-5branches-protocol-ids-test
;  (let [mon (generate-spec (single-choice-5branches-protocol false))]
;    (is (= (get-active-interaction mon) single-choice-5branches-protocolControl))))
;
;(deftest dual-choice-protocol-ids-test
;  (let [mon (generate-spec (dual-choice-protocol false))]
;    (is (= (get-active-interaction mon) dual-choice-protocolControl))))
;
;(deftest multiple-nested-choice-branch-protocol-ids-test
;  (let [mon (generate-spec (multiple-nested-choice-branch-protocol false))]
;    (is (= (get-active-interaction mon) multiple-nested-choice-branch-protocolControl))))
;
;(deftest single-choice-multiple-interactions-protocol-test
;  (let [mon (generate-spec (single-choice-multiple-interactions-protocol false))]
;    (is (= (get-active-interaction mon) single-choice-multiple-interactions-protocolControl))))
;
;(deftest multiple-nested-branches-protocol-ids-test
;  (let [mon (generate-spec (multiple-nested-branches-protocol false))]
;    (is (= (get-active-interaction mon) multiple-nested-branches-protocolControl))))
;
;(deftest nested-recur-protocol-ids-test
;  (let [mon (generate-spec (nested-recur-protocol false))]
;    (println (:recursion-set mon))
;    (is (= (get-active-interaction mon) nested-recur-protocolControl))))
;
;(deftest parallel-with-rec-protocol-ids-test
;  (let [mon (generate-spec (parallel-with-rec false))]
;    (is (= (get-active-interaction mon) parallel-with-rec-control))
;    (is (not-empty @(:recursion-set mon)))))
;
;(deftest one-recur-with-choice-protocol-ids-test
;  (let [mon (generate-spec (one-recur-with-choice-protocol false))]
;    (println (:recursion-set mon))
;    (is (= (get-active-interaction mon) one-recur-with-choice-protocolControl))))
;
;(deftest rec-with-parallel-with-choice-multicast-ids-test
;  (let [mon (generate-spec (rec-with-parallel-with-choice-multicast false))]
;    (println (:recursion-set mon))
;    (is (not-empty @(:recursion-set mon)))))
;(deftest one-recur-with-startchoice-and-endchoice-protocol-ids-test
;  (let [mon (generate-spec (one-recur-with-startchoice-and-endchoice-protocol false))]
;    (is (= (get-active-interaction mon) one-recur-with-startchoice-and-endchoice-protocolControl))))
;
;(deftest two-buyer-protocol-ids-test
;  (let [mon (generate-spec (two-buyer-protocol false))]
;    (is (= (get-active-interaction mon) two-buyer-protocolControl))))
;
;(deftest parallel-after-interaction-test
;  (let [mon (generate-spec (parallel-after-interaction false))]
;    (is (= (get-active-interaction mon) parallel-after-interactionControl))))
;
;(deftest parallel-after-interaction-with-after-test
;  (let [mon (generate-spec (parallel-after-interaction-with-after false))]
;    (is (= (get-active-interaction mon) parallel-after-interaction-with-afterControl))))
;
;(deftest parallel-after-choice-with-after-test
;  (let [mon (generate-spec (parallel-after-choice-with-after false))]
;    (is (= (get-active-interaction mon) parallel-after-choice-with-afterControl))))
;
;(deftest parallel-after-choice-with-after-choice-test
;  (let [mon (generate-spec (parallel-after-choice-with-after-choice false))]
;    (is (= (get-active-interaction mon) parallel-after-choice-with-after-choiceControl))))
;
;(deftest parallel-after-rec-with-after-test
;  (let [mon (generate-spec (parallel-after-rec-with-after false))]
;    (is (= (get-active-interaction mon) parallel-after-rec-with-afterControl))))
;
;(deftest parallel-after-rec-with-after-rec-test
;  (let [mon (generate-spec (parallel-after-rec-with-after-rec false))]
;    (is (= (get-active-interaction mon) parallel-after-rec-with-after-recControl))))
;
;(deftest nested-parallel-test
;  (let [mon (generate-spec (nested-parallel false))]
;    (is (= (get-active-interaction mon) nested-parallelControl))))
;
;(deftest after-parallel-nested-parallel-test
;  (let [mon (generate-spec (after-parallel-nested-parallel false))]
;    (is (= (get-active-interaction mon) after-parallel-nested-parallelControl))))
;
;(deftest rec-with-parallel-with-choice-multicast-and-close-test
;  (let [mon (generate-spec (rec-with-parallel-with-choice-multicast-and-close false))]
;    (is (not-empty @(:recursion-set mon)))
;    (is (= (get-active-interaction mon) rec-with-parallel-with-choice-multicast-and-closeControl))))
;
;;;;;
;;;;; parameterizedRecTests.clj (partial)
;;;;;
;
;(defn single-recur-protocol-params [include-ids]
;  (if include-ids (create-protocol [(make-interaction (message-checker "1") "A" "B")
;                                    (make-recursion [:test {:r1 "A" :r2 "B" :r3 "C"}]
;                                                    [(make-interaction (message-checker "1") :r2 :r1)
;                                                     (make-choice [
;                                                                   [(make-interaction (message-checker "2") :r1 :r3)
;                                                                    (make-interaction (message-checker "2") :r3 :r1)
;                                                                    (do-recur [:test [:r1 :r2 :r3]])]
;                                                                   [(make-interaction (message-checker "3") :r1 :r2)
;                                                                    ]
;                                                                   ])
;                                                     ])
;                                    (make-interaction (message-checker "end") "A" ["B" "C"])
;                                    ])
;                  (create-protocol [(->Interaction nil nil "A" "B" #{} nil)
;                                    (->Recursion nil [:test {:r1 "A" :r2 "B" :r3 "C"}]
;                                                 [(->Interaction nil nil :r2 :r1 #{} nil)
;                                                  (->Choice nil [
;                                                                 [(->Interaction nil nil :r1 :r3 #{} nil)
;                                                                  (->Interaction nil nil :r3 :r1 #{} nil)
;                                                                  (->Continue nil [:test [:r1 :r2 :r3]] :recur nil)]
;                                                                 [(->Interaction nil nil :r1 :r2 #{} nil)]] nil)
;                                                  ] nil)
;                                    (->Interaction nil nil "A" ["B" "C"] #{} nil)
;                                    ])))
;
;(def single-recur-protocol-paramsControl
;  (->Interaction nil nil "A" "B" #{}
;                 (->Interaction nil nil "B" "A" #{}
;                                (->Choice nil [
;                                               (->Interaction nil nil "A" "C" #{}
;                                                              (->Interaction nil nil "C" "A" #{}
;                                                                             (->Continue nil [:test [:r1 :r2 :r3]] :recur nil)))
;                                               (->Interaction nil nil "A" "B" #{} (->Interaction nil nil "A" ["B" "C"] #{} nil))
;
;                                               ] nil))
;                 )
;  )
;
;(deftest single-recur-protocol-params-test
;  (let [mon (generate-spec (single-recur-protocol-params false))]
;    (is (= (get-active-interaction mon) single-recur-protocol-paramsControl))))
;
;(deftest unique6-roles-single-recur-params-test
;  (is (= 6 (count (get-distinct-role-pairs (get-interactions (single-recur-protocol-params true)))))))
;
;;;;;
;;;;; rolesTests.clj
;;;;;
;
;(deftest unique2-roles-test
;  (is (= 2 (count (get-distinct-role-pairs (get-interactions (testDualProtocol true)))))))
;
;(deftest unique3-roles-test
;  (is (= 3 (count (get-distinct-role-pairs (get-interactions (testTripleProtocol true)))))))
;
;(deftest unique5-multicast-roles-test
;  (is (= 5 (count (get-distinct-role-pairs (get-interactions (testMulticastProtocol true)))))))
;
;(deftest unique8-multicast-roles-test
;  (is (= 8 (count (get-distinct-role-pairs (get-interactions (testQuadProtocol true)))))))
;
;(deftest unique2-roles-single-choice-test
;  (is (= 2 (count (get-distinct-role-pairs (get-interactions (single-choice-protocol)))))))
;
;(deftest unique6-roles-single-choice-5branches-test
;  (is (= 6 (count (get-distinct-role-pairs (get-interactions (single-choice-5branches-protocol true)))))))
;
;(deftest unique5-roles-dual-choice-test
;  (is (= 5 (count (get-distinct-role-pairs (get-interactions (dual-choice-protocol true)))))))
;
;(deftest unique6get-distinct-role-pairs-roles-single-choice-multiple-interactions-protocol-test
;  (is (= 6 (count (get-distinct-role-pairs (get-interactions (single-choice-multiple-interactions-protocol true)))))))
;
;(deftest unique4-roles-single-nested-branch-choice-test
;  (is (= 4 (count (get-distinct-role-pairs (get-interactions (single-nested-choice-branch-protocol)))))))
;
;(deftest unique12-roles-multiple-nested-branch-choice-test
;  (is (= 12 (count (get-distinct-role-pairs (get-interactions (multiple-nested-branches-protocol true)))))))
;
;(deftest unique4-roles-single-recur-test
;  (is (= 4 (count (get-distinct-role-pairs (get-interactions (single-recur-protocol true)))))))
;
;(deftest unique6-roles-nested-recur-protocol-testt
;  (is (= 6 (count (get-distinct-role-pairs (get-interactions (nested-recur-protocol true)))))))
;
;(deftest unique6-roles-multiple-nested-recur-protocol-test
;  (is (= 6 (count (get-distinct-role-pairs (get-interactions (multiple-nested-recur-protocol)))))))
;
;(deftest unique-minimum-role-pairs-test
;  (let [roles (get-distinct-role-pairs (get-interactions (testQuadProtocol true)))]
;    (is (== 8 (count roles)))))
;
;(deftest unique-minimum-multiple-nested-branches-protocol-role-pairs-test
;  (let [roles (get-distinct-role-pairs (get-interactions (multiple-nested-branches-protocol true)))]
;    (is (== 12 (count roles)))))
;
;(deftest unique-minimum-single-recur-protocol-role-pairs-test
;  (let [roles (get-distinct-role-pairs (get-interactions (single-recur-protocol true)))]
;    (is (== 4 (count roles)))))
;
;(deftest two-buyer-protocol-role-test
;  (let [roles (get-distinct-role-pairs (get-interactions (two-buyer-protocol true)))]
;    (is (== 5 (count roles)))))
;
;(deftest two-buyer-protocol-role-pairs-test
;  (let [roles (get-distinct-role-pairs (get-interactions (two-buyer-protocol true)))]
;    (is (== 5 (count roles)))))
;
;;;;;
;;;;; stringifyTests.clj
;;;;;
;
;(deftest stringify-interaction-test
;  (is (= "Interaction - Action: 1, Sender: A, Receivers: B with accepted sends #{}" (to-string (make-interaction "1" "A" "B")))))
;
;(deftest stringify-close-test
;  (is (= "Closer from Sender: A to Receiver: B" (to-string (make-closer "A" "B")))))
;
;(deftest stringify-branch-test
;  (is (= "Branching with branches - [ Interaction - Action: 1, Sender: A, Receivers: B with accepted sends #{} ][ Interaction - Action: 1, Sender: A, Receivers: B with accepted sends #{} ]" (to-string (get-active-interaction (generate-spec (create-protocol [(make-choice [[(make-interaction "1" "A" "B")] [(make-interaction "1" "A" "B")]])])))))))
;
;(deftest stringify-parallel-test
;  (is (= "Parallel with parallels - [ Interaction - Action: 1, Sender: A, Receivers: B with accepted sends #{} ][ Interaction - Action: 1, Sender: A, Receivers: B with accepted sends #{} ]" (to-string (get-active-interaction (generate-spec (create-protocol [(make-parallel [[(make-interaction "1" "A" "B")] [(make-interaction "1" "A" "B")]])])))))))
;
;(deftest stringify-identifiable-recur-test
;  (is (= "Recur-identifier - name: :rec, option: :recur" (to-string (do-recur :rec)))))
;
;;(deftest stringify-channel-test
;;  (is (= "channel with Provider a, Consumer b and buffer 1" (to-string (generate-channel "a" "b" nil 1)))))