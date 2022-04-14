(ns transform-data
  (:require
   [clojure.set :as set]))

(defn get-raw-data [] (->
                       (slurp "./resources/raw/data.edn")
                       (read-string)
                       (eval)))

(def old-keys [:name :country-code :alpha-2 :alpha-3])

(def new-keys {:name         :country/name
               :country-code :country/code
               :alpha-2      :country/alpha-2
               :alpha-3      :country/alpha-3})

(defn transform [country]
  (-> country
      (select-keys old-keys)
      (set/rename-keys new-keys)))

(defn wrap-with-template [data]
  (str {:visitera/data1 {:txes [(vec data)]}}))

(defn save-parsed []
  (spit "./resources/raw/parsed-data.edn"
        (binding [*print-namespace-maps* false]
          (->>
           (get-raw-data)
           (map transform)
           (wrap-with-template)))))

(defn -main [& args]
  (save-parsed))
