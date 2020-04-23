(ns discourje.core.async.examples.main
  (:require [discourje.core.async.examples.config :as config]))

(defn run [lib input ns]
  (binding [config/*lib* lib
            config/*input* input
            config/*output* nil]
    (try
      (require ns :reload)
      config/*output*
      (catch Throwable t (.printStackTrace t)))))

(run :clj {:k 2 :secs 3} 'discourje.core.async.examples.micro.ring)