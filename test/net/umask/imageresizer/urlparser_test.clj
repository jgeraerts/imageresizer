(ns net.umask.imageresizer.urlparser_test
  (:require [net.umask.imageresizer.urlparser :refer :all]
            [clojure.test :refer :all]
            [ring.mock.request :refer :all]))

(def ^:private handler (wrap-url-parser identity) )

(defn- call-handler [url]
  (handler (request :get (str "/" url))))

(deftest urlparsing
  (testing "check if imageresizer parameters are correctly parsed"
    (are [x y] (= (assoc x :uri y)
                  (:imageresizer  (call-handler y)))
         {:original "filename.jpg" 
          :size {:width 300
                 :height 400 }} "size/300x400/filename.jpg"
         {:original "foo/bar/original.jpg"
          :size {:size 300}} "size/300/foo/bar/original.jpg"
         {:original "foo/bar/original.jpg"
          :size {:size 300}
          :rotate {:angle  30}} "rotate/30/size/300/foo/bar/original.jpg"
         {:original "foo/bar/original.jpg"
          :size {:width 300}} "size/300w/foo/bar/original.jpg"
         {:original "foo/bar/original.jpg"
          :size {:height 300}} "size/300h/foo/bar/original.jpg"
         {:original "foo/bar/original.jpg"
          :size {:height 400 :width 300 :color 0xFFEEDD}} "size/300x400-0xFFEEDD/foo/bar/original.jpg"
         {:crop {:x 10, :y 10, :width 10, :height 10}
          :size {:size 10},
          :original "foo.jpg"} "crop/10x10x10x10/size/10/foo.jpg"
         {:watermark {:x 10 :y 10 :watermark "logo"}
          :original "foo.jpg"} "watermark/10x10-logo/foo.jpg"
         {:watermark {:x -10 :y -10 :watermark "logo"}
           :original "foo.jpg"} "watermark/-10x-10-logo/foo.jpg"
         {:watermark {:anchor :topleft :watermark "logo"}
          :original "foo.jpg"} "watermark/topleft-logo/foo.jpg")))
