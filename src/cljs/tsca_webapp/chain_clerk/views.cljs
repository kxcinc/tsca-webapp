(ns tsca-webapp.chain-clerk.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [tsca-webapp.chain-clerk.subs :as subs]
   [tsca-webapp.chain-clerk.events :as events]))

(defn- class-for-visibility [step-id]
  {:style (when (not @(re-frame/subscribe [::subs/step-visible? step-id]))
            {:display "none"})})

(defn- jump-next-or-back-buttons [next-label back-label]
  [:div
   [:button.btn.btn-primary
    {:on-click #(re-frame/dispatch [::events/go-to-next-step])}
    (seq next-label)]
   " "
   [:button.btn
    {:on-click #(re-frame/dispatch [::events/go-back-previous-step])}
    (seq back-label)]])

(defn clerk-top []
  [:div.columns
   [:div.column.col-4.col-xl-12
    [:h1 "What"] [:h2 "to do"]]
   [:div.column.col-8.col-xl-12
    [:div.card
     [:ul.step
      (for [{:keys [active? display]} @(re-frame/subscribe [::subs/step-indicator])]
        [:li.step-item {:key display
                        :class (when active? "active")} [:a display]])]
     [:div.card-body
      [:div (class-for-visibility :user-confirmation)
       [:h3 "Please enter your information"]
       [:form.form-horizontal
        [:div.form-group
         [:div.col-3.col-md-12
          [:label.form-label {:for "input-text"} "Name"]]
         [:div {:class "col-9 col-md-12"}
          [:input.form-input]]]
        [:div.form-group
         [:div {:class "col-3 col-md-12"}
          [:label.form-label {:for "input-text"} "e-mail"]]
         [:div {:class "col-9 col-md-12"}
          [:input.form-input]]]]
       [:div.gap]
       [jump-next-or-back-buttons [[:i.icon.icon-mail] " Send confirmation"] "Back"]]

      [:div (class-for-visibility :address-selection)
       [:h3 "Select a source address"]
       [:div.form-group
        [:label.form-radio
         [:input {:type "radio" :name "address" :checked true}]
         [:i.form-icon]
         "hoge a"]
        [:label.form-radio
         [:input {:type "radio" :name "address" :checked false}]
         [:i.form-icon]
         "hoge b"]]
       [:div.gap]
       [jump-next-or-back-buttons
        [[:i.icon.icon-arrow-right] " Next"] "Back"]]

      [:div (class-for-visibility :simulation)
       [:h3 "Simulating..."]
       [:div "it costs 15.32 ꜩ"]
       [:div.gap]
       [jump-next-or-back-buttons
        [[:i.icon.icon-arrow-right] " Next"] "Back"]]

      [:div (class-for-visibility :run)
       [:h3 "Let's do it!"]
       [:div "it costs 15.32 ꜩ"]
       [:div.gap]
       [jump-next-or-back-buttons [[:i.icon.icon-arrow-right] " Next"] ["Back"]]]]]]])
