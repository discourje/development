(ns discourje.core.spec.mcrl2
  (:require [clojure.string :refer [join]]
            [clojure.java.shell :refer [sh]]
            [clojure.pprint :refer [pprint]]
            [discourje.core.spec.lts :as lts])
  (:import (java.io File)))

(def ^:dynamic *mcrl2-bin* nil)

(defn mcrl2 [tool]
  (if *mcrl2-bin*
    (str *mcrl2-bin* File/separator (name tool))
    (throw (Exception.))))

(defn ltsgraph [lts temp-dir]
  (future
    (let [aut-file (str temp-dir File/separator "ltsgraph-" (System/currentTimeMillis) ".aut")]
      (spit aut-file (str lts))
      (sh (mcrl2 :ltsgraph) aut-file))))

(defn lts2pbes-pbes2bool [lts formulas temp-dir]
  (future
    (let [timestamp (System/currentTimeMillis)
          aut-file (str temp-dir File/separator "lts2pbes-pbes2bool-" timestamp ".aut")
          mcrl2-file (str temp-dir File/separator "lts2pbes-pbes2bool-" timestamp ".mcrl2")
          mcf-file (str temp-dir File/separator "lts2pbes-pbes2bool-" timestamp ".mcf")
          pbes-file (str temp-dir File/separator "lts2pbes-pbes2bool-" timestamp ".pbes")]

      ;; TODO: How to deal with non-numeric labels?

      (let [lts-string (str lts)
            lts-string (clojure.string/replace lts-string "‽" "handshake")
            lts-string (clojure.string/replace lts-string "!" "send")
            lts-string (clojure.string/replace lts-string "?" "receive")
            lts-string (clojure.string/replace lts-string "C" "close")]
        (spit aut-file lts-string))

      (spit mcrl2-file (join "\n" ["sort Role = struct " (join " | " (sort (lts/roles lts))) ";"
                                   "act"
                                   "  handshake, send, receive: Nat # Role # Role;"
                                   "  close: Role # Role;"]))

      (loop [formulas formulas
             bools {}]
        (if (empty? formulas)
          (pprint bools)
          (let [[name formula] (first formulas)
                _ (spit mcf-file formula)
                _ (sh (mcrl2 :lts2pbes) "-D" mcrl2-file "-f" mcf-file aut-file pbes-file)
                pbes2bool (sh (mcrl2 :pbes2bool) pbes-file)
                bool (read-string (:out pbes2bool))]
            (recur (rest formulas)
                   (assoc bools name bool)))))

      (clojure.java.io/delete-file mcf-file)
      (clojure.java.io/delete-file mcrl2-file)
      (clojure.java.io/delete-file pbes-file))))

;;;;;
;;;;; Example
;;;;;
;
;(defn handshakes [actions & {:keys [message sender receiver]
;                             :or   {message nil, sender nil, receiver nil}}]
;  (vec (filter #(and (= (:type %) :sync)
;                     (if message (= (:expr (:predicate %)) message) true)
;                     (if sender (= (:sender %) sender) true)
;                     (if receiver (= (:receiver %) receiver) true))
;               actions)))
;
;(defn messages [actions]
;  (map #(:expr (:predicate %)) actions))
;
;;;;;
;
;(def ^:dynamic *roles* nil)
;(def ^:dynamic *network* nil)
;
;(defn id [r]
;  (:id (get *roles* r)))
;
;(defn initiator? [r]
;  (:initiator (get *roles* r)))
;
;(defn initiators []
;  (keys (filter (fn [[k _]] (initiator? k)) *roles*)))
;
;(defn neighbours [r]
;  (get *network* r))
;
;;;;;
;
;(s/defrole ::alice "alice")
;(s/defrole ::bob "bob")
;(s/defrole ::carol "carol")
;
;(s/defrole ::dave "dave")
;
;(s/defsession ::election-init [p]
;              (s/par-every [q (neighbours p)]
;                           (s/session ::election [(id p) p q])))
;
;(s/defsession ::election [wave-id p q]
;              (s/--> (s/predicate wave-id) p q)
;
;              (s/let [handshakes-q (handshakes &hist :receiver q)
;                      max-id (max (reduce max (messages handshakes-q)) (if (initiator? q) (id q) -1))
;                      handshakes-q-max-id (handshakes &hist :receiver q :message max-id)
;                      n (count handshakes-q-max-id)
;                      parent (if (not= max-id (id q)) (:sender (first handshakes-q-max-id)))]
;
;                     (s/if (= wave-id max-id)
;                       (s/if parent
;                         (s/cat (s/if (= n 1)
;                                  (s/par-every [r (disj (neighbours q) p)]
;                                               (s/session ::election [wave-id q r])))
;
;                                (s/if (= n (count (neighbours q)))
;                                  (s/session ::election [wave-id q parent])))
;
;                         (s/if (= n (count (neighbours q)))
;                           (s/--> (s/predicate wave-id) q ::dave))))))
;
;(try
;  (let [alice (s/role ::alice)
;        bob (s/role ::bob)
;        carol (s/role ::carol)]
;
;    (binding [*roles* {alice {:id 0, :initiator true}
;                       bob   {:id 1, :initiator false}
;                       carol {:id 2, :initiator true}}
;
;              *network* {alice #{bob}
;                         bob   #{alice, carol}
;                         carol #{bob}}]
;
;      (let [s (s/par-every [p (initiators)]
;                           (s/session ::election-init [p]))
;            lts (lts/lts s :history true)]
;
;        (binding [*mcrl2-bin* "/Applications/mCRL2.app/Contents/bin"]
;          (ltsgraph lts "/Users/sungshik/Desktop/temp")
;          (lts2pbes-pbes2bool lts
;                              {:deadlock-free                "[true*] <true> true"
;                               :deadlock-free-or-termination "[true*] (<true> true || [true] false)"
;                               :election-greatest            (str "forall i1,i2:Nat, r1,r2,r3:Role. (val(i1 < 3) && val(i2 < 3)) => ("
;                                                                  "[true* . handshake(i1,r1,r2) . true* . handshake(i2,r3,dave)] val(i1 <= i2))")
;                               :election-unique              (str "forall i1:Nat, r1:Role. val(i1 < 3) => ("
;                                                                  "[true* . handshake(i1,r1,dave)] forall i2:Nat, r2:Role. val(i2 < 3) => ("
;                                                                  "[true* . handshake(i2,r2,dave)] false))")}
;                              "/Users/sungshik/Desktop/temp")))))
;
;  (catch Throwable t (.printStackTrace t)))
