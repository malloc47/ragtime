(ns ragtime.sql.test.database
  (:use [clojure.java.jdbc :as sql]
        [clojure.test]
        [ragtime.sql.database]
        [ragtime.core :only [add-migration-id
                             remove-migration-id
                             applied-migration-ids
                             connection]]))

(def test-db
  (connection "jdbc:h2:mem:test_db"))

(deftest test-add-migrations
  (sql/with-db-transaction [connection test-db]
    (add-migration-id connection "12")
    (add-migration-id connection "13")
    (add-migration-id connection "20")
    (is (= ["12" "13" "20"] (applied-migration-ids connection)))
    (remove-migration-id connection "13")
    (is (= ["12" "20"] (applied-migration-ids connection)))))
