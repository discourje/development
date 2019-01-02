(ns discourje.core.dataStructures)

;monitor for a single send action
(defrecord sendM [action from to])
;monitor for a single recv action
(defrecord receiveM [action to from])
;We also need a data structure to create a conditional with branches.
;When the protocol encounters this it will check the conditional and continue on the correct branch.
(defrecord choice [trueBranch falseBranch])
;recursion construct
(defrecord recursion [name protocol])
;recur or end the recursion block
(defrecord recur! [name status])
;protocol(interface) to implement in object
(defprotocol role
  (send-to [this action value to])
  (receive-by [this action from callback]))