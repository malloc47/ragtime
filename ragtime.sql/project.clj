(defproject com.malloc47/ragtime.sql "0.3.7-p4"
  :description "Ragtime migrations for SQL databases"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [com.malloc47/ragtime.core "0.3.7-p4"]
                 [org.clojure/java.jdbc "0.3.5"]]
  :profiles
  {:dev {:dependencies [[com.h2database/h2 "1.3.160"]]}})
