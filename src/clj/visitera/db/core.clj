(ns visitera.db.core
  (:require
   [datomic.api :as d]
   [io.rkn.conformity :as c]
   [mount.core :refer [defstate]]
   [visitera.config :refer [env]]
   [clojure.string :as str]
   [buddy.hashers :as hs]))

(defstate conn
  :start (do (-> env :database-url d/create-database) (-> env :database-url d/connect))
  :stop (-> conn .release))

(def db-resources
  ["migrations/schema.edn"
   "migrations/countries-data.edn"
   "migrations/test-data.edn"])

(defn install-schema
  [conn]
  (for [resource db-resources]
    (let [norms-map (c/read-resource resource)]
      (c/ensure-conforms conn norms-map (keys norms-map)))))

(defn show-schema
  "Show currently installed schema"
  [conn]
  (let [system-ns #{"db" "db.type" "db.install" "db.part"
                    "db.lang" "fressian" "db.unique" "db.excise"
                    "db.cardinality" "db.fn" "db.sys" "db.bootstrap"
                    "db.alter"}]
    (d/q '[:find ?ident
           :in $ ?system-ns
           :where
           [?e :db/ident ?ident]
           [(namespace ?ident) ?ns]
           [((comp not contains?) ?system-ns ?ns)]]
         (d/db conn) system-ns)))

(defn show-transaction
  "Show all the transaction data
   e.g.
    (-> conn show-transaction count)
    => the number of transaction"
  [conn]
  (seq (d/tx-range (d/log conn) nil nil)))

(defn find-one-by
  "Given db value and an (attr/val), return the user as EntityMap (datomic.query.EntityMap)
   If there is no result, return nil.

   e.g.
    (d/touch (find-one-by (d/db conn) :user/email \"user@example.com\"))
    => show all fields
    (:user/first-name (find-one-by (d/db conn) :user/email \"user@example.com\"))
    => show first-name field"
  [db attr val]
  (d/entity db
            ;;find Specifications using ':find ?a .' will return single scalar
            (d/q '[:find ?e .
                   :in $ ?attr ?val
                   :where [?e ?attr ?val]]
                 db attr val)))

(defn add-user
  "Adds new user to a database"
  [conn {:keys [email password]}]
  (when-not (find-one-by (d/db conn) :user/email email)
    @(d/transact conn [{:user/email    email
                        :user/password (hs/derive password)}])))

(defn find-user
  "Find user by email"
  [db email]
  (if-let [user-id (find-one-by db :user/email email)]
    (d/touch user-id)))

(defn find-country
  "Find country by name"
  [db name]
  (d/touch (find-one-by db :country/name name)))

(defn delete-database
  []
  (-> env :database-url d/delete-database))

(defn get-country-id-by-alpha-3
  [db alpha-3]
  (-> (find-one-by db :country/alpha-3 alpha-3)
      (d/touch)
      (:db/id)))

(defn concat-keyword [part-1 part-2]
  (let [name-1 (str/replace part-1 #"^:" "")
        name-2 (name part-2)]
    (-> (str name-1 name-2)
        (keyword))))

(defn remove-from-countries
  "Remove country from list"
  [type conn user-email alpha-3]
  (let [user-id (-> (find-user (d/db conn) user-email)
                    (:db/id))
        country-id (get-country-id-by-alpha-3 (d/db conn) alpha-3)
        attr (concat-keyword :user/countries- type)]
    @(d/transact conn [[:db/retract user-id attr country-id]])))

(defn add-to-countries
  "Add country to visited list"
  [type conn user-email alpha-3]
  (when-let [country-id (get-country-id-by-alpha-3 (d/db conn) alpha-3)]
    (case type
      :visited  (remove-from-countries :to-visit conn user-email alpha-3)
      :to-visit (remove-from-countries :visited  conn user-email alpha-3))
    (let [attr (concat-keyword :user/countries- type)
          tx-user {:user/email user-email
                   attr        [country-id]}]
      @(d/transact conn [tx-user]))))

(defn- format-countries [countries]
  (let [countries-content (-> countries (first) (first))
        map-fn (fn [el] (:country/alpha-2 el))]
    {:visited  (map map-fn (:user/countries-visited countries-content))
     :to-visit (map map-fn (:user/countries-to-visit countries-content))}))

(defn get-countries [db user-email]
  (-> (d/q '[:find (pull ?e
                         [{:user/countries-to-visit
                           [:country/alpha-2]}
                          {:user/countries-visited
                           [:country/alpha-2]}])
             :in $ ?user-email
             :where [?e :user/email ?user-email]]
           db user-email)
      (format-countries)))

;;(add-user conn {:user/email    "test@user.com"
;;                :user/password "somepass"})
;;
;;(find-user (d/db conn) "test@user.com")
;;
;;(:country/alpha-3 (find-country (d/db conn) "China"))

;;(add-to-countries :visited conn "test@user.com" "BLR")
;;(get-countries (d/db conn) "test@user.com")
;;(add-to-countries :to-visit conn "test@user.com" "BLR")
;;(get-countries (d/db conn) "test@user.com")
