(defproject backend "0.1-SNAPSHOT"

  :description "Backend of the Bitcoin Seismograph. Serves and aggregates data from multiple data sources."
  :url "http://bitcoinseismograph.info"

  :license {:name   "MIT License"
            :url    "https://opensource.org/licenses/MIT"
            :author "Versility Labs GmbH & Marcel Morisse"
            :year   2017
            :key    "mit"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [yada "1.2.1"]
                 [capacitor "0.6.0"]
                 [clojurewerkz/elastisch "2.2.2"]

                 [org.apache.commons/commons-math3 "3.5"]
                 [org.clojure/core.cache "0.6.5"]
                 [org.clojure/core.memoize "0.5.8"]

                 [org.clojure/tools.logging "0.4.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [environ "1.1.0"]]

  :plugins [[lein-environ "1.1.0"]]

  :main ^:skip-aot backend.core

  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}})
