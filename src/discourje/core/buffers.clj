;buffers.clj
(in-ns 'discourje.core.async)

(defn buffer-full? [channel] "Check if buffer on channel is full"
  (bufs/full? (.buf channel)))

(defn something-in-buffer? [channel] "Check whether there is something in the buffer of a channel"
  (< 0 (count (.buf channel))))

(defn peek-channel [channel] "Peek the value on a channel"
  (if (and (.buf channel)
           (pos? (count (.buf channel))))
    (if (instance? clojure.core.async.impl.buffers.PromiseBuffer
                   (.buf channel))
      (async/<!! channel)
      (last (.buf (.buf channel))))
    (some-> channel .puts first second)))