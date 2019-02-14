(in-ns 'discourje.core.async.async)

(defprotocol transportable
  (get-provider [this])
  (get-consumer [this])
  (get-chan [this])
  (get-monitor [this]))

(defrecord channel [sender receiver chan monitor]
  transportable
  (get-provider [this] sender)
  (get-consumer [this] receiver)
  (get-chan [this] chan)
  (get-monitor [this] monitor))

(defn- generate-channel
  "Function to generate a channel between sender and receiver"
  ([sender receiver]
   (println "generating channelL" sender receiver)
   (generate-channel sender receiver monitor nil))
  ([sender receiver monitor buffer]
   (println "generating channel" sender receiver)
   (if (nil? buffer)
     (->channel (str sender) (str receiver) (clojure.core.async/chan) monitor)
     (->channel (str sender) (str receiver) (clojure.core.async/chan buffer) monitor))))

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