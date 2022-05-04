(defproject tech.gojek/bulwark "1.2.1"
  :description "Hystrix for Clojurists"
  :url "https://github.com/gojektech/bulwark"
  :license {:name "Apache License, Version 2.0"
            :url  "https://www.apache.org/licenses/LICENSE-2.0"}
  :deploy-repositories [["clojars" {:url           "https://clojars.org/repo"
                                    :username      :env/clojars_username
                                    :password      :env/clojars_password
                                    :sign-releases false}]]
  :dependencies [[com.netflix.hystrix/hystrix-clj "1.5.12"]
                 [org.slf4j/slf4j-api "1.7.25"]]
  :profiles {:test {:plugins [[lein-cloverage "1.0.13"]]}
             :dev  {:plugins      [[jonase/eastwood "0.2.6"]
                                   [lein-cljfmt "0.6.3"]
                                   [lein-cloverage "1.0.13"]
                                   [lein-kibit "0.1.6"]]
                    :dependencies [[org.clojure/clojure "1.9.0"]
                                   [org.apache.logging.log4j/log4j-slf4j-impl "2.17.2"]]}}
  :cljfmt {:indents {bulwark/with-hystrix [[:inner 0]]}})
