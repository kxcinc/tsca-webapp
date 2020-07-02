(ns tsca-webapp.spell-assistant.views
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [tsca-webapp.common.view-parts :as common]
   [tsca-webapp.spell-assistant.subs :as subs]
   [tsca-webapp.spell-assistant.events :as events]
   [tsca-webapp.task.events :as task-events]))

(defn- spell-runner [agreed?]
  (let [entering-text (reagent/atom "")
        entering-num  (reagent/atom "")]
    [:div.docs-content {:style {:display (when (not @agreed?) "none")}}
     [:div.panel
      [:div.panel-header
       [:h2 "Spell"]]
      [:div.panel-body
       [:div.form-horizontal
        [:div {:class "form-group"}
         [:div {:class "col-3 col-sm12"}
          [:label {:class "form-label" :for "input-text"} "Text Field"]]
         [:div {:class "col-9 col-sm12"}
          [common/input-with-trigger "input-text" :sending-spell entering-text]]]
        [:div {:class "form-group"}
         [:div {:class "col-3 col-sm12"}
          [:label {:class "form-label" :for "input-text"} "Number Field"]]
         [:div {:class "col-9 col-sm12"}
          [common/input-with-trigger "input-num" :sending-spell entering-num]]]]]
      [:div.panel-footer
       [:button {:class @(re-frame/subscribe [::subs/spell-button-class])
                 :on-click #(re-frame/dispatch
                             [::events/spell-out {:num @entering-num
                                                  :text @entering-text}])}
        "Proceed"]
       [:span " "]
       (when @(re-frame/subscribe [::subs/spell-processing?])
         [:button.btn {:on-click #(re-frame/dispatch [::task-events/cancel])} "cancel"])]]

     [:div.gap]

     [:div.panel
      [:div.panel-header
       [:h2 "Clerk"]]
      [:div.panel-body
       [:h3 "Estimation"]
       [:div.p (case @(re-frame/subscribe [::subs/estimate-state])
                 :not-yet  [:div "not calcurated"]
                 :processing [:div
                              [:span "calcurating... "]
                              [:button.btn {:on-click #(re-frame/dispatch [::task-events/cancel])}
                               "cancel"]]
                 [:div "It will cost: " [:span.text-large
                                         @(re-frame/subscribe [::subs/estimate-value])
                                         " ꜩ"]])]

       [:h3 "Operation"]

       [:div.p
        [:div.columns
         [:div.column
          [:div.panel
           [:div.panel-header
            [:h4 "if you have Ledger device"]]
           [:div.panel-body
            [:div.p
             [:div "make sure to put your ledger device in your PC"]
             [:button {:class @(re-frame/subscribe [::subs/ledger-button-class])}
              "Run"]]]]]
         [:div.divider-vert {:data-content "OR"}]
         [:div.column
          [:h4 "use CLI"]]]]]]]))

(defn- assistant-term [agreed?]
  (let [initial-agreements @(re-frame/subscribe [::subs/initial-agreements-assistant])
        agreements (reagent/atom initial-agreements)
        terms @(re-frame/subscribe [::subs/assistant-terms])]
    [:div {:style {:display (when @agreed? "none")}}
     (common/agreement-checkboxes
      [:div.text-center.h3 "Terms Of The Service"]
      terms false agreements :terms "agree")
     [:div.gap]
     [:div.text-center
      [common/conditional-button agreements ::subs/expected-agreements-assistant
       ["Use this template and create a Smart Contract now!" " "]
       #(reset! agreed? true)]]]))

(defn spell-assistant-top []
  (let [agreed? (reagent/atom false)]
    [:div
     [spell-runner agreed?]
     [assistant-term agreed?]]))
