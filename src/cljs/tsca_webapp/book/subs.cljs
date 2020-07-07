(ns tsca-webapp.book.subs
  (:require [re-frame.core :as re-frame]
            [tsca-webapp.common.subs-parts :as common]))

(def help-link-of-status "https://tezos.foundation/")

(re-frame/reg-sub
 ::books
 (fn [db _]
   (get-in db [:screen :books])))

(defn- parse-summary [{:keys [bookhash basicinfo]}]
  (-> basicinfo
      (assoc :bookhash bookhash)))

(re-frame/reg-sub
 ::books-summary
 :<- [::books]
 (fn [books]
   (->> books (map parse-summary))))

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



(re-frame/reg-sub
 ::routing-params
 (fn [db]
   (:routing-params db)))

(defn- parse-book-detail [{:keys [bookhash tmplversion basicinfo detailedinfo
                                  provider-detail]}]
  {:title (:title basicinfo)
   :synopsis (:synopsis basicinfo)
   :provider provider-detail
   :template-details {:contract-parameters (:parameters detailedinfo)
                      :contract-terms (:englishterms detailedinfo)
                      :caveats        (:caveats detailedinfo)}
   :bookhash bookhash
   :tmplversion tmplversion})

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
 ::book-charge
 :<- [::screen]
 (fn [screen _]
   (:charge screen)))

(re-frame/reg-sub
 ::book-status
 :<- [::screen]
 (fn [screen _]
   (:status screen)))

(re-frame/reg-sub
 ::book-contract-complexity
 :<- [::book-status]
 (fn [status _]
   {:value (:contract_complexity status)
    :url help-link-of-status}))

(re-frame/reg-sub
 ::book-certification-status
 :<- [::book-status]
 (fn [status _]
   {:value (:certification_status status)
    :url help-link-of-status}))

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
 (fn [{:keys [provider]} _]
   {:value (:displayname provider)
    :url (:website provider)}))

(re-frame/reg-sub
 ::expected-agreements
 :<- [::book-info]
 (fn [{:keys [template-details]} _]
   (merge (common/make-array-same-element template-details :contract-terms true)
          (common/make-array-same-element template-details :caveats true))))

(re-frame/reg-sub
 ::specifictions
 :<- [::screen]
 (fn [screen _]
   (get-in screen [:references :specifications])))

