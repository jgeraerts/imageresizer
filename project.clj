(defproject net.umask/imageresizer "0.7.0-SNAPSHOT"
  :description  "simple on the fly image resizing server"
  :url "https://github.com/jogeraerts/imageserver"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[com.twelvemonkeys.imageio/imageio-jpeg "3.2.1"]
                 [com.twelvemonkeys.imageio/imageio-tiff "3.2.1"]
                 [org.clojure/clojure "1.7.0"]
                 [http-kit "2.1.19"]
                 [netty-ring-adapter "0.4.6"]
                 [org.imgscalr/imgscalr-lib "4.2"]
                 [clj-aws-s3 "0.3.10" :exclusions [commons-logging joda-time]]
                 [joda-time/joda-time "2.9.1"]
                 [org.clojure/tools.cli "0.3.3"]
                 [com.stuartsierra/component "0.3.1"]
                 [ring "1.4.0"]
                 [digest "1.4.4"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [org.slf4j/jcl-over-slf4j "1.7.13"]
                 [com.mortennobel/java-image-scaling "0.8.6"]]
  :exclusions [commons-logging/commons-logging]
  :main net.umask.imageresizer
  :repl-options {:init-ns user}
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.10"]
                                  [ring/ring-mock "0.3.0"]
                                  [reloaded.repl "0.2.1"]
                                  [org.clojure/tools.trace "0.7.9"]]
                   :source-paths ["dev"]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0-RC3"]]}})
