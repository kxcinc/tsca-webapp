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

(re/reg-sub
 ::ledger
 (fn [db _]
   (get-in db [:clerk :ledger])))

(re/reg-sub
 ::ledger-pubkey-state
 :<- [::ledger]
 (fn [ledger _]
   (get-in ledger [:state :pubkey])))

(re/reg-sub
 ::ledger-op-state
 :<- [::ledger]
 (fn [ledger _]
   (get-in ledger [:state :op])))

(re/reg-sub
 ::ledger-source-address
 :<- [::ledger-pubkey-state]
 (fn [state _]
   (:source-address state)))

(re/reg-sub
 ::ledger-pubkey-status
 :<- [::ledger-pubkey-state]
 (fn [state _]
   (:status state)))

(re/reg-sub
 ::ledger-pubkey-loading?
 :<- [::ledger-pubkey-status]
 (fn [state _]
   (#{:finding-ledger :loading :finding-source-address} state)))

(re/reg-sub
 ::ledger-pubkey-message
 :<- [::ledger-pubkey-status]
 (fn [status _]
   (case status
     :finding-ledger "Connect your Ledger and launch Tezos App ..."
     :loading "loading public key ..."
     :confirming "Click OK on your Ledger"
     :finding-source-address "loading source address ..."
     :found "Source Address Found!"
     :error "ERROR!"
     "(unknown state)")))

(re/reg-sub
 ::ledger-pubkey-error
 :<- [::ledger-pubkey-state]
 (fn [state _]
   (:error state)))

(defn- error-message [{:keys [type]}]
  (case type
     :time-out "time out"
     :busy "device busy"
     :denied-by-user "operation denied"
     (:transport-status-error :transport-error) ""
     ""))

(re/reg-sub
 ::ledger-pubkey-error-message
 :<- [::ledger-pubkey-error]
 (fn [err _]
   (error-message err)))

(re/reg-sub
 ::ledger-op-status
 :<- [::ledger-op-state]
 (fn [state _]
   (:status state)))

(re/reg-sub
 ::ledger-op-loading?
 :<- [::ledger-op-status]
 (fn [state _]
   (#{:finding-ledger :signing :sending-op} state)))

(re/reg-sub
 ::ledger-op-cancellable?
 :<- [::ledger-op-status]
 (fn [state _]
   (not=  state :done)))

(re/reg-sub
 ::ledger-op-message
 :<- [::ledger-op-status]
 (fn [status _]
   (case status
     :finding-ledger "Connect your Ledger and launch Tezos App..."
     :signing "signing ..."
     :confirming "Click OK on your Ledger"
     :sending-op "sending the operation  ..."
     :done "Your operation done successfully!"
     :error "ERROR!"
     "(unknown state)")))

(re/reg-sub
 ::ledger-op-error
 :<- [::ledger-op-state]
 (fn [state _]
   (:error state)))

(re/reg-sub
 ::ledger-op-error-message
 :<- [::ledger-op-error]
 (fn [err _]
   (error-message err)))
