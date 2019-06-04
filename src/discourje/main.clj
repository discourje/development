(ns discourje.main
  (:gen-class)
  (:require [discourje.core.async :refer :all]))

(defn -main [& args]
  (println "Test!"))

(defn hello-world [hi]
  (println hi))