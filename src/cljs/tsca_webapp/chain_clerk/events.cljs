(ns tsca-webapp.chain-clerk.events
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [tsca-webapp.chain-clerk.effects :as effects]))

(defn- move-step [{:keys [db]} tag determine-next]
  (let [current-step (get-in db [:clerk :current-step])
        next-step (determine-next current-step)]
    {:db (assoc-in db [:clerk :current-step] next-step)
     ::effects/on-move {:tag tag :step next-step}}))

(re-frame/reg-event-fx
 ::go-to-next-step
 (fn-traced
  [cofx _]
  (move-step cofx :forward #(case %
                              :user-confirmation :address-selection
                              :address-selection :simulation
                              :simulation        :run
                              :run :run))))

(re-frame/reg-event-fx
 ::go-back-previous-step
 (fn-traced
  [cofx _]
  (move-step cofx :backward #(case %
                               :user-confirmation :user-confirmation
                               :address-selection :user-confirmation
                               :simulation        :address-selection
                               :run :simulation))))
