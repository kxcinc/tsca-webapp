(ns tsca-webapp.book.subs
  (:require [re-frame.core :as re-frame]
            [tsca-webapp.common.subs-parts :as common]))

(def help-link-of-status "https://tezos.foundation/")

(re-frame/reg-sub
 ::books
 (fn [db _]
   (get-in db [:screen :books])))

(re-frame/reg-sub
 ::books-loading-state
 (fn [db _]
   (or (get-in db [:screen :state]) :loading)))

(re-frame/reg-sub
 ::books-loaded?
 :<- [::books-loading-state]
 (fn [state _]
   (= :loaded state)))

(re-frame/reg-sub
 ::loading-message
 :<- [::books-loading-state]
 (fn [state _]
   (case state
     :loading "loading ..."
     :error   "load failed."
     (str "unknown state: " state))))

(defn- parse-book-detail [{:keys [bookhash title synopsis provider-detail tmplhash
                                  contract_parameters_en contract_terms_en contract_caveats_en
                                  specifications]}]
  {:title title
   :synopsis synopsis
   :provider-detail provider-detail
   :template-details {:contract-parameters contract_parameters_en
                      :contract-terms contract_terms_en
                      :caveats        contract_caveats_en}
   :bookhash bookhash
   :tmplhash tmplhash
   :specifications specifications})

(re-frame/reg-sub
 ::screen
 (fn [db _]
   (-> db :screen)))

(re-frame/reg-sub
 ::book-info
 :<- [::screen]
 (fn [screen _]
   (-> screen :info parse-book-detail)))

(re-frame/reg-sub
 ::book-title
 :<- [::book-info]
 (fn [{:keys [title]} _]
   title))

(re-frame/reg-sub
 ::book-synopsis
 :<- [::book-info]
 (fn [{:keys [synopsis]} _]
   synopsis))

(re-frame/reg-sub
 ::bookhash
 :<- [::book-info]
 (fn [{:keys [bookhash]} _]
   bookhash))

(re-frame/reg-sub
 ::tmplhash
 :<- [::book-info]
 (fn [{:keys [tmplhash]} _]
   tmplhash))

(re-frame/reg-sub
 ::book-charge
 :<- [::screen]
 (fn [screen _]
   (:charges (:status screen))))

(re-frame/reg-sub
 ::book-status
 :<- [::screen]
 (fn [screen _]
   (:status screen)))

(re-frame/reg-sub
 ::book-contract-complexity
 :<- [::book-status]
 (fn [status _]
   {:value (get-in status [:review_results :contract_complexity])
    :url help-link-of-status}))

(re-frame/reg-sub
 ::book-certification-status
 :<- [::book-status]
 (fn [status _]
   {:value (get-in status [:review_results :certification_status])
    :url help-link-of-status}))

(re-frame/reg-sub
 ::contract-terms
 :<- [::book-info]
 (fn [{:keys [template-details]} _]
   (:contract-terms template-details)))

(re-frame/reg-sub
 ::contract-parameters
 :<- [::book-info]
 (fn [{:keys [template-details]} _]
   (:contract-parameters template-details)))

(re-frame/reg-sub
 ::caveats
 :<- [::book-info]
 (fn [{:keys [template-details]} _]
   (:caveats template-details)))

(defn- make-initial-array [xs]
  (mapv #(-> % :mandatory_consensus not) xs))

(re-frame/reg-sub
 ::initial-agreements
 :<- [::book-info]
 (fn [{:keys [template-details]} _]
   {:contract-terms (make-initial-array (:contract-terms template-details))
    :caveats (make-initial-array        (:caveats template-details))}))

(re-frame/reg-sub
 ::book-provider
 :<- [::book-info]
 (fn [{:keys [provider-detail]} _]
   {:value (:display_name provider-detail)
    :url (:website provider-detail)}))

(re-frame/reg-sub
 ::expected-agreements
 :<- [::contract-terms]
 :<- [::caveats]
 (fn [[contract_terms caveats] _]
   {:contract-terms (mapv (fn [_] true) contract_terms)
    :caveats        (mapv (fn [_] true) caveats)}))

(re-frame/reg-sub
 ::specifictions
 :<- [::book-info]
 (fn [{:keys [specifications]}]
   specifications))

