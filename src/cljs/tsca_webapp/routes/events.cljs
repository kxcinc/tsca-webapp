(ns tsca-webapp.routes.events
  (:require
   [re-frame.core :as re-frame]
   [tsca-webapp.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))


(re-frame/reg-event-fx
 ::initialize-db
 (fn-traced [_ _]
            {:db (-> db/default-db
                     (assoc :aii {:state :loading}))
             :aii-initialize {:success-id ::aii-ready
                              :error-id   ::aii-loading-error}}))

(re-frame/reg-event-fx
 ::aii-ready
 (fn-traced [{:keys [db]} _]
            {:db (dissoc db :aii)
             :routing {}}))

(re-frame/reg-event-db
 ::aii-loading-error
 (fn-traced [db _]
            (assoc db :aii {:state :error})))

(re-frame/reg-event-fx
 ::set-active-panel
 (fn-traced [{:keys [db]} [_ active-panel params initialize-event]]
            (let [cofx {:db (assoc db :active-panel active-panel
                                   :routing-params params)}]
              (if initialize-event
                (assoc cofx :dispatch initialize-event)
                cofx))))

