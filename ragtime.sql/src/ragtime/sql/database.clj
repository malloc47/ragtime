(ns ragtime.sql.database
  (:use [ragtime.core :only (Migratable connection)])
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as sql])
  (:import java.io.FileNotFoundException
           java.util.Date
           java.text.SimpleDateFormat))

(def ^:private migrations-table "ragtime_migrations")

(defn ^:internal ensure-migrations-table-exists [db]
  ;; TODO: is there a portable way to detect table existence?
  (try
    (sql/db-do-commands db
      true
      (sql/create-table-ddl migrations-table
                            [:id "varchar(255)"]
                            [:created_at "varchar(32)"]))
    (catch Exception _)))

(defn format-datetime [dt]
  (-> (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS")
      (.format dt)))

(defrecord SqlDatabase []
  Migratable
  (add-migration-id [db id]
    (sql/with-db-transaction [connection db]
      (ensure-migrations-table-exists connection)
      (sql/insert! connection
                   migrations-table
                   [:id :created_at]
                   [(str id) (format-datetime (Date.))])))

  (remove-migration-id [db id]
    (sql/with-db-transaction [connection db]
      (ensure-migrations-table-exists connection)
      (sql/delete! connection migrations-table ["id = ?" id])))

  (applied-migration-ids [db]
    (sql/with-db-transaction [connection db]
      (ensure-migrations-table-exists connection)
      (sql/query connection
                 [(format "SELECT id FROM %s ORDER BY created_at" migrations-table)]
                 :result-set-fn #(->> % (map :id) vec)))))

(defmethod connection "jdbc" [url]
  (map->SqlDatabase {:connection-uri url}))
