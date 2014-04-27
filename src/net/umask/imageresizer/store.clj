(ns net.umask.imageresizer.store)

(defprotocol Store
  (store-write [this name stream] "stores the inputstream under name")
  (store-read [this name] "loads the name as inputstream"))
