(ns discourje.async.parameterizedRecTests
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.async.operationTests :refer :all]))

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
                 (->interaction nil nil "B" "A" #{}
                                (->branch nil [
                                               (->interaction nil nil "A" "C" #{}
                                                              (->interaction nil nil "C" "A" #{}
                                                                             (->recur-identifier nil [:test {:r1 "A" :r2 "B" :r3 "C"}] :recur nil)))
                                               (->interaction nil nil "A" "B" #{} (->interaction nil nil "A" ["B" "C"] #{} nil))

                                               ] nil))
                 )
  )

(deftest single-recur-protocol-params-test
  (let [mon (generate-monitor (single-recur-protocol-params false))]
    (is (= (get-active-interaction mon) single-recur-protocol-paramsControl))))

(deftest unique6-roles-single-recur-params-test
  (is (= 6 (count (get-distinct-role-pairs (get-interactions (single-recur-protocol-params true)))))))

(deftest send-receive-single-recur-protocol-params
  (let [channels (generate-infrastructure (single-recur-protocol-params true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        ac (get-channel channels "A" "C")
        ca (get-channel channels "C" "A")
        flag (atom false)]
    (do (>!! ab (->message "1" "AB"))
        (let [a->b (<!!-test ab)]
          (is (= "AB" a->b))
          (while (false? @flag)
            (>!! ba (->message "1" "AB"))
            (let [b->a (<!!-test ba)]
              (is (= "AB" b->a))
              (if (== 1 (+ 1 (rand-int 2)))
                (do
                  (>!! ac (->message "2" "AC"))
                  (let [a->c (<!!-test ac)]
                    (is (= "AC" a->c))
                    (>!! ca (->message "2" "AC"))
                    (let [c->a (<!!-test ca)]
                      (is (= "AC" c->a)))))
                (do
                  (>!! ab (->message "3" "AB3"))
                  (let [a->b3 (<!!-test ab)]
                    (is (= "AB3" a->b3))
                    (reset! flag true))))))
          (>!! [ab ac] (->message "end" "ending"))
          (let [a->b-end (<!!-test ab)
                a->c-end (<!!-test ac)]
            (is (= "ending" a->b-end))
            (is (= "ending" a->c-end)))))))

(defn rec-with-parallel-with-choice-multicast-and-close-params [include-ids]
  (if include-ids (create-protocol [(make-recursion [:test {:r1 "a" :r2 "b" :r3 "c"}]
                                                    [(make-parallel [[(make-choice [[(make-interaction (message-checker 1) :r1 [:r2 :r3])]
                                                                                    [(make-interaction (message-checker 0) :r1 [:r2 :r3])
                                                                                     (do-recur [:test {:r1 "a" :r2 "b" :r3 "c"}])]])]
                                                                     [(make-interaction (message-checker 4) :r2 [:r1 :r3])
                                                                      (make-interaction (message-checker 5) :r1 [:r2 :r3])]
                                                                     ])
                                                     ])
                                    (make-closer "a" "b")
                                    (make-closer "a" "c")
                                    (make-interaction (message-checker 6) "b" ["a" "c"])
                                    (make-closer "b" "a")
                                    (make-closer "b" "c")])
                  (create-protocol [(->recursion nil :test [(->lateral nil [[(->branch nil [[(->interaction nil nil "a" ["b" "c"] #{} nil)]
                                                                                            [(->interaction nil nil "a" ["b" "c"] #{} nil)
                                                                                             (->recur-identifier nil :test :recur nil)]] nil)]
                                                                            [(->interaction nil nil "b" ["a" "c"] #{} nil)
                                                                             (->interaction nil nil "a" ["b" "c"] #{} nil)]
                                                                            ] nil)
                                                            ] nil)
                                    (->closer nil "a" "b" nil)
                                    (->closer nil "a" "c" nil)
                                    (->interaction nil nil "b" ["a" "c"] #{} nil)
                                    (->closer nil "b" "a" nil)
                                    (->closer nil "b" "c" nil)])))

(deftest send-and-receive-rec-with-parallel-with-choice-multicast-and-close-params-test
  (let [channels (add-infrastructure (rec-with-parallel-with-choice-multicast-and-close-params true))
        ab (get-channel channels "a" "b")
        ac (get-channel channels "a" "c")
        ba (get-channel channels "b" "a")
        bc (get-channel channels "b" "c")]
    (loop [reps 0]
      (if (> reps 2)
        (do
          (>!! [ab ac] (msg 1 1))
          (is (= (<!!-test ab) 1))
          (is (= (<!!-test ac) 1)))
        (do (>!! [ab ac] (msg 0 0))
            (is (= (<!!-test ab) 0))
            (is (= (<!!-test ac) 0))
            (do (>!! [ba bc] (msg 4 4))
                (let [b->a4 (<!!-test ba)
                      b->c4 (<!!-test bc)]
                  (is (= b->a4 4))
                  (is (= b->c4 4))
                  (>!! [ab ac] (msg 5 5))
                  (is (= (<!!-test ab) 5))
                  (is (= (<!!-test ac) 5))))
            (recur (+ reps 1)))))
    (do
      (close-channel! ab)
      (is true (channel-closed? ab))
      (is true (channel-closed? (get-channel channels "a" "b")))
      (close-channel! "a" "c" channels)
      (is true (channel-closed? ac))
      (is true (channel-closed? (get-channel channels "a" "c")))
      (>!! [ba bc] (msg 6 6))
      (let [b->a6 (<!!-test ba)
            b->c6 (<!!-test bc)]
        (is (= b->a6 6))
        (is (= b->c6 6))
        (close-channel! ba)
        (close-channel! bc)
        (is true (channel-closed? ba))
        (is true (channel-closed? (get-channel channels "b" "a")))
        (is true (channel-closed? bc))
        (is true (channel-closed? (get-channel channels "b" "c")))
        (is (nil? (get-active-interaction (get-monitor ab))))))))

