(defproject discourje "2.0.0"
  :description "To simplify shared-memory concurrent programming, in addition to low-level synchronisation primitives,
  several modern programming languages have started to offer core support for higher-level communication primitives as
  well, in the guise of message passing through channels. Yet, a growing body of evidence suggests that channel-based
  programming abstractions for shared memory also have their issues.

  The Discourje project aims to help programmers cope with message- passing concurrency bugs in Clojure programs, based
  on run-time verification and dynamic monitoring. The idea is that programmers write not only implementations, but also
  specifications (of sessions of channel actions). Discourje subsequently offers a library to verify that
  implementations are safe relative to specifications (= bad channel actions never happen); it is built on a formal
  foundation, inspired by process algebra and multiparty session types."

  :url "https://gitlab.com/ruben.hamers/Discourje"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "0.4.500"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [str-to-argv "0.1.1"]
                 [clojure-complete "0.2.5"]]
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"]
  :java-source-paths ["src/main/java"]
  :profiles {:examples  {:main         discourje.examples.main
                         :aot          [discourje.core.async
                                        discourje.core.spec
                                        discourje.examples.main]
                         :uberjar-name "discourje-examples.jar"}})

;; $ lein with-profile examples uberjar