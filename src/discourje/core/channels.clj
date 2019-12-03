;channels.clj
(in-ns 'discourje.core.async)

(defrecord channel [provider consumers chan buffer monitor meta-put meta-take]
  transportable
  (get-provider [this] provider)
  (get-consumer [this] consumers)
  (get-chan [this] chan)
  (get-monitor [this] monitor)
  (get-buffer [this] buffer))

(defn new-channel [provider consumers chan buffer monitor]
  (->channel provider consumers chan buffer monitor
             (new java.util.concurrent.Semaphore buffer)
             (new java.util.concurrent.Semaphore 0)))
(defn acquire-put [channel]
  (.acquire (:meta-put channel)))

(defn release-put [channel]
  (.release (:meta-put channel)))

(defn acquire-take [channel]
  (.acquire (:meta-take channel)))

(defn release-take [channel]
  (.release (:meta-take channel)))

(defn- get-infra-channel
  "Finds a channel based on provider and consumer"
  [provider consumer channels]
  (first
    (filter #(and (= (get-provider %) provider)
                  (= (get-consumer %) consumer))
            channels)))

(defprotocol infrastructurable
  (get-channel [this provider consumer])
  (get-channels [this]))

(defrecord infrastructure [channels]
  infrastructurable
  (get-channel [this provider consumer] (get-infra-channel provider consumer channels))
  (get-channels [this] channels))

(defn generate-channel
  "Function to generate a channel between sender and receiver"
  ([sender receiver monitor buffer]
   (if (nil? buffer)
     (new-channel sender receiver (clojure.core.async/chan) nil monitor)
     (new-channel sender receiver (clojure.core.async/chan buffer) buffer monitor)))
  ([sender receiver buffer]
   (if (nil? buffer)
     (new-channel sender receiver (clojure.core.async/chan) nil nil)
     (new-channel sender receiver (clojure.core.async/chan buffer) buffer nil))))

(defn generate-minimum-channels
  "Generates communication channels between minimum amount of participants required for a protocol, and adds the monitor
  Note: this requires a (participants) vector of maps {:sender x :receivers y}, where receivers can also be a vector, [y z]."
  [participants monitor buffer]
  (let [channels (atom [])]
    (doseq [participant participants]
      (if (instance? Seqable (:receivers participant))
        (doseq [receiver (:receivers participant)]
          (when (empty? (distinct (filter (fn [c] (and (= (:provider c) (:sender participant)) (= (:consumers c) receiver))) @channels)))
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