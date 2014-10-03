(ns ragtime.sql.files
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.java.jdbc :as sql]
            [ragtime.core :as core]))

(def ^:private migration-pattern
  #"(.*)\.(up|down)\.sql$")

(defn migration? [filename]
  (re-find migration-pattern filename))

(defn migration-id [filename]
  (second (re-find migration-pattern filename)))

(defn get-migration-files [dir]
  (->> (.listFiles (io/file dir))
       (filter (comp migration? #(.getName (io/file %))))
       (sort)
       (group-by (comp migration-id #(.getName (io/file %))))))

;; Lexer borrowed from Clout

(defn- lex-1 [src clauses]
  (some
    (fn [[re action]]
      (let [matcher (re-matcher re src)]
        (if (.lookingAt matcher)
          [(if (fn? action) (action matcher) action)
           (subs src (.end matcher))])))
    (partition 2 clauses)))

(defn- lex [src & clauses]
  (loop [results []
         src     src
         clauses clauses]
    (if-let [[result src] (lex-1 src clauses)]
      (let [results (conj results result)]
        (if (= src "")
          results
          (recur results src clauses))))))

(defn- quoted-string [quote]
  (re-pattern
   (str quote "(?:[^" quote "]|\\\\" quote ")*" quote)))

(def ^:private sql-end-marker
  "__END_OF_SQL_SCRIPT__")

(defn- mark-sql-statement-ends [sql]
  (apply str
    (lex sql
      (quoted-string \') #(.group %)
      (quoted-string \") #(.group %)
      (quoted-string \`) #(.group %)
      #"[^'\"`;]+"       #(.group %)
      #";"               sql-end-marker)))

(defn- split-sql [sql]
  (-> (mark-sql-statement-ends sql)
      (str/split (re-pattern sql-end-marker))))

(defn sql-statements
  "Split a SQL script into its component statements."
  [sql]
  (->> (split-sql sql)
       (map str/trim)
       (remove str/blank?)))

(defn run-psql-fn [file]
  (fn [db]
    (core/run-migrations db [(slurp file)])))

(defn run-sql-fn [file]
  (fn [db]
    (core/run-migrations db (sql-statements (slurp file)))))

(defn- make-migration [run-fn [id [down up]]]
  {:id       id
   :up       (run-fn up)
   :down     (run-fn down)
   :sql-up   (slurp up)
   :sql-down (slurp down)})

(def ^:private default-dir "migrations")

(defn migrations
  "Return a list of migrations to apply. Takes an optional map as the
  second argument, where the function used to generate sql commands
  can be overridden with the :run-fn key and the function used to
  generate the file list from the directory name can be overriden
  with :file-list-fn."
  ([] (migrations default-dir {}))
  ([dir] (migrations dir {}))
  ([dir {:keys [run-fn file-list-fn] :or {run-fn run-sql-fn
                                          file-list-fn get-migration-files}}]
     (->> (file-list-fn dir)
          (map (partial make-migration run-fn))
          (sort-by :id))))
