(ns discourje.core.lint.benchmarks
  (:gen-class)
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [discourje.core.spec :as s]
            [discourje.core.lint :as c]
            [discourje.core.spec.lts :as lts]
            [discourje.core.spec.mcrl2 :as mcrl2]))

(defn -main [& args]
  (let [input (read-string (clojure.string/join " " args))
        algorithm (:algorithm input)
        initiator (:initiator input)
        network ((eval (read-string (str "discourje.core.lint.benchmarks/" (name (:network input))))) (:n input))
        spec (s/session algorithm [initiator network])

        begin-lts (System/nanoTime)
        lts (lts/lts spec)
        end-lts (System/nanoTime)
        time-lts (long (/ (- end-lts begin-lts) 1000000))

        begin-lint (System/nanoTime)
        results (binding [mcrl2/*mcrl2-bin* (str (:mcrl2-bin input))
                          mcrl2/*mcrl2-tmp* (str (:mcrl2-tmp input))]
                  (c/lint lts
                          :engine (:engine input)
                          :witness false
                          :exclude #{:send-before-close :causality}))
        end-lint (System/nanoTime)
        time-lint (long (/ (- end-lint begin-lint) 1000000))

        output (clojure.string/join " " [(:engine input)
                                         (:algorithm input)
                                         (:initiator input)
                                         (:network input)
                                         (:n input)
                                         (let [s (str lts)
                                               [_ transitions states] (.split (.substring
                                                                                s
                                                                                (inc (.indexOf s "("))
                                                                                (.indexOf s ")")) ",")]
                                           (str "(" states "," transitions ")"))
                                         time-lts
                                         time-lint])]
    (.println System/out results)
    (.println System/err output)
    (if (nil? (:no-exit input))
      (System/exit 0))))

(deftest -main-test
  (let [input {:engine    :dcj,
               :mcrl2-bin "/Applications/mCRL2.app/Contents/bin",
               :mcrl2-tmp "/Users/sungshik/Desktop/tmp",
               :algorithm :awerbuch,
               :initiator 0,
               :network   :ring,
               :n         4
               :no-exit   true}
        args (clojure.string/split (str input) #" ")]
    (apply -main args)))