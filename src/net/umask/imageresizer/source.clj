(ns net.umask.imageresizer.source)

(defprotocol ImageSource
  (get-image-stream [this name]))

(extend-protocol ImageSource
  nil
  (get-image-stream [this name]
    nil))
