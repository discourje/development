(defproject discourje "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "0.4.500"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [danlentz/clj-uuid "0.1.9"]
                 [str-to-argv "0.1.1"]
                 [clojure-complete "0.2.5"]
                 ]
  :source-paths ["src"]
  :java-source-paths ["src"]
  ;:java-source-paths ["src/discourje/demo/javaObjects"]
  :profiles {:discourje {:main discourje.main
                         :aot  [discourje.main]}
             :tacas2020 {:main         discourje.examples.tacas2020.main
                         :aot          [discourje.examples.tacas2020.main]
                         :uberjar-name "tacas2020.jar"}})

;; $ lein with-profile default uberjar
;; $ lein with-profile tacas2020 uberjar