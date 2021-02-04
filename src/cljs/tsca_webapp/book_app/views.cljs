(ns tsca-webapp.book-app.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as r]
   [tsca-webapp.book-app.events :as events]
   [tsca-webapp.book-app.subs :as subs]))

(def ^const iframe-id "assistant-modal")

(defn- show-modal [modal-atom]
  (let [salabel "withdraw"]
    (reset! modal-atom {:show true})
    (r/dispatch [::events/display-spell-assistant salabel iframe-id])))

(defn- close-modal [modal-atom]
  (reset! modal-atom {:show false :url nil}))

(defn- main-page [modal-atom]
  [:div.card
     [:div.card-body
      (map-indexed (fn [i {:keys [title value]}]
                     [:div.columns {:key (str "p-" i)}
                      [:div.col-4 title]
                      [:div.col-4 value]])
                   @(r/subscribe [::subs/parameters]))
      [:div.gap]
      [:button.btn
       {:on-click #(show-modal modal-atom)}
       @(r/subscribe [::subs/button-label])]]])

(defn- assistnt-modal [modal-atom]
  (let [{:keys [show url]} @modal-atom]
    (when show
      [:div.modal.active
       [:div.modal-overlay]
       [:div.modal-container.modal-large
        [:div.modal-body
         [:iframe {:id iframe-id}]]
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
     (case @(r/subscribe [::subs/status])
       :loading [:h4 "Loading..."]
       :error   [:h4.text-error "loading ERROR!"]
       [main-page modal-atom])
     [assistnt-modal modal-atom]]))
