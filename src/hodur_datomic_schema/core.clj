(ns hodur-datomic-schema.core
  (:require [camel-snake-kebab.core :refer [->kebab-case-string]]
            [datascript.core :as d]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parsing functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti ^:private primitive-or-ref-type
  (fn [field]
    (let [ref-type (-> field :field/type :type/name)
          dat-type (-> field :datomic/type)]
      (or dat-type ref-type))))

(defmethod primitive-or-ref-type "String" [_] :db.type/string)

(defmethod primitive-or-ref-type "Float" [_] :db.type/float)

(defmethod primitive-or-ref-type "Integer" [_] :db.type/long)

(defmethod primitive-or-ref-type "Boolean" [_] :db.type/boolean)

(defmethod primitive-or-ref-type "DateTime" [_] :db.type/instant)

(defmethod primitive-or-ref-type "ID" [_] :db.type/uuid)

(defmethod primitive-or-ref-type :default [_] :db.type/ref)

(defn ^:private get-value-type
  [field]
  (if-let [dat-type (-> field :datomic/type)]
    dat-type
    (primitive-or-ref-type field)))

(defn ^:private get-cardinality
  "Given a field, returns the Datomic :db.cardinality"
  [{:keys [field/arity]}]
  (if arity
    (if (and (= (first arity) 1)
             (= (second arity) 1))
      :db.cardinality/one
      :db.cardinality/many) 
    :db.cardinality/one))

(defn ^:private assoc-documentation
  [m {:keys [field/doc field/deprecation]}]
  (if (or doc deprecation)
    (assoc m :db/doc
           (cond-> ""
             doc                   (str doc)
             (and doc deprecation) (str "\n\n")
             deprecation           (str "DEPRECATION NOTE: " deprecation)))
    m))

(defn ^:private assoc-attributes
  [m field]
  (let [table {:datomic/isComponent :db/isComponent
               :datomic/fulltext :db/fulltext
               :datomic/index :db/index
               :datomic/unique :db/unique
               :datomic/noHistory :db/noHistory}]
    (reduce-kv (fn [a k v]
                 (if-let [entry (get table k)]
                   (assoc a entry v)
                   a))
               m field)))

(defn ^:private process-field
  [entity-id {:keys [field/name] :as field}]
  (-> {:db/ident (keyword entity-id
                          (->kebab-case-string name))
       :db/valueType (get-value-type field)
       :db/cardinality (get-cardinality field)}
      (assoc-documentation field)
      (assoc-attributes field)))


(defmulti ^:private get-type
  (fn [{:keys [datomic/enum]}]
    (if enum :enum :type)))

(defmethod get-type :type
  [{:keys [type/name field/_parent] :as t}]
  (let [entity-id (->kebab-case-string name)]
    (reduce (fn [c field]
              (conj c (process-field entity-id field)))
            [] _parent)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn schema
  [conn]
  (let [types
        (d/q '[:find
               [(pull ?e [* {:field/_parent
                             [* {:field/type [*]}]}]) ...]
               :where
               [?e :type/nature :user]
               [?e :datomic/tag true]]
             @conn)]
    (reduce (fn [c t]
              (concat c (get-type t)))
            [] types)))


(comment
  (require '[hodur-engine.core :as engine])

  (def conn (engine/init-schema
             '[^{:datomic/tag true}
               default

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
                  :arity [0 n]}
                co-workers
                ^{:datomic/type :db.type/keyword}
                keword-type
                ^{:datomic/type :db.type/uri}
                uri-type
                ^{:datomic/type :db.type/double}
                double-type
                ^{:datomic/type :db.type/bigdec
                  :deprecation "This is deprecated" }
                bigdec-type]]))

  (clojure.pprint/pprint
   (schema conn)))
