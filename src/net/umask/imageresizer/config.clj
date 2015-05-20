(ns net.umask.imageresizer.config
  (:require [net.umask.imageresizer.httpsource :refer [create-httpsource]]
            [net.umask.imageresizer.filestore :refer [create-filecache
                                                      create-filesource]]
            [net.umask.imageresizer.resizer :refer [create-resizer]]
            [net.umask.imageresizer.s3store :refer [create-s3cache
                                                    create-s3source]]))

(defn resizer [&{:keys [secret source cache]}]
  (create-resizer secret source cache))

(defn s3source [&{:keys [bucket cred]}]
  (create-s3source bucket cred))

(defn s3cache [&{:keys [bucket cred]}]
  (create-s3cache bucket cred))

(defn filesource [&{:keys [basedir]}]
  (create-filesource basedir))

(defn filecache [&{:keys [basedir]}]
  (create-filecache basedir))

(defn httpsource [& {:keys [url] :as rest}]
  (apply create-httpsource url (mapcat seq rest)))

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
