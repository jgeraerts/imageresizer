[![Build Status](https://travis-ci.org/jgeraerts/imageresizer.svg?branch=master)](https://travis-ci.org/jgeraerts/imageresizer)

# Image Resizer

Dynamic image resizing server

## Configuration

The image server is configured through a `config.clj` file which is passed as argument. A sample configuration can be found in [sample-config.clj](../master/sample-config.clj)

The configuration file has 2 global keys. `:http` specifies the port the server will be listening on. The `:vhosts` key is an vector containing all the virtual hosts of the server. Every virtual host definition is in the format `[alias1 alias2 alias3] (resizer <resizerconfiguration>`. Then the resizer should be configured with a `:secret` which is a key used to checksum the url's. The resizer also needs a `:store` where it should fetch the original images and where the cached versions are written to. Currenly following stores are supported:

## File store
<tbd>

## S3 Store
<tbd>

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
crop | $(xoffset)x$(yoffset)x$(width)x$(height) | This crops the image starting from `xoffset,yoffset` from the top left corner of the images and cuts out a picture of width x height pixels. 
size | $(size) | this resizes the image by keeping the aspect ratio and `size` will be used as the longest edge
size | $(size)w | this resizes the image by keeping the aspect ratio and `size` will be used as the width
size | $(size)h | this resizes the image by keeping the aspect ratio and `size` will be used as the height
size | $(width)wx$(height) | this resizes the image to the given dimensions of `width x height`. This crops the images from the center so to change the aspect ratio
size | $(width)wx$(height)-0x$(rgbcolor) | this resizes the images to the given dimensions of `width x height` but instead of cropping from the center it pads the border with a color with rgb value `rgbcolor` to match the new aspect ratio. 

The operations can be combined in multiple key value pairs. Check the unit tests for [examples] (../master/test/net/umask/imageresizer/resizer_test.clj#L61) of urls. 

## Copyright and License

Copyright © 2015 Jo Geraerts

[Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html)
