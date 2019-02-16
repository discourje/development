;channels.clj
(in-ns 'discourje.core.async.async)

(defprotocol transportable
  (get-provider [this])
  (get-consumer [this])
  (get-chan [this])
  (get-monitor [this]))

(defrecord channel [provider consumers chan buffer monitor]
  transportable
  (get-provider [this] provider)
  (get-consumer [this] consumers)
  (get-chan [this] chan)
  (get-monitor [this] monitor))

(defn get-channel
  "Finds a channel based on provider and consumer"
  [provider consumer channels]
  (first
    (filter (fn [c]
              (and
                (= (get-provider c) provider)
                (= (get-consumer c) consumer)))
            channels)))

(defn- generate-channel
  "Function to generate a channel between sender and receiver"
  ([sender receiver monitor buffer]
   (if (nil? buffer)
     (->channel sender receiver (clojure.core.async/chan) nil monitor)
     (->channel  sender receiver (clojure.core.async/chan buffer) buffer monitor))))

(defn unique-cartesian-product
  "Generate channels between all participants and filters out duplicates e.g.: A<->A"
  [x y]
  (filter some?
          (for [x x y y]
            (when (not (identical? x y))
              (vector x y)))))

(defn generate-channels
  "Generates communication channels between all participants, and adds the monitor"
  [participants monitor buffer]
  (map #(apply (fn [s r] (generate-channel s r monitor buffer)) %) (unique-cartesian-product participants participants)))

(defn add-monitor-to-channels
  "Add the monitor to existing channels"
  [channels monitor]
  (for [c channels] (generate-channel (get-sender c) (get-receivers c) monitor (:buffer c))))

(defn equal-senders?
  "Check if all channels have the same sender"
  [channels]
  (= 1 (count (distinct (for [c channels] (get-provider c))))))

