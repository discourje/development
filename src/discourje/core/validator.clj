(ns discourje.core.validator
  (require [clojure.core.async :as async :refer :all]))


(def logging-channel (chan))

(defn log-message [message]
  (put! logging-channel message))