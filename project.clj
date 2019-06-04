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
                 [criterium "0.4.5"]]
  :source-paths ["src"]
  :main discourje.main
  :aot [discourje.main]
  ;:uberjar {:aot :all
  ;          :source-paths ["src/discourje"]
  ;          :main discourje.main}
  )
