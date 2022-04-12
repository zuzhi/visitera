(ns visitera.routes.home
  (:require
   [visitera.layout :refer [register-page login-page home-page]]
   [clojure.java.io :as io]
   [visitera.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]
   [visitera.db.core :refer [conn find-user add-user find-country-by-alpha-3 get-countries]]
   [visitera.validation :refer [validate-register validate-login]]
   [datomic.api :as d]
   [buddy.hashers :as hs]))

(defn register-handler!
  ""
  [{:keys [params]}]
  (if-let [errors (validate-register params)]
    (-> (response/found "/register")
        (assoc :flash {:errors errors
                       :email (:email params)}))
    (if-not (add-user conn params)
      (-> (response/found "/register")
          (assoc :flash {:errors {:email "User with that email already exists"}
                         :email (:email params)}))
      (-> (response/found "/login")
          (assoc :flash {:message {:success "User is registered! You can log in now."}
                         :email (:email params)})))))

(defn password-valid?
  ""
  [user pass]
  (hs/check pass (:user/password user)))

(defn login-handler
  ""
  [{:keys [params session]}]
  (if-let [errors (validate-login params)]
    (-> (response/found "/login")
        (assoc :flash {:errors errors
                       :email (:email params)}))
    (let [user (find-user (d/db conn) (:email params))]
      (cond
        (not user)
        (-> (response/found "/login")
            (assoc :flash {:errors {:email "user with that email does not exist"}
                           :email (:email params)}))
        (and user
             (not (password-valid? user (:password params))))
        (-> (response/found "/login")
            (assoc :flash {:errors {:password "The password is wrong"}
                           :email (:email params)}))
        (and user
             (password-valid? user (:password params)))
        (let [updated-session (assoc session :identity (keyword (:email params)))]
          (-> (response/found "/")
              (assoc :session updated-session)))))))

(defn logout-handler
  ""
  [request]
  (-> (response/found "/login")
      (assoc :session {})))

(defn get-user-countries-handler
  ""
  [{:keys [session]}]
  (let [email (:identity session)]
    (-> (response/ok (pr-str (get-countries (d/db conn) email)))
        (response/header "Content-Type" "application/edn"))))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page
         :middleware [middleware/wrap-restricted]}]
   ["/db-test" {:get (fn [_]
                       (let [db (d/db conn)
                             country (find-country-by-alpha-3 db "CHN")]
                         (-> (response/ok (:country/name country))
                             (response/header "Content-Type" "text/plain; charset=utf-8"))))}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]
   ["/register" {:get register-page
                 :post register-handler!}]
   ["/login" {:get login-page
              :post login-handler}]
   ["/logout" {:get logout-handler}]
   ["/api"
    {:middleware [middleware/wrap-restricted]}
    ["/user-countries"
     ["" {:get get-user-countries-handler}]]]])
