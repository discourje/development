(ns discourje.async.PeekTests
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer :all :as a]))

(defn peeker [ch]
  (if (and (.buf ch)
           (pos? (count (.buf ch))))
    (if (instance? clojure.core.async.impl.buffers.PromiseBuffer
                   (.buf ch))
      (a/<!! ch)
      (last (.buf (.buf ch))))
    (some-> ch .puts first second)))

(defn teste [ch]
  (if (instance? clojure.core.async.impl.buffers.PromiseBuffer
                 (.buf ch))
    (a/<!! ch)
    (last (.buf (.buf ch)))))

(defn buffer-full? [ch]
  (and
    (.buf ch)
    (pos? (count (.buf ch)))))

(defn something-in-buffer? [c]
  (< 0 (count (.buf c))))

(defn await-put [c v]
  (if (buffer-full? c)
    ))


(defn await-take [c label]
  (if (something-in-buffer? c)
    (let [value (peeker c)]
      (= (:label value) label))
    (do (while (false? (something-in-buffer? c)))
        (await-take c label))))

(def c (chan 1))
(buffer-full? c)
(something-in-buffer? c)
(teste c)
(thread (>!! c {:label "hi" :content "this is content"}))
(thread (println "yes yes" (await-take c "hi")))
(thread (println (peeker c)))
(thread (println (<!! c)))
(close! c)