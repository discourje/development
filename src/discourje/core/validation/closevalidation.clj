;closevalidation.clj
(in-ns 'discourje.core.async)

(defn is-valid-close?
  "Is the active interaction a valid close?"
  [sender receivers active-interaction]
  (and
    (= sender (get-from active-interaction))
    (= receivers (get-to active-interaction))))

(defn- swap-active-interaction-by-close
  "Swap active interaction by close"
  [active-interaction target-interaction]
  (let [pre-swap-interaction @active-interaction]
    (let [swapped (swap! active-interaction (fn [inter]
                                              (if (= (get-id inter) (get-id pre-swap-interaction))
                                                (if (not= nil (get-next target-interaction))
                                                  (get-next target-interaction)
                                                  nil)
                                                inter)))]
      (= (if (nil? swapped) "end-protocol" (get-id swapped))
         (if (or (nil? target-interaction) (nil? (get-next target-interaction)))
           "end-protocol"
           (get-id (get-next target-interaction)))))))

(defn- apply-close-to-mon
  "Apply new interaction"
  ([monitor channel active-interaction target-interaction]
   (log-message (format "Applying: Close sender %s, receiver %s." (get-provider channel) (get-consumer channel)))
   (clojure.core.async/close! (get-chan channel))
   (swap-active-interaction-by-close active-interaction target-interaction)))