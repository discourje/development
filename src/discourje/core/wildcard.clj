;wildcard.clj
(in-ns 'discourje.core.async)

(def ^{:private true} allow-wildcard (atom false))

(defn get-wildcard "Get the wildcard value" []
  @allow-wildcard)

(defn enable-wildcard "Enable wildcard logic" []
  (reset! allow-wildcard true))

(defn disable-wildcard "Disable wildcard logic" []
  (reset! allow-wildcard false))

