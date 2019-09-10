;closevalidation.clj
(in-ns 'discourje.core.async)

(defn is-valid-close?
  "Is the active interaction a valid close?"
  [sender receivers active-interaction]
  (and
    (= sender (get-from active-interaction))
    (= receivers (get-to active-interaction))))


(defn- apply-close-to-mon
  "Apply new interaction"
  ([monitor channel active-interaction target-interaction]
   (log-message (format "Applying: Close sender %s, receiver %s." (get-provider channel) (get-consumer channel)))
   (clojure.core.async/close! (get-chan channel))
   (swap-active-interaction-by-atomic active-interaction target-interaction nil)))