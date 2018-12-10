(ns discourje.multi.pubSub
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer :all]))

(def c (chan 1))

(def sub-c (pub c :route))

(def cx (chan 1))

(sub sub-c :up-stream cx)

(def cy (chan 1))

(sub sub-c :down-stream cy)

(go-loop [_ (<! cx)]
                (println "Got something coming up!"))

(go-loop [_ (<! cy)]
                (println "Got something going down!"))

(put! c {:route :up-stream :data 123})

(put! c {:route :down-stream :data 123})

(defn recv []
  (let [sub-c (pub c :route)
        ret (atom nil)]))
