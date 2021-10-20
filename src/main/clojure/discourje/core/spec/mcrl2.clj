(ns discourje.core.spec.mcrl2
  (:require [clojure.string :refer [join]]
            [clojure.java.shell :refer [sh]]
            [clojure.pprint :refer [pprint]]
            [discourje.core.spec.lts :as lts])
  (:import (java.io File)))

(def ^:dynamic *mcrl2-bin* nil)
(def ^:dynamic *mcrl2-tmp* nil)

(defn mcrl2 [tool]
  (if *mcrl2-bin*
    (str *mcrl2-bin* File/separator (name tool))
    (throw (Exception.))))

(defn ltsgraph [lts]
  (future
    (let [aut-file (str *mcrl2-tmp* File/separator "ltsgraph-" (System/currentTimeMillis) ".aut")]
      (spit aut-file (str lts))
      (sh (mcrl2 :ltsgraph) aut-file))))

(defn lts2pbes-pbes2bool [lts formulas]
  (future
    (let [timestamp (System/currentTimeMillis)
          aut-file (str *mcrl2-tmp* File/separator "lts2pbes-pbes2bool-" timestamp ".aut")
          mcrl2-file (str *mcrl2-tmp* File/separator "lts2pbes-pbes2bool-" timestamp ".mcrl2")
          mcf-file (str *mcrl2-tmp* File/separator "lts2pbes-pbes2bool-" timestamp ".mcf")
          pbes-file (str *mcrl2-tmp* File/separator "lts2pbes-pbes2bool-" timestamp ".pbes")]

      (let [lts-string (str lts)
            lts-string (clojure.string/replace lts-string #"(‽|!)\([a-zA-Z]*," "$1(")
            lts-string (clojure.string/replace lts-string "[" "(")
            lts-string (clojure.string/replace lts-string "]" ")")
            lts-string (clojure.string/replace lts-string "‽" "handshake")
            lts-string (clojure.string/replace lts-string "!" "send")
            lts-string (clojure.string/replace lts-string "?" "receive")
            lts-string (clojure.string/replace lts-string "C" "close")]
        (spit aut-file lts-string))

      (let [mcrl2-string (join "\n" [(str "sort Role = struct "
                                          (join " | " (distinct (map #(if-let [[_ name _] (re-matches #"(.+)\[([0-9]+)\]" %)]
                                                                        (str name "(Nat)")
                                                                        %)
                                                                     (sort (lts/roles lts)))))
                                          ";")
                                     "act"
                                     "  handshake, send, receive: Role # Role;"
                                     "  close: Role # Role;"])]
        (spit mcrl2-file mcrl2-string))

      (loop [formulas formulas
             bools (sorted-map)]
        (if (empty? formulas)
          bools
          (let [begin (System/nanoTime)
                [name formula] (first formulas)
                _ (spit mcf-file formula)
                lts2pbes (sh (mcrl2 :lts2pbes) "-D" mcrl2-file "-f" mcf-file aut-file pbes-file)
                ;_ (println (:err lts2pbes))
                pbes2bool (sh (mcrl2 :pbes2bool) pbes-file)
                ;_ (println (:err pbes2bool))
                bool (read-string (:out pbes2bool))
                end (System/nanoTime)
                time (long (/ (- end begin) 1000000))]
            (recur (rest formulas)
                   (assoc bools name {:verdict bool
                                      :time    time})))))

      ;(clojure.java.io/delete-file mcf-file)
      ;(clojure.java.io/delete-file mcrl2-file)
      ;(clojure.java.io/delete-file pbes-file)
      )))