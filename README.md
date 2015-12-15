[![Build Status](https://travis-ci.org/jgeraerts/imageresizer.svg?branch=master)](https://travis-ci.org/jgeraerts/imageresizer)

# Image Resizer

Dynamic image resizing server

## Configuration

The image server is configured through a `config.clj` file which is passed as argument. The sample configuration shown below can be found in the repository [sample-config.clj](../master/sample-config.clj). 


```clojure
(defconfig
  :http {:port 8080}
  :vhosts [["localhost"
            "127.0.0.1"] (resizer :secret "verysecret"
                                  :watermarks (filesource :basedir "/path/to/my/watermarks")
                                  :source (s3source :bucket "bucketname"
                                                     :cred {:endpoint "<endpoint goes here>"
                                                            ; example https://s3-eu-west-1.amazonaws.com  
                                                            :access-key "<access key goes here>"
                                                            :secret-key "<secret key goes here>"})
                                  :cache (filecache :basedir "/var/lib/imgcache"))
            ["some.other.domain.tld"
             "some.alias.domain.tld"] (resizer :secret "alsoverysecret"
                                               :source (httpsource :url "http://u.r.l/basepath/"
                                                                   :rewrite {:fromregex #"^([0-9a-zA-Z]{160})\.jpg$"
                                                                             :replacement "/medias/ignore.jpg?context=$1"})
                                               :cache (s3cache :bucket "cache"
                                                               :cred {:endpoint "<endpoint goes here>"
                                                                       ; example https://s3-eu-west-1.amazonaws.com  
                                                                       :access-key "<access key goes here>"
                                                                       :secret-key "<secret key goes here>"}))])
```

The configuration file has 2 global keys. `:http` specifies the port the server will be listening on. The `:vhosts` key is an vector containing all the virtual hosts of the server. Every virtual host definition is in the format `[alias1 alias2 alias3] (resizer <resizerconfiguration>`.

The resizer is configured with a `:secret` which is used to validate the checksum. Then a resizer needs a `:source` `:watermarks` `:cache`. A `:source` is used as a source of images that need to be resized. The `:watermarks` option is given a source to the location of the watermarks. A `:cache` in its turn is used to write the resized images to. When a request for a resized images is found in the cache, the cached result is returned.

Currently there are 3 types of sources and 2 types of caches. The sources can both be used for the `:source` and `:watermarks` option. 

## File source

```clojure
(filesource :basedir "/path/to/images")
```

## S3 source

```clojure
(s3source :bucket "bucketname"
          :cred {:endpoint "<endpoint goes here>"
                 ; example https://s3-eu-west-1.amazonaws.com  
                 :access-key "<access key goes here>"
                 :secret-key "<secret key goes here>"})
```

## Http source

```clojure
(httpsource :url "http://u.r.l/basepath/"
            :rewrite {:fromregex #"^([0-9a-zA-Z]{160})\.jpg$"
                      :replacement "/medias/ignore.jpg?context=$1"})
```

The `:rewrite` option can be used to rewrite the path of the original image as seen by the imageresizer to another path on the http endpoint.

## S3 cache

```clojure
(s3cache  :bucket "bucketname"
          :cred {:endpoint "<endpoint goes here>"
                 ; example https://s3-eu-west-1.amazonaws.com  
                 :access-key "<access key goes here>"
                 :secret-key "<secret key goes here>"})
```

## File Cache

```clojure
(filecache :basedir "/var/lib/imgcache")
```

## Starting the server

```
java -jar imageresizer-${VERSION}.jar -c config.clj
```

## API format

The basic api url structure for the image resizer is `http://hostname:port/checksum/key1/value1/key2/value2/originalimagename`

The checksum is calculated as
```
md5sum (secretkey + 'key1/value1/key2/value2/originalimagename')
```

The secretkey should match the `:secret` from the virtual host definition you're using. All mismatching checksums are rejected with a http status 400. This prevents malicious users of generating many variants of a specific image and eating up your precious resources. The `originalimagename` parameter is the path relative to the configured store of the original image name. 

Next you can specify multiple resize operations in the form of key/value pairs.

key | value | description
----|-------|------------
watermark | $(anchor)-watermarkref.ext | Adds a watermark image with reference `watermarkref` to the image. This watermarkref is resolved from the `:watermarks` source given in the configuration. Anchor can be one of topleft, topcenter, topright, midleft, midcenter, midright, bottomleft, bottomcenter, bottomright
watermark | $(xoffset)x$(yoffset)-watermarkref.ext | Same as above, but instead of using an anchor, add the topleft corner of the watermark image to location `xoffset,yoffset` in the baseimage. 
crop | $(xoffset)x$(yoffset)x$(width)x$(height) | This crops the image starting from `xoffset,yoffset` from the top left corner of the images and cuts out a picture of width x height pixels. 
size | $(size) | this resizes the image by keeping the aspect ratio and `size` will be used as the longest edge
size | $(size)w | this resizes the image by keeping the aspect ratio and `size` will be used as the width
size | $(size)h | this resizes the image by keeping the aspect ratio and `size` will be used as the height
size | $(width)wx$(height) | this resizes the image to the given dimensions of `width x height`. This crops the images from the center so to change the aspect ratio
size | $(width)wx$(height)-0x$(rgbcolor) | this resizes the images to the given dimensions of `width x height` but instead of cropping from the center it pads the border with a color with rgb value `rgbcolor` to match the new aspect ratio.
expires | $(timestamp) | if a link is accessed after `timestamp` a 404 is generated. If it is accessed before normal processing applies. However the `Cache-Control` headers and `Expires` headers are calculated relatively to the expiration timestamp. The `timestamp` is in milliseconds since epoch. 

The operations can be combined in multiple key value pairs. Check the unit tests for [examples] (../master/test/net/umask/imageresizer/resizer_test.clj#L61) of urls. 

## Copyright and License

Copyright © 2015 Jo Geraerts

[Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html)
