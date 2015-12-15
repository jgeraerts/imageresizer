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
