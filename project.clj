(defproject net.umask/imageresizer "0.7.0-SNAPSHOT"
  :description  "simple on the fly image resizing server"
  :url "https://github.com/jogeraerts/imageserver"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[com.twelvemonkeys.imageio/imageio-jpeg "3.1.1"]
                 [com.twelvemonkeys.imageio/imageio-tiff "3.1.1"]
                 [org.clojure/clojure "1.7.0"]
                 [http-kit "2.1.19"]
                 [netty-ring-adapter "0.4.6"]
                 [org.imgscalr/imgscalr-lib "4.2"]
                 [clj-aws-s3 "0.3.10" :exclusions [commons-logging joda-time]]
                 [joda-time/joda-time "2.8.1"]
                 [org.clojure/tools.cli "0.3.1"]
                 [com.stuartsierra/component "0.2.3"]
                 [ring "1.3.2"]
                 [digest "1.4.4"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [org.slf4j/jcl-over-slf4j "1.7.12"]
                 [com.mortennobel/java-image-scaling "0.8.6"]]
  :exclusions [commons-logging/commons-logging]
  :main net.umask.imageresizer
  :repl-options {:init-ns user}
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.10"]
                                  [ring/ring-mock "0.2.0"]
                                  [reloaded.repl "0.1.0"]]
                   :source-paths ["dev"]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0-alpha3"]]}})

 
