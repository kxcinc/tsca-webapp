(ns tsca-webapp.book-app.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [tsca-webapp.routes.routes :as rt]
   [tsca-webapp.mock :as mock]
   [tsca-webapp.book-app.events :as events]
   [tsca-webapp.book-app.subs :as subs]))

(defn- show-modal [modal-atom]
  (let [url (rt/sa-proto0 {:label "withdraw"
                           :query-params {:for mock/target-spec-frozen}})]
    (reset! modal-atom {:show true :url url})
    (re-frame/dispatch [::events/change-iframe-url url])))

(defn- close-modal [modal-atom]
  (reset! modal-atom {:show false :url nil}))

(defn- main-page [modal-atom]
  [:div.card
     [:div.card-body
      (map-indexed (fn [i {:keys [title]}]
                     [:div.columns {:keys (str "p-" i)}
                      [:div.col-4 title]
                      [:div.col-4 "???"]])
                   @(re-frame/subscribe [::subs/parameters]))
      [:div.gap]
      [:button.btn
       {:on-click #(show-modal modal-atom)}
       @(re-frame/subscribe [::subs/button-label])]]])

(defn- assistnt-modal [modal-atom]
  (let [{:keys [show url]} @modal-atom]
    (when show
      [:div.modal.active
       [:div.modal-container.modal-large
        [:div.modal-body
         [:iframe {:id "assistant-modal"}]]
        [:div.modal-footer
         [:button.btn.btn-error
          {:on-click #(show-modal modal-atom)}
          "Start over"]
         " "
         [:button.btn
          {:on-click #(close-modal modal-atom)}
          "Close"]]]])))

(defn top []
  (let [modal-atom (reagent/atom {:show false :url nil})]
    [:div
     [main-page modal-atom]
     [assistnt-modal modal-atom]]))