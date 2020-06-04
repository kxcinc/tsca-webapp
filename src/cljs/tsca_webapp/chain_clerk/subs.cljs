(ns tsca-webapp.chain-clerk.subs
  (:require [re-frame.core :as re]))

(re/reg-sub
 ::clerk
 (fn [db]
   (:clerk db)))

(re/reg-sub
 ::current-step

 :<- [::clerk]
 (fn [clerk]
   (:current-step clerk)))

(re/reg-sub
 ::step-indicator
 :<- [::current-step]

 (fn [current-step]
   (let [steps [:user-confirmation :address-selection :simulation :run]]
     (->> steps
          (map-indexed
           (fn [i step]
             {:active? (= current-step step) :display (str "Step " (inc i))}))))))

(re/reg-sub
 ::step-visible?
 :<- [::current-step]
 (fn [current-step [_ step-id]]
   (= current-step step-id)))


