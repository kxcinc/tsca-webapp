(ns tsca-webapp.routes.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
 ::routing-params
 (fn [db _]
   (:routing-params db)))

