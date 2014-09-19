(ns ragtime.sql.test.files
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as sql])
  (:use clojure.test
        ragtime.sql.files
        ragtime.sql.database
        ragtime.core))

(def test-db
  (connection "jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1"))

(defn table-exists? [db table-name]
  (not-empty
   (sql/query db ["select true from information_schema.tables where table_name = ?"
                       (str/upper-case table-name)])))

(deftest test-sql-statements
  (are [x y] (= (sql-statements x) y)
    "foo;bar"    ["foo" "bar"]
    "foo;; bar;" ["foo" "bar"]
    "'foo;bar'"  ["'foo;bar'"]
    "`foo;bar`"  ["`foo;bar`"]
    "\"fo;ba\""  ["\"fo;ba\""]
    "'a;b' c; d" ["'a;b' c" "d"]))

(deftest test-migrations
  (testing "no migration directory"
    (is (= (migrations) [])))
  (testing "custom migration directory"
    (let [migs (migrations "test/migrations")]
      (is (= (count migs) 1))
      (is (= (:id (first migs)) "20111202110600-create-foo-table"))
      (migrate-all test-db migs)
      (is (table-exists? test-db "ragtime_migrations"))
      (is (table-exists? test-db "foo")))))
