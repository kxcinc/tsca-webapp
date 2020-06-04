(ns tsca-webapp.core
  (:require
   [reagent.core :as reagent]
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [tsca-webapp.routes.events :as events]
   [tsca-webapp.routes.routes :as routes]
   [tsca-webapp.routes.views :as views]
   [tsca-webapp.config :as config]
   [tsca-webapp.effects]))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (routes/app-routes)
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
