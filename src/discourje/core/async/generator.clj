(in-ns 'discourje.core.async.async)

(defn getDistinctRoles
  "Get all distinct senders and receivers in the protocol"
  [interactions]
  (let [x (findAllRoles interactions [])]
    (vec (filter some? (distinct (flatten (first x)))))))