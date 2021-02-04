(ns tsca-webapp.spell-assistant.events
  (:require
   [re-frame.core :as re-frame]
   [cljs.core.match :refer-macros [match]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [tsca-webapp.task.events :as task]))

(re-frame/reg-event-fx
 ::generate-verifier
 (fn-traced [{:keys [db]} _]
            (let [label (get-in db [:routing-params :label])
                  tmplhash (get-in db [:routing-params :tmplhash])]
              {:db (-> db
                       (assoc :screen {:state :verifier-loading}))
               :aii {:commands [{:type :spell-verifier
                                 :label label
                                 :tmplhash tmplhash}]
                     :success-id ::verifier-ready
                     :error-id   ::verifier-loading-error}})))

(re-frame/reg-event-db
 ::verifier-ready
 (fn-traced [db [_ [{:keys [verifier tmplversion]}]]]
            (-> db
                (assoc :screen {:state :verifier-loaded
                                :verifier verifier
                                :tmplversion tmplversion}))))

(re-frame/reg-event-db
 ::verifier-loading-error
 (fn-traced [db [ex]]
            (-> db
                (assoc :screen {:state :verifier-loading-error
                                :message ex}))))
(re-frame/reg-event-fx
 ::proceed-to-chain-clerk
 (fn-traced [{:keys [db]} [_ form]]
            (let [salabel (get-in db [:screen :verifier :salabel])
                  tmplversion (get-in db [:screen :tmplversion])]
              {:db db
               :aii {:commands [{:type :verify-spell-params
                                 :salabel salabel
                                 :tmplversion tmplversion
                                 :fieldValues form}]
                     :success-id ::parameter-validated
                     :error-id   ::invalid-spell}})))

(re-frame/reg-event-fx
 ::parameter-validated
 (fn-traced [{:keys [db]} [_ [spell]]]
            (let [tmplhash (get-in db [:routing-params :tmplhash])
                  query-params (get-in db [:routing-params :query-params])
                  params {:query-params (assoc query-params
                                               :spell spell)
                          :tmplhash     tmplhash}]
              {:db db
               :dispatch [:set-active-panel :clerk-panel params]})))

(re-frame/reg-event-db
 ::invalid-spell
 (fn-traced [db [_ error]]
            (-> db
                (assoc-in [:screen :aii-error] error))))

