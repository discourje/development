(in-ns 'discourje.core.async.async)

(defprotocol transportable
  (get-provider [this])
  (get-consumer [this])
  (get-chan [this]))

(defrecord channel [sender receiver chan]
  transportable
  (get-provider [this] sender)
  (get-consumer [this] receiver)
  (get-chan [this] chan))

(defn- generate-channel
  "Function to generate a channel between sender and receiver"
  ([sender receiver]
   (generate-channel sender receiver nil))
  ([sender receiver buffer]
   (if (nil? buffer)
     (->channel (str sender) (str receiver) (clojure.core.async/chan))
     (->channel (str sender) (str receiver) (clojure.core.async/chan buffer)))))

(defn unique-cartesian-product
  "Generate channels between all participants and filters out duplicates e.g.: A<->A"
  [x y]
  (filter some?
          (for [x x y y]
            (when (not (identical? x y))
              (vector x y)))))

(defn generate-channels
  "Generates communication channels between all participants"
  [participants buffer]
  (map #(apply (fn [s r] (generate-channel s r buffer)) %) (unique-cartesian-product participants participants)))