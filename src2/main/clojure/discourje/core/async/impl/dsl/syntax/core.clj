;core.clj
(in-ns 'discourje.core.async.impl.dsl.syntax)

(import clojure.lang.Seqable)

(defn- map-value! [original mapping]
  (let [map-fn (fn [org mapp] (if (keyword? org)
                                (org mapp)
                                org))]
    (if (vector? original)
      (vec (for [rsvr original] (map-fn rsvr mapping)))
      (map-fn original mapping))))

(defn is-predicate-valid?
  "Is the predicate in the monitor valid compared to the message or label (when given)"
  [message active-interaction]
  ((get-action active-interaction) message))

(defn- contains-value?
  "Does the vector contain a value?"
  [element coll]
  (when (instance? Seqable coll)
    (boolean (some #(= element %) coll))))

(defn- is-valid-interaction?
  "Is the given interaction valid compared to the active-interaction of the monitor"
  [sender receivers message active-interaction]
  (and
    (= sender (:sender active-interaction))
    (is-predicate-valid? message active-interaction)
    (and (if (instance? Seqable (:receivers active-interaction))
           (or (contains-value? receivers (:receivers active-interaction)) (= receivers (:receivers active-interaction)))
           (or (= receivers (:receivers active-interaction)) (contains-value? (:receivers active-interaction) receivers))))))

(defn- interaction-to-string
  "Stringify an interaction, returns empty string if the given interaction is nil"
  [interaction]
  (if (satisfies? stringify interaction) (to-string interaction) interaction))