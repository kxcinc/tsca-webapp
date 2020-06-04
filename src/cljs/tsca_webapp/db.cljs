(ns tsca-webapp.db)

(def default-db
  {:name "re-frame"
   :apdu {:status :waiting
          :result nil}
   :clerk {:current-step :user-confirmation}})
