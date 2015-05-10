(ns net.umask.imageresizer.config
  (:require [net.umask.imageresizer.filestore :refer [create-filestore]]
            [net.umask.imageresizer.resizer :refer [create-resizer]]
            [net.umask.imageresizer.s3store :refer [create-s3store]]))

(defn resizer [&{:keys [secret store]}]
  (create-resizer secret store))

(defn s3store [&{:keys [bucket cred]}]
  (create-s3store bucket cred))

(defn filestore [&{:keys [basedir]}]
  (create-filestore basedir))

(defn- vhost-alias [aliases app]
  (reduce #(assoc %1 %2 app) {} (if (vector? aliases) aliases [aliases])))

(defn make-vhostmap [vhosts]
  (assert (even? (count vhosts)) "vhosts should have an even amount of elements")
  (reduce #(merge %1 (vhost-alias (first %2) (second %2))) {} (partition 2 vhosts)))

(defn defconfig [&{:keys [http vhosts]}]
  {:httpconfig http
   :vhosts (make-vhostmap vhosts)})

(defn load-config [file]
  (binding [*ns* *ns*] 
           (in-ns 'net.umask.imageresizer.config)
           (load-file file)))
