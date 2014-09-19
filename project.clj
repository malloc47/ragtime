(defproject com.malloc47/ragtime "0.3.7-p4"
  :description "A database-independent migration library"
  :dependencies [[com.malloc47/ragtime.core "0.3.7-p4"]
                 [com.malloc47/ragtime.sql "0.3.7-p4"]
                 [com.malloc47/ragtime.sql.files "0.3.7-p4"]]
  :plugins [[lein-sub "0.2.1"]
            [codox "0.6.7"]]
  :sub ["ragtime.core" "ragtime.sql" "ragtime.sql.files"]
  :codox {:sources ["ragtime.core/src"
                    "ragtime.sql/src"
                    "ragtime.sql.files/src"]
          :exclude [ragtime.sql.database ragtime.main]}
  :profiles
  {:dev {:dependencies [[codox/codox.core "0.6.7"]]}})
