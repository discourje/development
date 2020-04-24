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
  (.start (Thread. (fn [] (prn (run ns lib input))))))

;(start :dcj {:buffered true :k 2 :secs 3} 'discourje.core.async.examples.micro.ring)
;(start :dcj {:buffered true :ordered-sends true :k 2 :secs 3} 'discourje.core.async.examples.micro.star)
;(start :dcj {:buffered false :k 2 :secs 3} 'discourje.core.async.examples.micro.master-worker)


;(start :dcj {:k 4 :secs 3} 'discourje.core.async.examples.micro.merge)
;(start :dcj {:k 2 :secs 3} 'discourje.core.async.examples.micro.client-server)
