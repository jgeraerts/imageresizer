(ns net.umask.imageresizer.util)

(defn trim-leading-slash [str]
  (let [firstchar (first (seq str))]
    (if (= \/ firstchar)
      (subs str 1)
      str)))
