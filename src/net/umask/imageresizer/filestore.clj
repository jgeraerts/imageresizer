(ns net.umask.imageresizer.filestore
  (:require digest
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [net.umask.imageresizer.cache :refer [CacheProtocol]]
            [net.umask.imageresizer.nio :as nio]
            [net.umask.imageresizer.source :refer [ImageSource]]))

(defn getfile [ basedir name] 
    (io/file basedir name))

(defrecord FileImageSource [basedir]
  ImageSource
  (get-image-stream [this name]
    (let [path (nio/resolve (:basedir this) name)]
      (when (nio/exists path) 
        (io/input-stream path)))))

(defn create-file-image-store [basedir]
  (->FileImageSource basedir))

(defn- hash-path [path]
  (let [hashed (digest/md5 path)]
    (string/join "/"  [(subs hashed 0 2)
                       (subs hashed 2 4)
                       (subs hashed 4)])))

(defn resolve-path [basedir name extension]
  (nio/resolve basedir (str (hash-path name) extension)))

(defn- meta-path [basedir name]
  (resolve-path basedir name ".meta"))

(defn- blob-path [basedir name]
  (resolve-path basedir name ".blob"))

(defrecord FileCache [basedir]
  CacheProtocol
  (store! [this name response]
    (let [blobpath (blob-path basedir name)
          metapath (meta-path basedir name)
          parentdir (nio/parent blobpath)]
      (when-not (nio/exists parentdir) (nio/createDirectories parentdir))
      (with-open [blobout (io/output-stream blobpath)
                  metaout (io/output-stream metapath)]
        (io/copy (:body response) blobout)
        (io/copy (pr-str (dissoc response :body)) metaout))))
  (fetch [this name]
    (let [blobpath (blob-path basedir name)
          metapath (meta-path basedir name)]
      (when (and (nio/exists blobpath)
                 (nio/exists metapath))
        (with-open [blobin (io/input-stream blobpath)
                    metain (io/input-stream metapath)
                    bos (java.io.ByteArrayOutputStream.)]
          (let [meta (edn/read-string (slurp  metain))]
            (io/copy blobin bos)
            (assoc meta :body (.toByteArray bos))))))))

(defn create-filecache [basedir]
  (->FileCache (nio/get-path basedir)))

(defn create-filesource [basedir]
  (->FileImageSource (nio/get-path  basedir)))
