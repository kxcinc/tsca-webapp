(ns tsca-webapp.routes.effects
  (:require
   [re-frame.core :as re-frame]
   [tsca-webapp.routes.routes :as routes]))

(re-frame/reg-fx
 :routing
 (fn [_]
   (routes/app-routes)))
