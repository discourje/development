(ns discourje.core.validation.example-applications
  (:require [clojure.test :refer :all]
            [discourje.core.spec.lts :as lts]
            [discourje.core.spec :as s]
            [discourje.examples.games.chess :as c]
            [discourje.core.async]))

(alias 'a 'discourje.core.async)
(def chess-protocol (lts/lts (s/session ::c/chess []) :on-the-fly true))