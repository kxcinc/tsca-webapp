(ns tsca-webapp.book.events
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-fx
 ::open-list
 (fn-traced [{:keys [db]} _]
            {:db (-> db
                     (assoc :screen {:state :loading}))
             :aii {:commands [{:type :all-book}]
                   :success-id ::book-list-loaded
                   :error-id   ::book-load-failed}}))

(re-frame/reg-event-fx
 ::open
 (fn-traced [{:keys [db]} _]
            (let [bookhash (get-in db [:routing-params :bookhash])
                  commands (->> [:book-info :book-charge :book-status
                                 :book-references]
                                (map (fn [t] {:type t :bookhash bookhash})))]
              {:db (-> db
                       (assoc :screen {:state :loading}))
               :aii {:commands commands
                     :success-id ::book-info-loaded
                     :error-id   ::book-load-failed}})))

(re-frame/reg-event-db
 ::book-list-loaded
 (fn-traced [db [_ [info]]]
            (-> db
                (assoc :screen {:state :loaded
                                :books info}))))

(re-frame/reg-event-db
 ::book-info-loaded
 (fn-traced [db [_ [info charge status references ]]]
            (-> db
                (assoc :screen {:state :loaded
                                :info info
                                :charge charge
                                :status status
                                :references references
                                }))))

(re-frame/reg-event-db
 ::book-load-failed
 (fn-traced [db [_ ex]]
            (-> db
                (assoc :screen {:state :error}))))

