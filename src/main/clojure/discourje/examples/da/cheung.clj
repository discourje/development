(ns discourje.examples.da.cheung
  (:require [discourje.core.spec :as s]
            [discourje.core.lint :as l]
            [discourje.examples.config :as config]
            [discourje.examples.da.topologies :as topologies]))

;;;;;
;;;;; Specification
;;;;;

(s/defrole ::process)

(s/defsession ::cheung [initiator topology]
  (::cheung nil initiator {} topology))

(s/defsession ::cheung [i j parents topology]
  (s/let [neighbours (get topology j)
          todo (s/difference neighbours (set (keys parents)))]

         (s/if (not (contains? parents j))
           (::cheung i j (assoc parents j i) topology)

           (s/if (not (empty? todo))
             (s/alt-every [k todo]
               (s/cat (s/--> (::process j) (::process k))
                      (::cheung j k parents topology)))

             (s/let [k (get parents j)]
                    (s/if (some? k)
                      (s/cat (s/--> (::process j) (::process k))
                             (::cheung j k parents topology))))))))

(defn spec []
  (cheung (:initiator config/*input*)
          (condp = (:topology config/*input*)
            :ring (topologies/ring (:k config/*input*))
            :tree (topologies/tree (:k config/*input*))
            :mesh-2d (topologies/mesh-2d (:k config/*input*))
            :star (topologies/star (:k config/*input*))
            :mesh-full (topologies/mesh-full (:k config/*input*)))))

(when (some? config/*lint*)
  (set! config/*output* (l/lint (spec))))