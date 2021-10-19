(ns discourje.core.ctl.example-applications
  (:require [discourje.core.spec.lts :as lts]
            [discourje.core.spec :as s]
            [discourje.core.async]))

;;
;; chess
;;
(s/defrole ::white)
(s/defrole ::black)

(s/defsession ::chess []
              (::chess-turn ::white ::black))

(s/defsession ::chess-turn [r1 r2]
              (s/--> String r1 r2)
              (s/alt (::chess-turn r2 r1)
                     (s/par (s/close r1 r2)
                            (s/close r2 r1))))

(def chess-protocol
  (lts/lts (s/session ::chess [])))

;
; go-fish
;
(defrecord Card [suit rank]
  Object
  (toString [_] (str suit rank)))

(defrecord Turn [])
(defrecord Ask [suit rank])
(defrecord Go [])
(defrecord Fish [])

(defrecord OutOfCards [])

(s/defrole ::dealer)
(s/defrole ::player)

(s/defsession ::go-fish [player-ids]
              (s/cat (s/par-every [i player-ids]
                                  (s/cat-every [_ (range 5)]
                                               (s/--> Card ::dealer (::player i))))
                     (s/alt-every [i player-ids]
                                  (s/cat (s/--> Turn ::dealer (::player i))
                                         (::go-fish-turn i player-ids)))
                     (s/par-every [i player-ids]
                                  (s/cat (s/close ::dealer (::player i))
                                         (s/par (s/cat (s/* (s/--> Card (::player i) ::dealer))
                                                       (s/close (::player i) ::dealer))
                                                (s/par-every [j (disj player-ids i)]
                                                             (s/close (::player i) (::player j))))))))

(s/defsession ::go-fish-turn [i player-ids]
              (s/alt-every [j (disj player-ids i)]
                           (s/cat (s/--> Ask (::player i) (::player j))
                                  (s/alt (s/cat (s/--> Card (::player j) (::player i))
                                                (s/--> OutOfCards (::player i) ::dealer))
                                         (s/cat (s/--> Card (::player j) (::player i))
                                                (::go-fish-turn i player-ids))
                                         (s/cat (s/--> Go (::player j) (::player i))
                                                (s/--> Fish (::player i) ::dealer)
                                                (s/alt (s/--> Card ::dealer (::player i))
                                                       (s/--> OutOfCards ::dealer (::player i)))
                                                (s/--> Turn (::player i) (::player j))
                                                (::go-fish-turn j player-ids))))))

(def go-fish-protocol
  (lts/lts (s/session ::go-fish [(set (range 1))])))
;
; rock-paper-scissors
;
(s/defrole ::player)

(s/defsession ::rock-paper-scissors [ids]
              (::rock-paper-scissors-round ids s/empty-set))

(s/defsession ::rock-paper-scissors-round [ids co-ids]
              (s/if (> (s/count ids) 1)
                    (s/cat (s/par-every [i ids
                                         j (s/disj ids i)]
                                        (s/--> String (::player i) (::player j)))
                           (s/alt-every [winner-ids (s/power-set ids)]
                                        (s/let [loser-ids (s/difference ids winner-ids)]
                                          (s/par (::rock-paper-scissors-round winner-ids (s/union co-ids loser-ids))
                                                 (s/par-every [i loser-ids
                                                               j (s/disj (s/union ids co-ids) i)]
                                                              (s/close (::player i) (::player j)))))))))

(def rock-paper-scissors-protocol
  (lts/lts (s/session ::rock-paper-scissors [(set (range 1))])))
;
; tic-tac-toe
;
(s/defrole ::alice)
(s/defrole ::bob)

(s/defsession ::tic-tac-toe []
              (s/alt (::tic-tac-toe-turn ::alice ::bob)
                     (::tic-tac-toe-turn ::bob ::alice)))

(s/defsession ::tic-tac-toe-turn [r1 r2]
              (s/--> Long r1 r2)
              (s/alt (::tic-tac-toe-turn r2 r1)
                     (s/par (s/close r1 r2)
                            (s/close r2 r1))))

(def tic-tac-toe-protocol
  (lts/lts (s/session ::tic-tac-toe [])))

;;
;; NBP3
;;
;
;;
;; cg
;;
;(s/defrole ::master)
;(s/defrole ::worker)
;
;(s/defsession ::cg [k]
;              (s/cat (s/* (s/par-every [i (range k)]
;                                       (s/cat (s/-->> discourje.examples.npb3.impl.CGThreads.CGMessage ::master (::worker i))
;                                              (s/-->> discourje.examples.npb3.impl.DoneMessage (::worker i) ::master))))
;                     (s/par-every [i (range k)]
;                                  (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::worker i))
;                                         (s/-->> discourje.examples.npb3.impl.DoneMessage (::worker i) ::master)))
;                     (s/par (s/par-every [i (range k)]
;                                         (s/close ::master (::worker i)))
;                            (s/par-every [i (range k)]
;                                         (s/close (::worker i) ::master)))))
;
;(defn get-protocol [size]
;  (lts/lts (s/session ::cg [size])))
;;
;; ft
;;
;(s/defrole ::master)
;(s/defrole ::evolve)
;(s/defrole ::fft)
;
;(s/defsession ::ft [k]
;              (s/cat (s/* (s/alt (s/par-every [i (range k)]
;                                              (s/cat (s/-->> discourje.examples.npb3.impl.FTThreads.EvolveMessage ::master (::evolve i))
;                                                     (s/-->> discourje.examples.npb3.impl.DoneMessage (::evolve i) ::master)))
;                                 (s/par-every [i (range k)]
;                                              (s/cat (s/-->> discourje.examples.npb3.impl.FTThreads.FFTMessage ::master (::fft i))
;                                                     (s/-->> discourje.examples.npb3.impl.DoneMessage (::fft i) ::master)))
;                                 (s/par-every [i (range k)]
;                                              (s/cat (s/-->> discourje.examples.npb3.impl.FTThreads.FFTSetVariablesMessage ::master (::fft i))
;                                                     (s/-->> discourje.examples.npb3.impl.DoneMessage (::fft i) ::master)))))
;                     (s/par-every [i (range k)]
;                                  (s/par (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::evolve i))
;                                                (s/-->> discourje.examples.npb3.impl.DoneMessage (::evolve i) ::master))
;                                         (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::fft i))
;                                                (s/-->> discourje.examples.npb3.impl.DoneMessage (::fft i) ::master))))
;                     (s/par (s/par-every [i (range k)]
;                                         (s/close ::master (::evolve i)))
;                            (s/par-every [i (range k)]
;                                         (s/close (::evolve i) ::master))
;                            (s/par-every [i (range k)]
;                                         (s/close ::master (::fft i)))
;                            (s/par-every [i (range k)]
;                                         (s/close (::fft i) ::master)))))
;(defn get-protocol [size]
;  (lts/lts (s/session ::ft [size])))
;;
;; is
;;
;(s/defrole ::master)
;(s/defrole ::worker)
;
;(s/defsession ::is [k]
;              (s/cat (s/* (s/par-every [i (range k)]
;                                       (s/cat (s/-->> discourje.examples.npb3.impl.ISThreads.RankMessage ::master (::worker i))
;                                              (s/-->> discourje.examples.npb3.impl.DoneMessage (::worker i) ::master))))
;                     (s/par-every [i (range k)]
;                                  (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::worker i))
;                                         (s/-->> discourje.examples.npb3.impl.DoneMessage (::worker i) ::master)))
;                     (s/par (s/par-every [i (range k)]
;                                         (s/close ::master (::worker i)))
;                            (s/par-every [i (range k)]
;                                         (s/close (::worker i) ::master)))))
;
;(defn get-protocol [size]
;  (lts/lts (s/session ::is [size])))
;;
;; mg
;;
;(s/defrole ::master)
;(s/defrole ::interp)
;(s/defrole ::psinv)
;(s/defrole ::rprj)
;(s/defrole ::resid)
;
;(s/defsession ::mg [k]
;              (s/cat (s/* (s/alt (s/par-every [i (range k)]
;                                              (s/cat (s/-->> discourje.examples.npb3.impl.MGThreads.InterpMessage ::master (::interp i))
;                                                     (s/-->> discourje.examples.npb3.impl.DoneMessage (::interp i) ::master)))
;                                 (s/par-every [i (range k)]
;                                              (s/cat (s/-->> discourje.examples.npb3.impl.MGThreads.PsinvMessage ::master (::psinv i))
;                                                     (s/-->> discourje.examples.npb3.impl.DoneMessage (::psinv i) ::master)))
;                                 (s/par-every [i (range k)]
;                                              (s/cat (s/-->> discourje.examples.npb3.impl.MGThreads.RprjMessage ::master (::rprj i))
;                                                     (s/-->> discourje.examples.npb3.impl.DoneMessage (::rprj i) ::master)))
;                                 (s/par-every [i (range k)]
;                                              (s/cat (s/-->> discourje.examples.npb3.impl.MGThreads.ResidMessage ::master (::resid i))
;                                                     (s/-->> discourje.examples.npb3.impl.DoneMessage (::resid i) ::master)))))
;                     (s/par-every [i (range k)]
;                                  (s/par (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::interp i))
;                                                (s/-->> discourje.examples.npb3.impl.DoneMessage (::interp i) ::master))
;                                         (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::psinv i))
;                                                (s/-->> discourje.examples.npb3.impl.DoneMessage (::psinv i) ::master))
;                                         (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::rprj i))
;                                                (s/-->> discourje.examples.npb3.impl.DoneMessage (::rprj i) ::master))
;                                         (s/cat (s/-->> discourje.examples.npb3.impl.ExitMessage ::master (::resid i))
;                                                (s/-->> discourje.examples.npb3.impl.DoneMessage (::resid i) ::master))))
;                     (s/par (s/par-every [i (range k)]
;                                         (s/close ::master (::interp i)))
;                            (s/par-every [i (range k)]
;                                         (s/close (::interp i) ::master))
;                            (s/par-every [i (range k)]
;                                         (s/close ::master (::psinv i)))
;                            (s/par-every [i (range k)]
;                                         (s/close (::psinv i) ::master))
;                            (s/par-every [i (range k)]
;                                         (s/close ::master (::rprj i)))
;                            (s/par-every [i (range k)]
;                                         (s/close (::rprj i) ::master))
;                            (s/par-every [i (range k)]
;                                         (s/close ::master (::resid i)))
;                            (s/par-every [i (range k)]
;                                         (s/close (::resid i) ::master)))))
;
;(defn get-protocol [size]
;  (lts/lts (s/session ::cg [size])))
