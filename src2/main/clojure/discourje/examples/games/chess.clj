(ns discourje.examples.games.chess
  (:require [clojure.core.async]
            [discourje.core.async]
            [discourje.core.spec :as s]
            [discourje.examples.config :as config])
  (:import (discourje.examples.games.impl.chess Engine)))

(config/clj-or-dcj)

;;;;
;;;; Specification
;;;;

(s/defrole ::white)
(s/defrole ::black)

(s/defsession ::chess []
  (::chess-turn ::white ::black))

(s/defsession ::chess-turn [r1 r2]
  (s/--> String r1 r2)
  (s/alt (::chess-turn r2 r1)
         (s/par (s/close r1 r2)
                (s/close r2 r1))))

;;;;
;;;; Implementation
;;;;

(defn set-stockfish-path [os]
  (set! Engine/STOCKFISH
        (str (System/getProperty "user.dir") "/"
             "src2/main/java/"
             "discourje/examples/games/impl/chess/"
             (cond (= os "linux")
                   "stockfish-linux"
                   (= os "mac")
                   "stockfish-mac"
                   (= os "win32")
                   "stockfish-win32.exe"
                   (= os "win64'")
                   "stockfish-win64.exe"))))

(let [input config/*input*
      _ (:resolution input)
      os (:os input)
      turns-per-player (:turns-per-player input)
      time-per-player (:time-per-player input)]

  (set-stockfish-path (str os))
  (if turns-per-player (set! Engine/TURNS_PER_PLAYER turns-per-player))
  (if time-per-player (set! Engine/TIME_PER_PLAYER time-per-player))

  (let [;; Start timer
        begin (System/nanoTime)

        ;; Create channels
        w->b (a/chan)
        b->w (a/chan)

        ;; Link monitor [optional]
        _
        (if (= config/*lib* :dcj)
          (let [s (chess)
                m (a/monitor s)]
            (a/link w->b white black m)
            (a/link b->w black white m)))

        ;; Spawn threads
        white
        (a/thread (let [e (Engine. true)]
                    (a/>!! w->b (.turn e nil))
                    (loop []
                      (let [m (a/<!! b->w)]
                        (if (not= m "(none)")
                          (let [m (.turn e m)]
                            (a/>!! w->b m)
                            (if (not= m "(none)")
                              (recur))))))
                    (a/close! w->b)
                    (.kill e)))

        black
        (a/thread (let [e (Engine. true)]
                    (loop []
                      (let [m (a/<!! w->b)]
                        (if (not= m "(none)")
                          (let [m (.turn e m)]
                            (a/>!! b->w m)
                            (if (not= m "(none)")
                              (recur))))))
                    (a/close! b->w)
                    (.kill e)))

        ;; Await termination
        output
        (do (a/<!! white)
            (a/<!! black)
            nil)

        ;; Stop timer
        end (System/nanoTime)]

    (set! config/*output* output)
    (set! config/*time* (- end begin))))