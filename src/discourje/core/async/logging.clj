(ns discourje.core.async.logging
  (require [clojure.core.async :as async]
           [slingshot.slingshot :refer :all])
  (:use [slingshot.slingshot :only [throw+]]))

;set the logging level to none, showing no logs nor exceptions
(def level-none :none)
;set logging level to messages only, and do not block communication when diverting from protocol.
(def level-logging :logging)
;(DEFAULT!!) set logging to throwing exceptions, and block communication when diverting from protocol.
(def level-exceptions :exceptions)
;Enable logging and throwing exceptions, and block communication when diverting from protocol.
(def level-logging-exceptions :logging-exceptions)
;set default exception logging
(def logging-level (atom level-logging-exceptions))

(defn- generate-exception
  "Generate a custom exception, as map data structure."
  [type message]
  {:type type :message message})

(defn set-logging-none
  "Set level to none"
  []
  (reset! logging-level level-none))

(defn set-logging
  "Set level to logging"
  []
  (reset! logging-level level-logging))

(defn set-logging-exceptions
  "Set level to exception throwing"
  []
  (reset! logging-level level-exceptions))

(defn set-logging-and-exceptions
  "Set level to exception logging/throwing"
  []
  (reset! logging-level level-logging-exceptions))

; define a channel to log data to, we use a channel to preserve order among println
(def logging-channel (async/chan))

(defn can-log?
  "Is logging enabled?"
  []
  (or (= @logging-level level-logging) (= @logging-level level-logging-exceptions)))

(defn can-throw?
  "Is throwing enabled?"
  []
  (or (= @logging-level level-exceptions) (= @logging-level level-logging-exceptions)))

(defn log-message
  "Put a message on the logging channel.
  We use a channel to preserve order among messages!"
  [message & more]
  (when (and (not (nil? logging-channel)) (can-log?))
    (async/>!! logging-channel (format "%s %s" message (apply str (flatten more))))))


(defn log-error
  "Always log message but throw exception (error) if exceptions level is set!"
  [type message & more]
  (let [msg (format "%s %s" message (apply str (flatten more)))]
    (if (can-throw?)
      (throw+ (generate-exception type msg))
      (log-message (format "ERROR-[%s] - %s" type msg)))))

;loop take on channel as long as the channel is open.
(async/thread
  (loop []
    (when-let [v (async/<!! logging-channel)]
      (println v)
      (recur)))
  (println "Logging Closed"))

(defn stop-logging
  "Stop logging and close the channel"
  []
  (async/close! logging-channel))