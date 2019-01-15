(ns discourje.core.validator
  (require [clojure.core.async :as async :refer :all]
           [slingshot.slingshot :refer :all])
  (:use [slingshot.slingshot :only [throw+]]))

;set logging level to messages only, and do not block communication when diverting from protocol.
(def level-logging :logging)
;(DEFAULT!!) set logging to throwing exceptions, and block communication when diverting from protocol.
(def level-exceptions :exceptions)
;set default exception logging
(def logging-level (atom level-exceptions))

(defn- generate-exception
  "Generate a custom exception, as map data structure."
  [type message]
  {:type type :message message})

(defn set-logging
  "Set level to logging"
  []
  (reset! logging-level level-logging))

(defn set-logging-exceptions
  "Set level to exception logging/throwing"
  []
  (reset! logging-level level-exceptions))

; define a channel to log data to, we use a channel to preserve order among println
(def logging-channel (chan))

(defn log-message
  "Put a message on the logging channel.
  We use a channel to preserve order among messages!"
  [message & more]
  (when (not (nil? logging-channel))
      (>!! logging-channel (format "%s %s" message (apply str (flatten more))))))

(defn log-error
  "Always log message but throw exception (error) if exceptions level is set!"
  [type message & more]
  (let [msg (format "%s %s" message (apply str (flatten more)))]
     (log-message (format "ERROR-[%s] - %s" type msg))
     (when (= @logging-level level-exceptions)
       (throw+ (generate-exception type msg)))))

;loop take on channel as long as the channel is open.
(thread
  (loop []
    (when-let [v (<!! logging-channel)]
      (println v)
      (recur)))
  (println "Logging Closed"))

(defn stop-logging
  "Stop logging and close the channel"
  []
  (close! logging-channel))