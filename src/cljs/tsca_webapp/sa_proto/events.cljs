(ns tsca-webapp.sa-proto.events
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-fx
 ::generate-verifier
 (fn-traced [{:keys [db]} _]
            (let [sahash (get-in db [:routing-params :label])]
              {:db (-> db
                       (assoc :screen {:state :verifier-loading}))
               :aii {:commands [{:type :spell-verifier :sahash sahash}]
                     :success-id ::verifier-ready
                     :error-id   ::verifier-loading-error}})))

(re-frame/reg-event-db
 ::verifier-ready
 (fn-traced [db [_ [verifier]]]
            (-> db
                (assoc :screen {:state :verifier-loaded
                                :verifier verifier}))))

(re-frame/reg-event-db
 ::verifier-loading-error
 (fn-traced [db [ex]]
            (-> db
                (assoc :screen {:state :verifier-loading-error
                                :message ex}))))

