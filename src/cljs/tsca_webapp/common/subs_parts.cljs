(ns tsca-webapp.common.subs-parts
  (:require [re-frame.core :as re]))

(defn button-class [disabled?]
  (if disabled?
    "btn btn-primary disabled"
    "btn btn-primary"))

(re/reg-sub
 ::routing-params
 (fn [db]
   (:routing-params db)))

(re/reg-sub
 ::query-params
 :<- [::routing-params]
 (fn [params]
   (:query-params params)))
