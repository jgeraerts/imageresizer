(defproject net.umask/imageresizer "0.1.0-SNAPSHOT"
  :description simple on the fly image resizing server
  :url "https://github.com/jogeraerts/imageserver"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [netty-ring-adapter "0.4.6"]
                 [midje "1.6.3"]
                 [org.imgscalr/imgscalr-lib "4.2"]
                 [clj-aws-s3 "0.3.8"]
                 [org.clojure/tools.cli "0.3.1"]
                 [com.stuartsierra/component "0.2.1"]
                 [ring "1.2.2"]
                 ]

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"] [ring-mock "0.1.5"]]
                   :source-paths ["dev"]}})

 
