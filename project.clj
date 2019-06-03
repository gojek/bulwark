(defproject tech.gojek/bulwark "1.0.1"
  :description "Hystrix for Clojurists"
  :url "https://github.com/gojektech/bulwark"
  :license {:name "Apache License, Version 2.0"
            :url  "https://www.apache.org/licenses/LICENSE-2.0"}
  :repl-options {:host "0.0.0.0"
                 :port 1337}
  :dependencies [[com.netflix.hystrix/hystrix-clj "1.5.12"]
                 [org.slf4j/slf4j-api "1.7.25"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]]}})
