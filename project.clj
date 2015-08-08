(defproject gc-api-browser "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs" "src/env/dev" "src/env/prod"]
  :clean-targets ^{:protect false} ["resources/public/js/app.js" "resources/public/js/out" "dist/app.js"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.48"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.omcljs/om "0.9.0"]
                 [cljs-http "0.1.35"]
                 [com.cognitect/transit-cljs "0.8.220"]
                 [com.novemberain/pantomime "2.7.0"]
                 [clj-aws-s3 "0.3.10" :exclusions [joda-time]]]


  :plugins [[lein-cljsbuild "1.0.6"]
            [cider/cider-nrepl "0.9.1"]
            [lein-figwheel "0.3.7"]]

  :min-lein-version "2.5.0"

  :uberjar-name "gc-api-browser.jar"

  :cljsbuild {:builds [{:id "main"
                        :figwheel {:on-jsload "gc-api-browser.core/main"}
                        :source-paths ["src/cljs" "src/env/dev"]
                        :compiler {:output-to     "resources/public/js/app.js"
                                   :output-dir    "resources/public/js/out"
                                   :main          "gc-api-browser.main"
                                   :asset-path    "/js/out"
                                   :verbose       true
                                   :optimizations :none}}
                       {:id "prod"
                        :source-paths ["src/cljs" "src/env/prod"]
                        :compiler {:output-to     "dist/app.js"
                                   :main          "gc-api-browser.main"
                                   :optimizations :advanced
                                   :pretty-print  false}}]}

  :figwheel {:http-server-root "public" ;; default and assumes "resources"
             :server-port 3010 ;; default is 3449
             :css-dirs ["resources/public/css"]
             ;; :open-file-command "emacsclient"
             ;; Start an nREPL server into the running fighweel process
             :nrepl-port 7888
             ;; to disable to launched repl
             :repl false
             ;; to specify a server logfile
             ;; :server-logfile "tmp/logs/test-server-logfile.log"
             ;; if you want to embed a server in figwheel do it like so:
             ;; :ring-handler example.server/handler
             }

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.nrepl "0.2.10"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}

             :uberjar {:omit-source true
                       :aot :all
                       :cljsbuild {:builds {:main
                                            {:compiler
                                             {:optimizations :advanced
                                              :pretty-print false}}}}}})
