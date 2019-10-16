(ns discourje.java.settings
  (require [discourje.core.async :refer :all]))

(discourje.core.async/enable-wildcard)
(discourje.core.logging/set-logging-exceptions)
(discourje.core.logging/set-throwing true)