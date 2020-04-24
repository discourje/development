(ns discourje.core.async.examples
  (:require [discourje.core.async.examples.config :as config]))

(defn run [ns lib input]
  (binding [config/*lib* lib
            config/*input* input
            config/*output* nil
            config/*time* nil]
    (try
      (require ns :reload)
      {:ns ns
       :lib lib
       :input config/*input*
       :output config/*output*
       :time config/*time*}
      (catch Throwable t (.printStackTrace t)))))

(defn compare [ns libs input]
  (mapv #(run ns % input) libs))

(defn start [ns lib input]
  (.start (Thread. ^Runnable (fn [] (prn (run ns lib input))))))
