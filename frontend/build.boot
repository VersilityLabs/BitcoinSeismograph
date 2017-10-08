(set-env!
 :source-paths    #{"src/cljs" "scss"}
 :resource-paths  #{"resources"}
 :dependencies '[[org.clojure/clojure "1.9.0-beta1" :scope "provided"]
                 [org.clojure/clojurescript "1.9.946"]

                 ;; Tooling
                 [adzerk/boot-cljs "2.1.4" :scope "test"]
                 [adzerk/boot-cljs-repl "0.3.3" :scope "test"]
                 [adzerk/boot-reload "0.5.2" :scope "test"]
                 [binaryage/devtools "0.9.4" :scope "test"]
                 [com.cemerick/piggieback "0.2.2" :scope "test"]
                 [deraen/boot-sass "0.3.1" :scope "test"]
                 [org.clojure/tools.nrepl "0.2.13" :scope "test"]
                 [org.slf4j/slf4j-nop "1.8.0-alpha2" :scope "test"]
                 [onetom/boot-lein-generate "0.1.3" :scope "test"]
                 [pandeiro/boot-http "0.8.3" :scope "test"]
                 [powerlaces/boot-cljs-devtools "0.2.0" :scope "test"]
                 [weasel "0.7.0" :scope "test"]

                 ;; Application
                 [cljsjs/c3 "0.4.14-0"]
                 [cljs-http "0.1.43"]
                 [re-frame "0.10.2-beta1"]
                 [reagent "0.8.0-alpha1"]])

(require
 '[adzerk.boot-cljs :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload :refer [reload]]
 '[boot.lein :as lein]
 '[deraen.boot-sass :refer [sass]]
 '[pandeiro.boot-http :refer [serve]]
 '[powerlaces.boot-cljs-devtools :refer [cljs-devtools dirac]])

;; generate an up-to-date project.clj each time boot is run
(lein/generate)

(deftask build
  "This task contains all the necessary steps to produce a build
   You can use 'profile-tasks' like `production` and `development`
   to change parameters (like optimizations level of the cljs compiler)"
  []
  (comp (notify)
        (sass)
        (sift :move {#"main.css" "css/main.css"})
        (cljs)
        (sift)))

(deftask cider
  "Injects necessary dependencies to support cider connections."
  []
  (require 'boot.repl)
   (swap! @(resolve 'boot.repl/*default-dependencies*)
          concat '[[org.clojure/tools.nrepl "0.2.12"]
                   [cider/cider-nrepl "0.15.0"]
                   [refactor-nrepl "2.3.1"]])
   (swap! @(resolve 'boot.repl/*default-middleware*)
          concat '[cider.nrepl/cider-middleware
                   refactor-nrepl.middleware/wrap-refactor])
   identity)

(deftask run
  "The `run` task wraps the building of your application in some
   useful tools for local development: an http server, a file watcher
   a ClojureScript REPL and a hot reloading mechanism"
  []
  (comp (serve)
        (watch)
        (cljs-repl :port 3333 :nrepl-opts {:port 9001})
        (cljs-devtools)
        (reload :port 3456)
        (build)))

(deftask trim-output
  "Remove all compilation-related output"
  []
  (sift :include #{#"assets/"
                   #"css/"
                   #"favicon\.ico"
                   #"impressum\.html"
                   #"index\.html"
                   #"js/"
                   #"privacypolicy\.html"
                   #"robots\.txt"
                   #"termsofuse\.html"
                   #"vendor/"}))

(deftask development []
  (task-options! cljs {:optimizations    :none
                       :compiler-options {:closure-defines      {"goog.DEBUG" true}
                                          :source-map-timestamp true}}
                 notify {:audible true}
                 reload {:on-jsload 'gui.app/init})
  identity)

(deftask dev
  "Simple alias to run application in development mode"
  []
  (comp (development)
        (run)))

(deftask production []
  (task-options! cljs {:optimizations :advanced})
  identity)

(deftask prod
  "Simple alias to bundle the application for production"
  []
  (comp (production)
        (build)
        (trim-output)
        (target)))
