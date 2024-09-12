(ns discourje.examples.lint-tests
  (:refer-clojure :exclude [print])
  (:require [clojure.test :refer :all]
            [clojure.pprint :as p]
            [discourje.examples.main :as main]))

(defn- print [x]
  (prn)
  (p/pprint x))

(def lint {:witness false, :exclude #{:send-before-close :causality}})
(def lint-dcj {:lint :dcj})
(def lint-mcrl2 {:lint      :mcrl2,
                 :mcrl2-bin "/Applications/mCRL2.app/Contents/bin",
                 :mcrl2-tmp "/Users/sungshik/Desktop/tmp"})

(deftest da-awerbuch-tests

  ;; Ring

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.da.awerbuch
                    {:topology :ring, :k 3, :initiator 0}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.da.awerbuch
                    {:topology :ring, :k 3, :initiator 0}))
  (is true)

  ;; Tree

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.da.awerbuch
                    {:topology :tree, :k 3, :initiator 0}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.da.awerbuch
                    {:topology :tree, :k 3, :initiator 0}))
  (is true)

  ;; 2d-Mesh

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.da.awerbuch
                    {:topology :mesh-2d, :k 3, :initiator 0}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.da.awerbuch
                    {:topology :mesh-2d, :k 3, :initiator 0}))
  (is true)

  ;; Star

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.da.awerbuch
                    {:topology :star, :k 3, :initiator 0}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.da.awerbuch
                    {:topology :star, :k 3, :initiator 0}))
  (is true)

  ;; Full Mesh

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.da.awerbuch
                    {:topology :mesh-full, :k 3, :initiator 0}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.da.awerbuch
                    {:topology :mesh-full, :k 3, :initiator 0}))
  (is true))

(deftest da-cheung-tests

  ;; Ring

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.da.cheung
                    {:topology :ring, :k 3, :initiator 0}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.da.cheung
                    {:topology :ring, :k 3, :initiator 0}))
  (is true)

  ;; Tree

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.da.cheung
                    {:topology :tree, :k 3, :initiator 0}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.da.cheung
                    {:topology :tree, :k 3, :initiator 0}))
  (is true)

  ;; 2d-Mesh

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.da.cheung
                    {:topology :mesh-2d, :k 3, :initiator 0}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.da.cheung
                    {:topology :mesh-2d, :k 3, :initiator 0}))
  (is true)

  ;; Star

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.da.cheung
                    {:topology :star, :k 3, :initiator 0}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.da.cheung
                    {:topology :star, :k 3, :initiator 0}))
  (is true)

  ;; Full Mesh

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.da.cheung
                    {:topology :mesh-full, :k 3, :initiator 0}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.da.cheung
                    {:topology :mesh-full, :k 3, :initiator 0}))
  (is true))

(deftest games-chess-tests

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.games.chess
                    {:stockfish nil :turns-per-player nil :time-per-player nil}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.games.chess
                    {:stockfish nil :turns-per-player nil :time-per-player nil}))
  (is true))

(deftest games-go-fish-tests

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.games.go-fish
                    {:k 3}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.games.go-fish
                    {:k 3}))
  (is true))

(deftest games-rock-paper-scissors-tests

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.games.rock-paper-scissors
                    {:k 3}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.games.rock-paper-scissors
                    {:k 3}))
  (is true))

(deftest games-tic-tac-toe-tests

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.games.tic-tac-toe
                    {}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.games.tic-tac-toe
                    {}))
  (is true))

(deftest micro-ring-tests

  ;; Unbuffered

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.micro.ring
                    {:flags #{:unbuffered}, :k 3}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.micro.ring
                    {:flags #{:unbuffered}, :k 3}))
  (is true)

  ;;
  ;; Buffered
  ;;

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.micro.ring
                    {:flags #{:buffered}, :k 3}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.micro.ring
                    {:flags #{:buffered}, :k 3}))
  (is true))

(deftest micro-mesh-tests

  ;; Unbuffered

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.micro.mesh
                    {:flags #{:unbuffered}, :k 3}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.micro.mesh
                    {:flags #{:unbuffered}, :k 3}))
  (is true)

  ;; Buffered

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.micro.mesh
                    {:flags #{:buffered}, :k 3}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.micro.mesh
                    {:flags #{:buffered}, :k 3}))
  (is true))

(deftest micro-star-tests

  ;; Unbuffered, Outwards

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.micro.star
                    {:flags #{:unbuffered :outwards}, :k 3}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.micro.star
                    {:flags #{:unbuffered :outwards}, :k 3}))
  (is true)

  ;; Unbuffered, Inwards

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.micro.star
                    {:flags #{:unbuffered :inwards}, :k 3}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.micro.star
                    {:flags #{:unbuffered :inwards}, :k 3}))
  (is true)

  ;; Buffered, Outwards

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.micro.star
                    {:flags #{:buffered :outwards}, :k 3}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.micro.star
                    {:flags #{:buffered :outwards}, :k 3}))
  (is true)

  ;; Buffered, Inwards

  (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
                    'discourje.examples.micro.star
                    {:flags #{:buffered :inwards}, :k 3}))
  (is true)

  (print (main/main (merge {:lint :dcj} lint lint-dcj)
                    'discourje.examples.micro.star
                    {:flags #{:buffered :inwards}, :k 3}))
  (is true))

;; (deftest npb3-cg-tests

;;   (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
;;                     'discourje.examples.npb3.cg
;;                     {:k 3}))
;;   (is true)

;;   (print (main/main (merge {:lint :dcj} lint lint-dcj)
;;                     'discourje.examples.npb3.cg
;;                     {:k 3}))
;;   (is true))

;; (deftest npb3-ft-tests

;;   (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
;;                     'discourje.examples.npb3.cg
;;                     {:k 3}))
;;   (is true)

;;   (print (main/main (merge {:lint :dcj} lint lint-dcj)
;;                     'discourje.examples.npb3.cg
;;                     {:k 3}))
;;   (is true))

;; (deftest npb3-is-tests

;;   (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
;;                     'discourje.examples.npb3.cg
;;                     {:k 3}))
;;   (is true)

;;   (print (main/main (merge {:lint :dcj} lint lint-dcj)
;;                     'discourje.examples.npb3.cg
;;                     {:k 3}))
;;   (is true))

;; (deftest npb3-mg-tests

;;   (print (main/main (merge {:lint :mcrl2} lint lint-mcrl2)
;;                     'discourje.examples.npb3.cg
;;                     {:k 3}))
;;   (is true)

;;   (print (main/main (merge {:lint :dcj} lint lint-dcj)
;;                     'discourje.examples.npb3.cg
;;                     {:k 3}))
;;   (is true))