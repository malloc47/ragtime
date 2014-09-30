(ns ragtime.sql.database
  (:use [ragtime.core :only (Migratable connection)])
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as sql])
  (:import java.io.FileNotFoundException
           java.util.Date
           java.text.SimpleDateFormat))

(def ^:private migrations-table "ragtime_migrations")

(defn ^:internal ensure-migrations-table-exists [db table]
  ;; TODO: is there a portable way to detect table existence?
  (try
    (sql/db-do-commands db
      (sql/create-table-ddl table
                            [:id "varchar(255)"]
                            [:created_at "varchar(32)"]))
    (catch Exception _)))

(defn format-datetime [dt]
  (-> (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS")
      (.format dt)))

(defn sql-add-migration-id
  [db table id]
  (ensure-migrations-table-exists db table)
  (sql/insert! db
               table
               [:id :created_at]
               [(str id) (format-datetime (Date.))]))

(defn sql-remove-migration-id
  [db table id]
  (ensure-migrations-table-exists db table)
  (sql/delete! db table ["id = ?" id]))

(defn sql-applied-migration-ids
  [db table]
  (ensure-migrations-table-exists db table)
  (sql/query db
             [(format "SELECT id FROM %s ORDER BY created_at" table)]
             :result-set-fn #(->> % (map :id) vec)))

(defn- print-next-ex-trace [e]
  (when e
    (when-let [next-e (.getNextException e)]
      (.printStackTrace next-e))))

(defn sql-run-migrations [db migrations]
  (sql/with-db-transaction [connection db]
    (try
      (doseq [migration migrations]
        (sql/execute! connection [migration]))
      (catch java.sql.BatchUpdateException e
        (print-next-ex-trace e)
        (throw e))
      (catch java.sql.SQLException e
        (print-next-ex-trace e)
        (throw e)))))

(defrecord SqlDatabase []
  Migratable
  (add-migration-id [db id] (sql-add-migration-id db migrations-table id))

  (remove-migration-id [db id] (sql-remove-migration-id db migrations-table id))

  (applied-migration-ids [db] (sql-applied-migration-ids db migrations-table))

  (run-migrations [db migrations] (sql-run-migrations db migrations)))

(defmethod connection "jdbc" [url]
  (map->SqlDatabase {:connection-uri url}))
