(ns discourje.core.async.impl.dsl.syntax
  (:require [discourje.core.async.logging :refer :all]))

(defprotocol closable
  (get-from [this])
  (get-to [this]))

(defprotocol linkable
  (get-id [this])
  (get-next [this])
  (apply-rec-mapping [this mapping]))

(defprotocol interactable
  (get-action [this])
  (get-sender [this])
  (get-receivers [this])
  (get-accepted-sends [this]))

(defprotocol parallelizable
  (get-parallel [this]))

(defprotocol stringify
  (to-string [this]))

(defprotocol swappable
  (get-pre-swap [this])
  (get-valid [this])
  (is-valid-for-swap? [this]))

(defrecord swappable-interaction [pre-swap valid]
  swappable
  (get-pre-swap [this] pre-swap)
  (get-valid [this] valid)
  (is-valid-for-swap? [this] (some? valid)))

(defprotocol branchable
  (get-branches [this]))

(defprotocol namable
  (get-name [this]))

(defprotocol recursable
  (get-recursion [this]))

(defprotocol identifiable-recur
  (get-option [this]))

(load "syntax/core"
      "syntax/atomic"
      "syntax/branch"
      "syntax/close"
      "syntax/identifiable-recur"
      "syntax/parallel")

(defrecord interaction [id action sender receivers accepted-sends next]
  interactable
  (get-action [this] action)
  (get-sender [this] sender)
  (get-receivers [this] receivers)
  (get-accepted-sends [this] accepted-sends)
  linkable
  (get-id [this] id)
  (get-next [this] next)
  (apply-rec-mapping [this mapping] (apply-rec-mapping-atomic! this mapping))
  stringify
  (to-string [this] (format "Interaction - Action: %s, Sender: %s, Receivers: %s with accepted sends %s" action sender receivers accepted-sends)))

(defrecord closer [id sender receiver next]
  closable
  (get-from [this] sender)
  (get-to [this] receiver)
  linkable
  (get-id [this] id)
  (get-next [this] next)
  (apply-rec-mapping [this mapping] (apply-rec-mapping-closer! this mapping))
  stringify
  (to-string [this] (format "Closer from Sender: %s to Receiver: %s" sender receiver)))

(defrecord branch [id branches next]
  branchable
  (get-branches [this] branches)
  linkable
  (get-id [this] id)
  (get-next [this] next)
  (apply-rec-mapping [this mapping] (apply-rec-mapping-branch! this mapping))
  stringify
  (to-string [this] (format "Branching with branches - %s" (apply str (for [b branches] (format "[ %s ]" (to-string b)))))))

(defrecord lateral [id parallels next]
  parallelizable
  (get-parallel [this] parallels)
  linkable
  (get-id [this] id)
  (get-next [this] next)
  (apply-rec-mapping [this mapping] (apply-rec-mapping-parallel! this mapping))
  stringify
  (to-string [this] (format "Parallel with parallels - %s" (apply str (for [p parallels] (format "[ %s ]" (to-string p)))))))

(defrecord recursion [id name recursion next]
  namable
  (get-name [this] name)
  recursable
  (get-recursion [this] recursion)
  linkable
  (get-id [this] id)
  (get-next [this] next)
  stringify
  (to-string [this] (format "Recursion name: %s, with recursion- %s" name (to-string recursion))))

(defrecord recur-identifier [id name option next]
  namable
  (get-name [this] name)
  identifiable-recur
  (get-option [this] option)
  linkable
  (get-id [this] id)
  (get-next [this] next)
  (apply-rec-mapping [this mapping] (apply-rec-mapping-recur-identifier! this mapping))
  stringify
  (to-string [this] (format "Recur-identifier - name: %s, option: %s" name option)))

(defn unique-cartesian-product
  "Generate channels between all participants and filters out duplicates e.g.: A<->A"
  [x y]
  (filter some?
          (for [x x y y]
            (when (not (identical? x y))
              (vector x y)))))

(defn- find-all-role-pairs
  "List all sender and receivers in the protocol"
  [protocol result]
  (let [result2 (conj result [])]
    (conj result2
          (flatten
            (for [element protocol]
              (cond
                (satisfies? discourje.core.async.impl.dsl.syntax/recursable element)
                (if (vector? (get-name element))
                  (let [mapping (second (get-name element))
                        mapping-vals (if (map? mapping)
                                       (vals mapping)
                                       (vals (apply hash-map mapping)))
                        cartesian-product (unique-cartesian-product mapping-vals mapping-vals)
                        mapped-channels (vec (for [pair cartesian-product] {:sender (first pair) :receivers (second pair)}))
                        result3 (conj result2 (flatten mapped-channels))]
                    (conj result3 (flatten (find-all-role-pairs (get-recursion element) result3)))
                    )
                  (conj result2 (flatten (find-all-role-pairs (get-recursion element) result2))))
                (satisfies? discourje.core.async.impl.dsl.syntax/branchable element)
                (let [branched-interactions (for [branch (get-branches element)] (find-all-role-pairs branch result2))]
                  (conj result2 (flatten branched-interactions)))
                (satisfies? discourje.core.async.impl.dsl.syntax/parallelizable element)
                (let [parallel-interactions (for [p (get-parallel element)] (find-all-role-pairs p result2))]
                  (conj result2 (flatten parallel-interactions)))
                (satisfies? discourje.core.async.impl.dsl.syntax/interactable element)
                (if (or (keyword? (get-sender element)) (or (and (vector? (get-receivers element)) (first (filter true? (filter keyword? (get-receivers element)))))) (keyword? (get-receivers element)))
                  result2
                  (if (vector? (get-receivers element))
                    (conj result2 (flatten (vec (for [rsvr (get-receivers element)] {:sender (get-sender element) :receivers rsvr}))))
                    (conj result2 {:sender (get-sender element) :receivers (get-receivers element)})))
                (satisfies? discourje.core.async.impl.dsl.syntax/closable element)
                result2
                (satisfies? discourje.core.async.impl.dsl.syntax/identifiable-recur element)
                result2
                :else
                (log-error :invalid-communication-type "Cannot find roles pairs for type:" element)))))))

(defn get-distinct-role-pairs
  "Get minimum amount of distinct sender and receivers pairs needed to implement the given protocol"
  [interactions]
  (vec (distinct (filter some? (flatten (find-all-role-pairs interactions []))))))

(defprotocol protocolable
  (get-interactions [this]))

(defrecord protocol [interactions]
  protocolable
  (get-interactions [this] interactions))