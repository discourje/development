(ns discourje.examples.experimental.dsl
  (:gen-class)
  (require [clojure.walk :refer :all]
           [clojure.pprint :refer :all]
           [discourje.core.async :refer :all]))

;;
;; Macros
;;

;; TODO - The following unmaps aren't very elegant...
;(ns-unmap 'discourje.examples.experimental.dsl 'seq)
(ns-unmap 'discourje.examples.experimental.dsl 'par)

(defmacro -->
  ([sender receiver]
   `(list '~(quote -->fn) '~sender '~receiver 'nil))
  ([sender receiver message-type]
   `(list '~(quote -->fn) '~sender '~receiver '~message-type)))

(defmacro alt
  [first & rest]
  `(list '~(quote altfn) ~first ~@rest))

;(defmacro seq
;  [first & rest]
;  `(list '~(quote seqfn) ~first ~@rest))

(defmacro par
  [first & rest]
  `(list '~(quote parfn) ~first ~@rest))

(defmacro fix
  ([var]
   `(list '~(quote fixfn) ~var))
  ([var body]
   `(list '~(quote fixfn) ~var ~body)))

(defmacro rep
  [op [var range] body]
  `(cons (cond (= '~op '~(quote alt)) '~(quote altfn)
               (= '~op '~(quote seq)) '~(quote seqfn)
               (= '~op '~(quote par)) '~(quote parfn))
         (map #(prewalk-replace {'~var %} ~body) ~range)))

(defmacro spec
  ([sp]
   `(let [s# (eval ~sp)]
      (->protocol (if (vector? s#) s# [s#])))))
  ;([smap sp]
  ; `(spec (eval (prewalk-replace '~smap '~sp)))))

(defn monitor [spec]
  (generate-monitor spec))

(defn monitor-reset [monitor]
  (force-monitor-reset! monitor))

;;
;; Functions
;;

(defn role [name]
  (fn
    ([] (str name))
    ([i] (str name "[" i "]"))
    ([i j] (str name "[" i "," j "]"))))

(def id (atom 0))
(defn next-id [] (swap! id inc))

(defn -->fn
  [sender receiver message-type]
  (->interaction (next-id)
                 message-type
                 (if (fn? sender) (sender) sender)
                 (if (fn? receiver) (receiver) receiver)
                 #{}
                 nil))

(defn altfn
  [first & rest]
  (->branch (next-id)
            (vec (mapcat identity
                         (map #(if (vector? %)
                                 [%]
                                 (if (satisfies? branchable %)
                                   (get-branches %)
                                   [[%]]))
                              (cons first rest))))
            nil))

;(defn seqfn
;  ([] [])
;  ([first & rest] (into (if (vector? first) first [first])
;                        (apply seqfn rest))))

(defn parfn
  [first & rest]
  (->parallel (next-id)
              (vec (mapcat identity
                           (map #(if (vector? %)
                                   [%]
                                   (if (satisfies? parallelizable %)
                                     (get-parallel %)
                                     [[%]]))
                                (cons first rest))))
              nil))

(defn fixfn
  ([var]
   (->recur-identifier (next-id) var :recur nil))
  ([var body]
   (->recursion (next-id) var (if (vector? body) body [body]) nil)))

(def alice (role "alice"))
(def bob (role "bob"))

;;;
;;; "Tests" (inspect output manually...)
;;;
;
;(defmacro ruben-vs-sung
;  ([ruben sung]
;   `(ruben-vs-sung false ruben sung))
;  ([flag ruben sung]
;   (when (eval flag)
;     (println)
;     (println " ###")
;     (println)
;     (println "ruben:" ruben)
;     (println)
;     (pprint (eval ruben))
;     (println)
;     (println " sung:" sung)
;     (println)
;     (pprint (eval sung))
;     (println))))
;
;(defmacro ruben-vs-sung' [ruben sung]
;  `(ruben-vs-sung ~(list 'mep ruben) ~(list 'spec sung)))
;
;;;
;;; "Tests" - Communications
;;;
;
;(ruben-vs-sung
;  ;true
;  (mep (-->> Integer (alice) (bob)))
;  (spec (--> (alice) (bob) Integer)))
;
;;;
;;; "Tests" - Alternative compositions
;;;
;
;(ruben-vs-sung
;  ;true
;  (mep (choice [(-->> Integer (alice) (bob))]
;               [(-->> Boolean (alice) (bob))]))
;  (spec (alt (--> alice bob Integer)
;             (--> alice bob Boolean))))
;
;(ruben-vs-sung
;  ;true
;  (mep (choice [(-->> Integer (alice) (bob))]
;               [(-->> Boolean (alice) (bob))]
;               [(-->> Float (alice) (bob))]))
;  (spec (alt (--> (alice) (bob) Integer)
;             (--> (alice) (bob) Boolean)
;             (--> (alice) (bob) Float))))
;
;(ruben-vs-sung
;  ;true
;  (mep (choice [(-->> Integer (alice) (bob))]
;               [(-->> Boolean (alice) (bob))]
;               [(-->> Float (alice) (bob))]))
;  (spec (alt (--> (alice) (bob) Integer)
;             (alt (--> (alice) (bob) Boolean)
;                  (--> (alice) (bob) Float)))))
;
;(ruben-vs-sung
;  ;true
;  (mep (choice [(-->> Integer (alice) (bob))]
;               [(-->> Boolean (alice) (bob))]
;               [(-->> Float (alice) (bob))]))
;  (spec (alt (alt (--> (alice) (bob) Integer)
;                  (--> (alice) (bob) Boolean))
;             (--> (alice) (bob) Float))))
;
;;;
;;; "Tests" - Sequential compositions
;;;
;
;(ruben-vs-sung
;  ;true
;  (mep (-->> Integer (alice) (bob))
;       (-->> Boolean (alice) (bob)))
;  (spec (seq (--> (alice) (bob) Integer)
;             (--> (alice) (bob) Boolean))))
;
;(ruben-vs-sung
;  ;true
;  (mep (-->> Integer (alice) (bob))
;       (-->> Boolean (alice) (bob))
;       (-->> Float (alice) (bob)))
;  (spec (seq (--> (alice) (bob) Integer)
;             (--> (alice) (bob) Boolean)
;             (--> (alice) (bob) Float))))
;
;(ruben-vs-sung
;  ;true
;  (mep (-->> Integer (alice) (bob))
;       (-->> Boolean (alice) (bob))
;       (-->> Float (alice) (bob)))
;  (spec (seq (--> (alice) (bob) Integer)
;             (seq (--> (alice) (bob) Boolean)
;                  (--> (alice) (bob) Float)))))
;
;(ruben-vs-sung
;  ;true
;  (mep (-->> Integer (alice) (bob))
;       (-->> Boolean (alice) (bob))
;       (-->> Float (alice) (bob)))
;  (spec (seq (seq (--> (alice) (bob) Integer)
;                  (--> (alice) (bob) Boolean))
;             (--> (alice) (bob) Float))))
;
;;;
;;; "Tests" - Alternatve/sequential compositions
;;;
;
;(ruben-vs-sung
;  ;true
;  (mep (choice [(-->> Integer (alice) (bob))]
;               [(-->> Boolean (alice) (bob))
;                (-->> Float (alice) (bob))]))
;  (spec (alt (--> (alice) (bob) Integer)
;             (seq (--> (alice) (bob) Boolean)
;                  (--> (alice) (bob) Float)))))
;
;(ruben-vs-sung
;  ;true
;  (mep (choice [(-->> Integer (alice) (bob))
;                (-->> Boolean (alice) (bob))]
;               [(-->> Float (alice) (bob))]))
;  (spec (alt (seq (--> (alice) (bob) Integer)
;                  (--> (alice) (bob) Boolean))
;             (--> (alice) (bob) Float))))
;
;(ruben-vs-sung
;  ;true
;  (mep (-->> Integer (alice) (bob))
;       (choice [(-->> Boolean (alice) (bob))]
;               [(-->> Float (alice) (bob))]))
;  (spec (seq (--> (alice) (bob) Integer)
;             (alt (--> (alice) (bob) Boolean)
;                  (--> (alice) (bob) Float)))))
;
;(ruben-vs-sung
;  ;true
;  (mep (choice [(-->> Integer (alice) (bob))]
;               [(-->> Boolean (alice) (bob))])
;       (-->> Float (alice) (bob)))
;  (spec (seq (alt (--> (alice) (bob) Integer)
;                  (--> (alice) (bob) Boolean))
;             (--> (alice) (bob) Float))))
;
;;;
;;; "Tests" - Recursion
;;;
;
;(ruben-vs-sung
;  ;true
;  (mep (rec :X (continue :X)))
;  (spec (fix :X (fix :X))))
;
;(ruben-vs-sung
;  ;true
;  (mep (rec :X (rec :Y (continue :X))))
;  (spec (fix :X (fix :Y (fix :X)))))
;
;(ruben-vs-sung
;  ;true
;  (mep (rec :X (-->> Integer (alice) (bob))))
;  (spec (fix :X (--> (alice) (bob) Integer))))
;
;(ruben-vs-sung
;  ;true
;  (mep (rec :X
;            (-->> Integer (alice) (bob))
;            (continue :X)))
;  (spec (fix :X
;             (seq (--> alice bob)
;                  (fix :X)))))
;(ruben-vs-sung
;  ;true
;  (mep (-->> Integer (alice) (bob))
;       (rec :X
;            (-->> Boolean (alice) (bob))
;            (-->> Float (alice) (bob))))
;  (spec (seq (--> (alice) (bob) Integer)
;             (fix :X (seq (--> (alice) (bob) Boolean)
;                          (--> (alice) (bob) Float))))))
;
;(ruben-vs-sung
;  ;true
;  (mep (rec :X
;            (-->> Integer (alice) (bob))
;            (-->> Boolean (alice) (bob)))
;       (-->> Float (alice) (bob)))
;  (spec (seq (fix :X (seq (--> (alice) (bob) Integer)
;                          (--> (alice) (bob) Boolean)))
;             (--> (alice) (bob) Float))))
;
;(ruben-vs-sung
;  ;true
;  (mep (rec :X
;            (choice [(-->> Integer (alice) (bob))]
;                    [(continue :X)])))
;  (spec (fix :X
;             (alt (--> alice bob)
;                  (fix :X)))))
;
;;;
;;; Replication examples (basic)
;;;
;;; Each of these also works with "alt" or "par" instead of "seq" as second argument of rep
;;;
;
;(defn pprint-if-flag
;  ([x]
;   (pprint-if-flag false x))
;  ([flag x]
;   (if flag (pprint x) nil)))
;
;(pprint-if-flag
;  ;true
;  (spec (rep seq [:i [0 1 2 3 4]]
;             (--> (alice :i) (bob) Integer))))
;
;(pprint-if-flag
;  ;true
;  (spec (rep seq [:i (range 5)]
;             (--> (alice :i) (bob) Integer))))
;
;(pprint-if-flag
;  ;true
;  (spec {:k 5}
;        (rep seq [:i (range :k)]
;             (--> (alice :i) (bob) Integer))))
;
;(pprint-if-flag
;  ;true
;  (spec {:k1 2
;         :k2 3}
;        (rep seq [:i (range (+ :k1 :k2))]
;             (--> (alice :i) (bob) Integer))))
;
;(pprint-if-flag
;  ;true
;  (spec {:k1 5
;         :k2 11}
;        (rep seq [:i (range :k1 :k2)]
;             (--> (alice :i) (bob) Integer))))
;
;(pprint-if-flag
;  ;true
;  (spec {:k1 5
;         :k2 7
;         :k3 11
;         :k4 13}
;        (rep seq [:i [:k1 :k2 :k3 :k4]]
;             (--> (alice :i) (bob) Integer))))
;
;(pprint-if-flag
;  ;true
;  (spec (rep seq [i [1 2]]
;             (rep seq [i [5 6]]
;                  (--> (alice i) (bob i) Integer)))))
;
;(pprint-if-flag
;  ;true
;  (spec (seq (--> (alice) (bob))
;             (rep alt [i [1 2 3]]
;                  (--> (alice i) (bob) Integer)))))
;
;;;
;;; Replication examples (bit more complex/meaningful)
;;;
;
;;; Recursive ring
;(pprint-if-flag
;  ;true
;  (spec {:k 3}
;        (fix :X (seq (rep seq [:i (range :k)]
;                          (--> (alice :i) (alice (inc :i)) Integer))
;                     (--> (alice :k) (alice 0) Integer)
;                     (fix :X)))))
;
;;; Recursive 2-dimensional load balancer
;(pprint-if-flag
;  ;true
;  (spec {:k 2
;         :l 2}
;        (fix :X (rep alt [:i (range :k)]
;                     (rep alt [:j (range :l)]
;                          (seq (--> (alice) (bob :i :j) Integer)
;                               (--> (bob :i :j) (alice) Boolean)
;                               (fix :X)))))))
;
;;;
;;; Prewalk stuff -- not needed right now, but maybe later to support:
;;;
;;;   (rep seq [i [0 1 2 3]]
;;;        (rep seq [j (range i)]
;;;             (--> (alice i) (bob j))))
;
;(def f '(foo x (bar y (baz z))))
;
;(defn prewalk-unless
;  [f p? form]
;  (walk (fn [x] (if (p? x)
;                  x
;                  (prewalk-unless f p? x))) identity (f form)))
;
;(defn prewalk-unless-demo
;  [form]
;  (prewalk-unless (fn [x] (print "Walked: ") (prn x) x)
;                  (fn [x] false)
;                  form))
;
;(defn prewalk-unless-replace
;  [smap p? form]
;  (prewalk-unless (fn [x] (if (contains? smap x) (smap x) x)) p? form))
;
;(prewalk-unless-replace {'x 3} (fn [x] (and (coll? x) (= 'bar (first x)))) f)
