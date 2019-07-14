(ns core-test
  (:require [clojure.test :refer :all]
            [hodur-engine.core :as engine]
            [hodur-datomic-schema.core :as datomic]))

(defn ^:private schema->datomic [s]
  (-> s
      engine/init-schema
      datomic/schema))

(deftest test-all
  (let [s (schema->datomic '[^{:datomic/tag true}
                             default

                             ^:interface
                             Person
                             [^String name]

                             Client
                             [^{:type ID :datomic/ns :company} id]

                             Employer
                             [^{:type ID :datomic/ns :company} id]

                             Employee
                             [^String name
                              ^{:type String
                                :doc "The very employee number of this employee"
                                :datomic/unique :db.unique/identity}
                              number
                              ^Float salary
                              ^Integer age
                              ^DateTime start-date
                              ^Employee supervisor
                              ^{:type Employee
                                :cardinality [0 n]
                                :doc "Has documentation"
                                :deprecation "But also deprecation"}
                              co-workers
                              ^{:datomic/type :db.type/keyword}
                              keyword-type
                              ^{:datomic/type :db.type/uri}
                              uri-type
                              ^{:datomic/type :db.type/double}
                              double-type
                              ^{:datomic/type :db.type/bigdec
                                :deprecation "This is deprecated" }
                              bigdec-type
                              ^EmploymentType employment-type
                              ^SearchResult last-search-results]
                             
                             ^{:union true}
                             SearchResult
                             [Employee Person EmploymentType]
                             
                             ^{:enum true}
                             EmploymentType
                             [FULL_TIME
                              ^{:doc "Documented enum"}
                              PART_TIME]])]
    (is (= [#:db{:ident :company/id
                 :valueType :db.type/uuid
                 :cardinality :db.cardinality/one}
            #:db{:ident :employee/age
                 :valueType :db.type/long
                 :cardinality :db.cardinality/one}
            #:db{:ident :employee/bigdec-type
                 :valueType :db.type/bigdec
                 :cardinality :db.cardinality/one
                 :doc "DEPRECATION NOTE: This is deprecated"}
            #:db{:ident :employee/co-workers
                 :valueType :db.type/ref
                 :cardinality :db.cardinality/many
                 :doc
                 "Has documentation\n\nDEPRECATION NOTE: But also deprecation"}
            #:db{:ident :employee/double-type
                 :valueType :db.type/double
                 :cardinality :db.cardinality/one}
            #:db{:ident :employee/employment-type
                 :valueType :db.type/ref
                 :cardinality :db.cardinality/one}
            #:db{:ident :employee/keyword-type
                 :valueType :db.type/keyword
                 :cardinality :db.cardinality/one}
            #:db{:ident :employee/last-search-results
                 :valueType :db.type/ref
                 :cardinality :db.cardinality/one}
            #:db{:ident :employee/name
                 :valueType :db.type/string
                 :cardinality :db.cardinality/one}
            #:db{:ident :employee/number
                 :valueType :db.type/string
                 :cardinality :db.cardinality/one
                 :unique :db.unique/identity
                 :doc "The very employee number of this employee"}
            #:db{:ident :employee/salary
                 :valueType :db.type/float
                 :cardinality :db.cardinality/one}
            #:db{:ident :employee/start-date
                 :valueType :db.type/instant
                 :cardinality :db.cardinality/one}
            #:db{:ident :employee/supervisor
                 :valueType :db.type/ref
                 :cardinality :db.cardinality/one}
            #:db{:ident :employee/uri-type
                 :valueType :db.type/uri
                 :cardinality :db.cardinality/one}
            #:db{:ident :employment-type/full-time}
            #:db{:ident :employment-type/part-time
                 :doc "Documented enum"}]
           s))))
