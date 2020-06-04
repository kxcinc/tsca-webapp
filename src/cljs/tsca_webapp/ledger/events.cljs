(ns tsca-webapp.ledger.events
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-fx
 ::ledger-sending-apdu
 (fn-traced [coeffects [_ command]]
            (let [db (:db coeffects)]
              {:db (-> db
                       (assoc-in [:apdu :status] :sending)
                       (assoc-in [:apdu :error] nil))
               :apdu {:command command
                      :received-event-id ::ledger-received-success
                      :error-event-id    ::ledger-received-failure}})))

(re-frame/reg-event-db
 ::ledger-received-success
 (fn-traced [db [_ message]]
            (-> db
                (assoc-in [:apdu :status] :waiting)
                (assoc-in [:apdu :result] message))))

(re-frame/reg-event-db
 ::ledger-received-failure
 (fn-traced [db [_ ex]]
            (-> db
                (assoc-in [:apdu :status] :waiting)
                (assoc-in [:apdu :error] ex))))
