(ns leiningen.ragtime
  (:require [leinjacker.deps :as deps])
  (:use [leiningen.run :only (run)]))

(defn- add-ragtime-deps [project]
  (-> project
      (deps/add-if-missing '[com.malloc47/ragtime.core "0.3.7-p4"])
      (deps/add-if-missing '[com.malloc47/ragtime.sql "0.3.7-p4"])))

(defn ragtime
  "Run ragtime.main with the options specified in the project file."
  [project command & args]
  (let [migrations (-> project :ragtime :migrations str)
        database   (-> project :ragtime :database)
        project    (add-ragtime-deps project)]
    (apply run project
           "-m" "ragtime.main"
           "-r" "ragtime.sql.database"
           "-d" database
           "-m" migrations
           command args)))
