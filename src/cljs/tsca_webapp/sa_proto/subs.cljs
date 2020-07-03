(ns tsca-webapp.sa-proto.subs
  (:require [re-frame.core :as re]
            [clojure.string :as s]))

(re/reg-sub
 ::routing-params
 (fn [db]
   (:routing-params db)))

(re/reg-sub
 ::label
 :<- [::routing-params]
 (fn [params]
   (:label params)))

(re/reg-sub
 ::target-spec
 :<- [::routing-params]
 (fn [params]
   (get-in params [:query-params :for])))

(re/reg-sub
 ::networks
 :<- [::routing-params]
 (fn [params]
   (some-> (get-in params [:query-params :networks])
           (s/split ","))))




