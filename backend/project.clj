(defproject backend "0.1-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [yada "1.2.1"]
                 [capacitor "0.6.0"]
                 [clojurewerkz/elastisch "2.2.2"]

                 [org.apache.commons/commons-math3 "3.5"]
                 [org.clojure/core.cache "0.6.5"]
                 [org.clojure/core.memoize "0.5.8"]

                 [org.clojure/tools.logging "0.4.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]]

  :main ^:skip-aot backend.core

  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}})
