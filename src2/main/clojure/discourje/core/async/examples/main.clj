(ns discourje.core.async.examples.main
  (:require [discourje.core.async.examples.config :as config]))

(defn run [lib input ns]
  (binding [config/*lib* lib
            config/*input* input
            config/*output* nil
            config/*time* nil]
    (try
      (require ns :reload)
      {:input config/*input*
       :output config/*output*
       :time config/*time*}
      (catch Throwable t (.printStackTrace t)))))

(run :dcj {:buffered true :k 2 :secs 3} 'discourje.core.async.examples.micro.ring)