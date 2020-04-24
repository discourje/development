(ns discourje.core.async.examples.main
  (:require [discourje.core.async.examples.config :as config]))

(defn run [lib input ns]
  (binding [config/*lib* lib
            config/*input* input
            config/*output* nil
            config/*time* nil]
    (try
      (require ns :reload)
      {:lib lib
       :input config/*input*
       :output config/*output*
       :time config/*time*}
      (catch Throwable t (.printStackTrace t)))))

(defn start [lib input ns]
  (.start (Thread. (fn [] (prn (run lib input ns))))))

(start :dcj {:buffered true :k 8 :secs 3} 'discourje.core.async.examples.micro.ring)
;(start :dcj {:k 8 :secs 3} 'discourje.core.async.examples.micro.merge)
;(start :dcj {:k 8 :secs 3} 'discourje.core.async.examples.micro.route)
;(start :dcj {:k 8 :secs 3} 'discourje.core.async.examples.micro.replicate)