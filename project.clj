(defproject discourje "2.0.0"
  :description "The Discourje project aims to help programmers cope with channels and
concurrency bugs in Clojure programs, based on dynamic analysis. The idea is
that programmers write not only implementations of communication protocols in
their Clojure programs, but also specifications. Discourje then offers a
run-time verification library to ensure that channel actions in implementations
are safe relative to specifications."

  :url "https://github.com/discourje"
  :dependencies [[org.clojure/clojure "1.11.0"]
                 [org.clojure/core.async "0.4.500"]
                 [org.clojure/tools.nrepl "0.2.13"]]
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure" "src/test/java"]
  :java-source-paths ["src/main/java"]
  :main discourje.examples.main
  :profiles {:examples  {:main         discourje.examples.main
                         :aot          [discourje.core.async
                                        discourje.core.spec
                                        discourje.examples.main]
                         :uberjar-name "discourje-examples.jar"}
             :dev       {:dependencies [[org.junit.jupiter/junit-jupiter "5.7.0"]
                                        [org.mockito/mockito-all "1.10.19"]]}})

;; $ lein with-profile examples uberjar
