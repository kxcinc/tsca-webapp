(ns tsca-webapp.sa-proto.subs
  (:require [re-frame.core :as re]))

(re/reg-sub
 ::label
 (fn [db]
   (let [{:keys [label]} (:routing-params db)]
     label)))

