(defconfig
  :netty {:port 8080}
  :vhosts [["localhost" "127.0.0.1"]
           (resizer :secret "verysecret" :store (s3store :bucket "my-bucket"
                                    :cred {:access-key "<access key goes here>"
                                           :secret-key "<secret key goes here>"}))
           ["some.other.domain.tld"]
           (resizer :secret "alsoverysecret" :store (filestore :basedir "/path/to/pictures"))])
