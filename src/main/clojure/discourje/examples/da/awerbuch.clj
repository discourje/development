(ns discourje.examples.da.awerbuch
  (:require [discourje.core.spec :as s]
            [discourje.core.lint :as l]
            [discourje.examples.config :as config]
            [discourje.examples.da.topologies :as topologies]))

;;;;;
;;;;; Specification
;;;;;

(s/defrole ::process)

(s/defsession ::awerbuch [initiator topology]
  (::awerbuch nil initiator {} topology))

(s/defsession ::awerbuch [i j parents topology]
  (s/let [neighbours (get topology j)
          todo (s/difference neighbours (set (keys parents)))]

         (s/if (not (contains? parents j))

           (s/cat (s/par-every [k (remove #{i} neighbours)]
                    (s/cat (s/--> (::process j) (::process k))
                           (s/--> (::process k) (::process j))))
                  (::awerbuch i j (assoc parents j i) topology))

           (s/if (not (empty? todo))
             (s/alt-every [k todo]
               (s/cat (s/--> (::process j) (::process k))
                      (::awerbuch j k parents topology)))

             (s/let [k (get parents j)]
                    (s/if (some? k)
                      (s/cat (s/--> (::process j) (::process k))
                             (::awerbuch j k parents topology))))))))

(defn spec []
  (awerbuch (:initiator config/*input*)
            (condp = (:topology config/*input*)
              :ring (topologies/ring (:k config/*input*))
              :tree (topologies/tree (:k config/*input*))
              :mesh-2d (topologies/mesh-2d (:k config/*input*))
              :star (topologies/star (:k config/*input*))
              :mesh-full (topologies/mesh-full (:k config/*input*)))))

(when (some? config/*lint*)
  (set! config/*output* (l/lint (spec))))