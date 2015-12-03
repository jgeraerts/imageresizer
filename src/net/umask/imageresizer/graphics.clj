(ns net.umask.imageresizer.graphics
  (:import [java.awt.image BufferedImage]
           [java.awt Graphics]))


(defprotocol ToGraphics
  (^java.awt.Graphics graphics [this]))

(extend-protocol ToGraphics
  BufferedImage
  (graphics [this] (.getGraphics this)))

(defmacro with-graphics [d & body]
  (let [i (gensym 'd)
        g (gensym 'g)]
    `(let [~i ~d
           ~g (graphics ~i)]
      (try
        (doto ~g ~@body)
        (finally (.dispose ~g)))
      ~i)))

