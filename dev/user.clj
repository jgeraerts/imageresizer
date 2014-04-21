(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer (javadoc)]
   [clojure.pprint :refer (pprint)]
   [clojure.reflect :refer (reflect)]
   [clojure.repl :refer (apropos dir doc find-doc pst source)]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [com.stuartsierra.component :as component]
   [net.umask.imageresizer.resizer :as resizer]
   [net.umask.imageresizer.memorystore :as memstore]
   [net.umask.imageresizer.server :as imgserver]
   [net.umask.imageresizer.store :as store])
  (:use ring.middleware.params))

(defn handler [req] {:status  200
                     :headers {"Content-Type" "text/html"}
                     :body    (pr-str req)})


(defn create-test-system []
  (let [mstore (memstore/create-memstore)
        rose (io/input-stream (io/resource "rose.jpg"))]
    (do
      (store/store-write mstore "rose.jpg" rose))
    (component/system-map
     
     :resizer (resizer/create-resizer "verysecret" mstore)
     :server  (component/using  (imgserver/create-server) [:resizer]))))

(def system
  "A Var containing an object representing the application under
  development."
  nil)

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  (alter-var-root #'system (constantly (create-test-system)))
  )

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (alter-var-root #'system component/start)
  )

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  (alter-var-root #'system (fn [s] (when s (component/stop s)))))

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after 'user/go))


