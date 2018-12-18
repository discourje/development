(ns research.multiparty.debug)

(def ^{:dynamic true} *debug-enabled* false)

(defn log [level s]
  (println (apply str (cons (str "[" level "] ") s))))

(defn debug [ & s ]
  (when *debug-enabled*
    (log "Logging: " s)))