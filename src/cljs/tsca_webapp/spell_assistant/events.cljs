(ns tsca-webapp.spell-assistant.events
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(defn- clear-clerk [db]
  (-> db
      (assoc-in [:estimate :value] nil)))

(re-frame/reg-event-fx
 ::spell-out
 (fn-traced [{:keys [db]} [_ params]]
            {:db (-> db
                     (assoc-in [:spell :status] :processing)
                     (clear-clerk))
             :spell {:params params
                     :done-event-id ::spell-out-done
                     :cancel-event-id ::spell-out-cancelled}}))

(re-frame/reg-event-fx
 ::spell-out-done
 (fn-traced [{:keys [db]} [_ result]]
            {:db (-> db
                     (assoc-in [:spell :status] :waiting)
                     (assoc-in [:spell :unsigned-operation] result))
             :dispatch [::estimate result]}))

(re-frame/reg-event-fx
 ::spell-out-cancelled
 (fn-traced [{:keys [db]} [_ result]]
            {:db (-> db
                     (assoc-in [:spell :status] :waiting)
                     (assoc-in [:spell :unsigned-operation] nil))}))

(re-frame/reg-event-fx
 ::estimate
 (fn-traced [{:keys [db]} [_ params]]
            {:db (-> db
                     (assoc-in [:estimate :status] :processing)
                     (assoc-in [:estimate :value] nil))
             :estimate {:params params
                        :done-event-id ::estimate-done
                        :cancel-event-id ::estimate-cancelled}}))

(re-frame/reg-event-fx
 ::estimate-done
 (fn-traced [{:keys [db]} [_ value]]
            {:db (-> db
                     (assoc-in [:estimate :status] :done)
                     (assoc-in [:estimate :value] value))}))

(re-frame/reg-event-fx
 ::estimate-cancelled
 (fn-traced [{:keys [db]} [_ value]]
            {:db (-> db
                     (assoc-in [:estimate :status] :waiting)
                     (assoc-in [:estimate :value] nil))}))
