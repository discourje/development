;(defproject discourje "0.1.0"
;  :description "FIXME: write description"
;  :url "http://example.com/FIXME"
;  :license {:name "Eclipse Public License"
;            :url  "http://www.eclipse.org/legal/epl-v10.html"}
;  :dependencies [[org.clojure/clojure "1.8.0"]
;                 [org.clojure/test.check "0.9.0"]
;                 [org.clojure/core.async "0.4.474"]
;                 [org.clojure/tools.nrepl "0.2.13"]
;                 [slingshot "0.12.2"]
;                 [danlentz/clj-uuid "0.1.7"]
;                 [criterium "0.4.5"]
;                 [str-to-argv "0.1.1"]
;                 [clojure-complete "0.2.5"]]
;  :source-paths ["src"]
;  :java-source-paths ["src"]
;  ;:java-source-paths ["src/discourje/demo/javaObjects"]
;  :profiles {:default   {:main discourje.main
;                         :aot  [discourje.main]}
;             :tacas2020 {:main         discourje.examples.tacas2020.main
;                         :aot          [discourje.examples.tacas2020.main]
;                         :uberjar-name "tacas2020.jar"}})
;
;;; $ lein with-profile default uberjar
;;; $ lein with-profile tacas2020 uberjar


(defproject discourje "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  ;  :java-source-paths ["src/discourje/demo/javaObjects"]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/test.check "0.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [slingshot "0.12.2"]
                 [danlentz/clj-uuid "0.1.7"]
                 [criterium "0.4.5"]
                 [str-to-argv "0.1.1"]]
  :source-paths ["src"]
  :main discourje.main
  :aot [discourje.main]
  ;:uberjar {:aot :all
  ;          :source-paths ["src/discourje"]
  ;          :main discourje.main}
  )

