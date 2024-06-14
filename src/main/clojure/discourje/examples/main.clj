(ns discourje.examples.main
  (:gen-class)
  (:require [discourje.core.lint :as l]
            [discourje.core.spec.mcrl2 :as mcrl2]
            [discourje.core.spec.ast :as ast]
            [discourje.examples.config :as config]))

(defn main [settings program input]
  (binding [config/*lint* (:lint settings)
            config/*run* (:run settings)
            config/*input* input
            config/*output* nil
            l/*engine* (:lint settings)
            l/*witness* (if (some? (:witness settings)) (:witness settings) l/*witness*)
            l/*exclude* (if (some? (:exclude settings)) (:exclude settings) l/*exclude*)
            mcrl2/*mcrl2-bin* (:mcrl2-bin settings)
            mcrl2/*mcrl2-tmp* (:mcrl2-tmp settings)]

    (when (some? (:timeout settings))
      (.start (Thread. ^Runnable (fn []
                                   (Thread/sleep (* 1000 (:timeout settings)))
                                   (prn "timeout")
                                   (System/exit 0)))))
    (let [begin (System/nanoTime)
          _ (require program :reload)
          end (System/nanoTime)]
      {:settings settings
       :program  program
       :input    config/*input*
       :output   config/*output*
       :time     (int (/ (- end begin) (* 1000 1000)))})))

(defmacro commit []
  (clojure.string/trim-newline (:out (clojure.java.shell/sh "git" "rev-parse" "--short" "HEAD"))))

(defn -main [& args]
  (try
    (let [[settings program-short input] (read-string (str "[" (clojure.string/join " " args) "]"))
          program (symbol (str "discourje.examples" "." program-short))
          output (main settings program input)]
      (prn output)
      (System/exit 0))

    (catch Throwable t
      (println)
      (println (str "Discourje Examples (" (commit) ")"))
      (println (str "Usage: java -jar discourje-examples.jar <settings> <program> <input>"))
      (println (str "  <program> \u2208 {"
                    (clojure.string/join ", " ["micro.mesh" "micro.ring" "micro.star"
                                               "games.chess" "games.go-fish" "games.rock-paper-scissors" "games.tic-tac-toe"
                                               "npb3.cg" "npb3.ft" "npb3.is" "npb3.mg"])
                    "}"))
      (println)
      (.printStackTrace t)
      (println))))

(comment
  (.start
   (Thread.
    #(do
       (println "begin")
       (main {:run :dcj}
             'discourje.examples.micro.star
             {:flags #{:buffered :outwards}, :k 3, :n 1000})
       (println "end")))))
