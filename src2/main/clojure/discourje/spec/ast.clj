(ns discourje.spec.ast
  (:refer-clojure :exclude [sync send cat loop]))

;;;;
;;;; Predicates
;;;;

(defrecord Predicate [expr])

(defn predicate? [x]
  (instance? Predicate x))

(defn predicate [expr]
  {:pre []}
  (->Predicate expr))

;;;;
;;;; Roles
;;;;

(def role-names (atom {}))

(defn put-role-name! [k name]
  {:pre [(keyword? k)
         (string? name)]}
  (swap! role-names (fn [m] (into m {k name}))))

(defn get-role-name [k]
  (get @role-names k))

(defrecord Role [name-expr index-exprs])

(defn role? [x]
  (instance? Role x))

(defn role
  ([expr]
   {:pre [(or (not (coll? expr)) (seq? expr))]}
   (cond
     (not (coll? expr)) (role expr [])
     (seq? expr) (role (first expr) (vec (rest expr)))))

  ([name-expr index-exprs]
   {:pre [(or (string? name-expr) (symbol? name-expr) (keyword? name-expr))
          (vector? index-exprs)]}
   (->Role name-expr index-exprs)))

;;;;
;;;; Actions
;;;;

(defrecord Action [type predicate sender receiver])

(defn action? [x]
  (= (type x) Action))

(defn action [type predicate sender receiver]
  {:pre [(contains? #{:sync :send :receive :close} type)
         (or (predicate? predicate) (symbol? predicate))
         (or (role? sender) (symbol? sender))
         (or (role? receiver) (symbol? receiver))]}
  (->Action type predicate sender receiver))

(defn sync [predicate sender receiver]
  (action :sync predicate sender receiver))

(defn send [predicate sender receiver]
  (action :send predicate sender receiver))

(defn receive [sender receiver]
  (action :receive (predicate '(fn [_] true)) sender receiver))

(defn close [sender receiver]
  (action :close (predicate '(fn [_] true)) sender receiver))

;;;;
;;;; Nullary operators
;;;;

(defrecord Nullary [type])

(defn end []
  (->Nullary :end))

;;;;
;;;; Multiary operators
;;;;

(defrecord Multiary [type branches])

(defn cat [branches]
  (->Multiary :cat branches))
(defn alt [branches]
  (->Multiary :alt branches))
(defn par [branches]
  (->Multiary :par branches))

(defrecord EveryMultiary [type ast-f vars exprs branch])

(defn every
  ([ast-f bindings branch]
   (let [vars (take-nth 2 bindings)
         exprs (take-nth 2 (rest bindings))]
     (every ast-f vars exprs branch)))
  ([ast-f vars exprs branch]
   (->EveryMultiary :every ast-f vars exprs branch)))

;;;;
;;;; Conditional operators
;;;;

(defrecord If [type condition branch1 branch2])

(defn if-then-else [condition branch1 branch2]
  (->If :if condition branch1 branch2))

;;;;
;;;; Recursion operators
;;;;

(defrecord Loop [type name vars exprs body])

(defn loop
  ([name bindings body]
   (let [vars (take-nth 2 bindings)
         exprs (take-nth 2 (rest bindings))]
     (loop name vars exprs body)))
  ([name vars exprs body]
   (->Loop :loop name vars exprs body)))

(defrecord Recur [type name exprs])

(defn recur [name exprs]
  (->Recur :recur name exprs))

;;;;
;;;; Definition operators
;;;;

(def registry (atom {}))

(defn register! [name vars body]
  (swap! registry
         (fn [m]
           (if (contains? m name)
             (update m name #(into % {(count vars) {:vars vars, :body body}}))
             (into m {name {(count vars) {:vars vars, :body body}}})))))

;;;;
;;;; Aldebaran
;;;;

(defrecord Graph [type v edges])

(defn- parse-predicate [s]
  (predicate (read-string s)))

(defn- parse-role [s]
  (if (clojure.string/includes? s "[")
    (let [name-expr (subs s 0 (clojure.string/index-of s "["))
          index-exprs (mapv read-string
                            (clojure.string/split (subs s
                                                        (inc (clojure.string/index-of s "["))
                                                        (dec (count s)))
                                                  #"\]\["))]
      (role name-expr index-exprs))
    (role s)))

(defn- parse-action
  ([s]
   (parse-action (case (first s)
                   \‽ :sync
                   \! :send
                   \? :receive
                   \C :close
                   (throw (Exception.)))
                 (subs s 2 (dec (count s)))))
  ([type s]
   (let [s (if (contains? #{:sync :send} type)
             s
             (str "(fn [_] true)," s))
         tokens (clojure.string/split s #"\,")
         predicate (parse-predicate (nth tokens 0))
         sender (parse-role (nth tokens 1))
         receiver (parse-role (nth tokens 2))]
     (action type predicate sender receiver))))

(defn aldebaran [v0 edges]
  (->Graph :aldebaran
           v0
           (clojure.core/loop [todo edges
                               result {}]
             (if (empty? todo)
               result
               (recur (rest todo)
                      (let [transition (first todo)
                            source (nth transition 0)
                            label (nth transition 1)
                            target (nth transition 2)
                            action (parse-action label)]

                        (if (and (contains? result source)
                                 (contains? (get result source) label))
                          (update result source #(merge-with into % {label [target]}))
                          (merge-with into result {source {action [target]}}))))))))
