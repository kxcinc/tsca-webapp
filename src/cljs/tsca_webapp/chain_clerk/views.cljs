(ns tsca-webapp.chain-clerk.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [tsca-webapp.common.view-parts :as common]
   [tsca-webapp.chain-clerk.subs :as subs]
   [tsca-webapp.chain-clerk.events :as events]
   [tsca-webapp.task.events :as task]))

(defn- class-for-visibility [step-id]
  {:style (when (not @(re-frame/subscribe [::subs/step-visible? step-id]))
            {:display "none"})})

(defn- error-block [message-key start-key on-cancel]
  [:div.empty
       [:div.text-large.empty-title.text-error "ERROR!"]
       [:div.empty-subtitle @(re-frame/subscribe [message-key])]
       [:div.empty-action
        [:div.btn.btn-warning.btn-lg
         {:on-click #(re-frame/dispatch [start-key])}
         "Retry"]
        " "
        [:button.btn.btn-link {:on-click on-cancel} "Cancel"]]])

(defn- ledger-pubkey-block [state]
  (when (get-in @state [:ledger-pubkey :show])
    (if @(re-frame/subscribe [::subs/ledger-pubkey-error])
      [error-block ::subs/ledger-pubkey-error-message ::events/start-ledger-pubkey
       #(swap! state assoc-in [:ledger-pubkey :show] false)]

      (let [source @(re-frame/subscribe [::subs/ledger-source-address])]
        [:div.empty
         [:div.empty-title @(re-frame/subscribe [::subs/ledger-pubkey-message])]
         (when source
           [:div.empty-subtitle "Your source address: " [:b source]])
         (if @(re-frame/subscribe [::subs/ledger-pubkey-loading?])
           [:div.empty-icon [:div.loading.loading-lg]])
         [:div.empty-action
          (when source
            [:button.btn.btn-success.btn-lg
             {:on-click (fn []
                          (swap! state #(-> %
                                            (assoc-in [:ledger-pubkey :show] false)
                                            (assoc-in [:entering :source-address] source))))}
             [:i.icon.icon-check]
             " Use This Address"])
          [:button.btn.btn-link
           {:on-click #(do (swap! state assoc-in [:ledger-pubkey :show] false)
                           (re-frame/dispatch [::task/cancel]))}
           "Cancel"]]]))))

(defn- ledger-op-block [state]
  (when (get-in @state [:ledger-op :show])
    (if @(re-frame/subscribe [::subs/ledger-op-error])
      [error-block ::subs/ledger-op-error-message ::events/start-ledger-op
       #(swap! state assoc-in [:ledger-op :show] false)]

      [:div.empty
       [:div.empty-title @(re-frame/subscribe [::subs/ledger-op-message])]
       (if @(re-frame/subscribe [::subs/ledger-op-loading?])
         [:div.empty-icon [:div.loading.loading-lg]])
       (when @(re-frame/subscribe [::subs/ledger-op-cancellable?])
         [:div.empty-action
          [:button.btn.btn-link
           {:on-click #(do (swap! state assoc-in [:ledger-op :show] false)
                           (re-frame/dispatch [::task/cancel]))}
           "Cancel"]])])))

(defn- fields-filled? [obj fields]
  (->> (map obj fields)
       (every? #(not (or (nil? %) (empty? %))))))

(defn input [state key-path]
  [:input {:class "form-input"
           :type "text"
           :value (get-in @state key-path)
           :on-change   #(swap! state assoc-in key-path (-> % .-target .-value))}])

(defn- proceed-button [state]
  [:div
   [:button.btn.btn-primary
    {:disabled (not (fields-filled? (get-in @state [:entering])
                                    [:name :e-mail :source-address]))
     :on-click #(re-frame/dispatch [::events/go-to-next-step])}
    [:i.icon.icon-arrow-right]
    " Proceed"]])

(defn- check-understand [state]
  [:label.form-switch
   [:input {:type "checkbox"
            :on-change #(swap! state update-in [:agreement] not)}]
   [:i.form-icon] "I understand"])

(defn- simulation-block [state]
  [:div
    [:h3 "Simulating..."]
    [:div "it costs 15.32 ꜩ"]
   [check-understand state]])

(defn- operation-block [state]
  [:div
   [:div.divider]
   [:div.columns
    [:div.column
     [:div.panel
      [:div.panel-header
       [:h4 "if you have Ledger device"]]
      [:div.panel-body
       [:button.btn
        {:on-click #(do
                      (swap! state assoc-in [:ledger-op :show] true)
                      (re-frame/dispatch [::task/cancel])
                      (re-frame/dispatch [::events/start-ledger-op]))}
        "Proceed with Ledger"]
       [:div.gap]
       [ledger-op-block state]]]]
    [:div.divider-vert {:data-content "OR"}]
    [:div.column
     [:h4 "use CLI"]]]])

(defn- doit-block [state]
  [:div
   [simulation-block state]
   (when (:agreement @state)
     [operation-block state])])

(defn clerk-top []
  (let [state (reagent/atom {:entering {}
                             :ledger-pubkey {:show false}
                             :ledger-op     {:show false}
                             :agreement false})]
    [:div.columns
     [:div.column.col-4.col-xl-12
      [:h1 "What"] [:h2 "to do"]]
     [:div.column.col-8.col-xl-12
      [:div.card
       [:div.card-body
        [:div (class-for-visibility :user-confirmation)
         [:h3 "Please enter your information"]
         [:form.form-horizontal
          [:div.form-group
           [:div.col-3.col-md-12
            [:label.form-label {:for "input-text"} "Name"]]
           [:div {:class "col-9 col-md-12"}
            [input state [:entering :name]]]]
          [:div.form-group
           [:div {:class "col-3 col-md-12"}
            [:label.form-label {:for "input-text"} "e-mail"]]
           [:div {:class "col-9 col-md-12"}
            [input state [:entering :e-mail]]]]

          [:div.divider]
          [:div.form-group
           [:div {:class "col-3 col-md-12"}
            [:label.form-label {:for "input-text"} "Source Address"]]
           [:div {:class "col-6 col-md-6"}
            [input state [:entering :source-address]]]
           [:div.text-right {:class "col-3 col-md-6"}
            [:div " or "
             [:button.btn
              {:on-click #(do
                            (swap! state assoc-in [:ledger-pubkey :show] true)
                            (re-frame/dispatch [::task/cancel])
                            (re-frame/dispatch [::events/start-ledger-pubkey]))}
              "Load from Ledger"]]]]
          [ledger-pubkey-block state :ledger-pubkey]]
         [:div.gap]
         [proceed-button state]]

        [:div (class-for-visibility :doit)
         [doit-block state]]]]]]))
