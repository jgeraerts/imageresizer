(ns net.umask.imageresizer.source)

(defprotocol ImageSource
  (get-image-stream [this name]))
