(ns net.umask.imageresizer
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :refer [info]]
            [com.stuartsierra.component :as component]
            [net.umask.imageresizer.config :refer [load-config]]
            [net.umask.imageresizer.server :refer [create-server]])
  (:gen-class))

(defn usage [options-summary]
  (->> ["Available options:"
        options-summary
        ]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))


(def cli-options
  ;; An option with a required argument
  [["-c" "--config FILE" "Configuration File"
    :validate [#(.exists (io/file %)) "File doesn't exist"]]
   ["-h" "--help"]])

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (info "Starting image resizer...")
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Handle help and error conditions
    (cond
     (:help options) (exit 0 (usage summary))
     errors (exit 1 (error-msg errors)))
    (let [{:keys [config]} options
          c (load-config config)
          http-config (:httpconfig c)
          vhosts (:vhosts c)]
      (do  (component/start
            (component/system-map
             :vhost vhosts
             :server (component/using (create-server http-config) [:vhost])))
           (info "Server started. Listening on port" (get-in http-config [:port]))
           nil))))
