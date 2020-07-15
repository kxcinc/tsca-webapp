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
                              :user-confirmation :doit
                              :doit :doit))))

(re-frame/reg-event-fx
 ::go-back-previous-step
 (fn-traced
  [cofx _]
  cofx))

;; pubkey

(re-frame/reg-event-fx
 ::start-ledger-pubkey
 (fn-traced [{:keys [db]} _]
            {:db (-> db
                     (assoc-in [:clerk :ledger :state :pubkey ] {:status :finding-ledger
                                                                 :source-address nil}))
             :ledger-ready? {:success-id ::ledger-connected-pubkey
                             :error-id   ::error-occured-pubkey}}))

(re-frame/reg-event-fx
 ::ledger-connected-pubkey
 (fn-traced [{:keys [db]} _]
            {:db (assoc-in db [:clerk :ledger :state :pubkey :status] :confirming)
             :ledger-pk {:success-id ::find-ledger-source-address
                         :error-id   ::error-occured-pubkey}}))

(re-frame/reg-event-fx
 ::find-ledger-source-address
 (fn-traced [{:keys [db]} [_ public-key]]
            (js/console.log "public key" public-key)
            {:db (assoc-in db [:clerk :ledger :state :pubkey :status] :finding-source-address)
             :aii {:commands [{:type :source-address
                               :public-key public-key}]
                   :success-id ::found-source-address
                   :error-id ::error-occured-pubkey}}))

(re-frame/reg-event-db
 ::error-occured-pubkey
 (fn-traced [db [_ ex]]
            (-> db
                (assoc-in [:clerk :ledger :state :pubkey :status] :error)
                (assoc-in [:clerk :ledger :state :pubkey :error] ex))))


(re-frame/reg-event-db
 ::found-source-address
 (fn-traced [db [_ [source-address]]]
            (-> db
                (assoc-in [:clerk :ledger :state :pubkey :status] :found)
                (assoc-in [:clerk :ledger :state :pubkey :source-address] source-address))))


;; simulate
(re-frame/reg-event-fx
 ::start-simulate
 (fn-traced [{:keys [db]} [_ form ops]]
            {:db (-> db
                     (assoc-in [:clerk :ledger :state :sim] {:status :loading})
                     (assoc-in [:clerk :form] form))
             :aii {:commands [{:type :simulate :ops ops}]
                   :success-id ::simulation-done
                   :error-id   ::simulation-error}}))

(re-frame/reg-event-db
 ::simulation-done
 (fn-traced [db [_ [result]]]
            (-> db
                (assoc-in [:clerk :ledger :state :sim] {:status :done
                                                        :result result}))))
(re-frame/reg-event-db
 ::simulation-error
 (fn-traced [db [_ message]]
            (js/console.error "simulation error" message)
            (-> db
                (assoc-in [:clerk :ledger :state :sim] {:status :error
                                                        :message message}))))

;; op
(re-frame/reg-event-fx
 ::start-ledger-op
 (fn-traced [{:keys [db]} _]
            {:db (-> db
                     (assoc-in [:clerk :ledger :state :op ] {:status :finding-ledger}))
             :ledger-ready? {
                             :success-id ::ledger-connecting-op
                             :error-id   ::error-occured-op}}))

(re-frame/reg-event-fx
 ::ledger-connecting-op
 (fn-traced [{:keys [db]} _]
            {:db (assoc-in db [:clerk :ledger :state :op :status] :confirming)
             :ledger-sign {:success-id ::ledger-signed
                           :error-id   ::error-occured-op}}))
(re-frame/reg-event-fx
 ::ledger-signed
 (fn-traced [{:keys [db]} _]
            {:db (assoc-in db [:clerk :ledger :state :op :status] :sending-op)
             :sleep {:return-value "ok"
                     :sec 2
                     :success-id ::done}}))

(re-frame/reg-event-db
 ::done
 (fn-traced [db [_ ex]]
            (-> db
                (assoc-in [:clerk :ledger :state :op :status] :done))))

(re-frame/reg-event-db
 ::error-occured-op
 (fn-traced [db [_ ex]]
            (-> db
                (assoc-in [:clerk :ledger :state :op :status] :error)
                (assoc-in [:clerk :ledger :state :op :error] ex))))
