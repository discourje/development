;channels.clj
(in-ns 'discourje.core.async.async)

(defprotocol transportable
  (get-provider [this])
  (get-consumer [this])
  (get-chan [this])
  (get-monitor [this])
  (get-buffer [this]))

(defrecord channel [provider consumers chan buffer monitor]
  transportable
  (get-provider [this] provider)
  (get-consumer [this] consumers)
  (get-chan [this] chan)
  (get-monitor [this] monitor)
  (get-buffer [this] buffer))

(defn get-channel
  "Finds a channel based on provider and consumer"
  [provider consumer channels]
  (first
    (filter (fn [c]
              (and
                (= (get-provider c) provider)
                (= (get-consumer c) consumer)))
            channels)))

(defn generate-channel
  "Function to generate a channel between sender and receiver"
  ([sender receiver monitor buffer]
   (if (nil? buffer)
     (->channel sender receiver (clojure.core.async/chan) nil monitor)
     (->channel sender receiver (clojure.core.async/chan buffer) buffer monitor)))
  ([sender receiver buffer]
   (if (nil? buffer)
     (->channel sender receiver (clojure.core.async/chan) nil nil)
     (->channel sender receiver (clojure.core.async/chan buffer) buffer nil))))

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

(defn generate-minimum-channels
  "Generates communication channels between minimum amount of participants required for a protocol, and adds the monitor
  Note: this requires a (participants) vector of maps {:sender x :receivers y}, where receivers can also be a vector, [y z]."
  [participants monitor buffer]
  (let [channels (atom [])]
    (doseq [participant participants]
      (if (instance? Seqable (:receivers participant))
        (doseq [receiver (:receivers participant)]
          (when (empty? (distinct (filter (fn [c] (and (= (:provider c) (:sender participant)) (= (:consumers c) receiver))) @channels )))
            (swap! channels conj (generate-channel (:sender participant) receiver monitor buffer))))
        (when (empty? (distinct (filter (fn [c] (and (= (:provider c) (:sender participant)) (= (:consumers c) (:receivers participant)))) @channels)))
          (swap! channels conj (generate-channel (:sender participant) (:receivers participant) monitor buffer)))))
    @channels))

(defn add-monitor-to-channels
  "Add the monitor to existing channels"
  [channels monitor]
  (for [c channels] (generate-channel (get-sender c) (get-receivers c) monitor (get-buffer c))))

(defn equal-senders?
  "Check if all channels have the same sender"
  [channels]
  (= 1 (count (distinct (for [c channels] (get-provider c))))))