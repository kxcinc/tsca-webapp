(ns tsca-webapp.ledger.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [tsca-webapp.common.view-parts :as common]
   [tsca-webapp.ledger.events :as events]
   [tsca-webapp.ledger.subs :as subs]))

(defn try-ledger []
  (let [entering-command (reagent/atom "")]
    [:div {:class "docs-content"}
     [:div {:class "form-horizontal"}
      [:div {:class "form-group"}
       [:div {:class "col-3 col-sm12"}
        [:label {:class "form-label" :for "input-op"} "Operation"]]
       [:div {:class "col-9 col-sm12"}
        [common/input-with-trigger "input-op" :ledger-sign-op entering-command]]]
      [:div {:class "form-group"}
       [:button {:class @(re-frame/subscribe [::subs/button-class])
                 :on-click #(re-frame/dispatch [::events/ledger-sign-op @entering-command])}
        "Sign"]]

      [:div {:class "form-group"}
       [:div {:class "col-3 col-sm12"}
        [:label {:class "form-label"} "Result"]]
       [:div {:class "col-9 col-sm12"}
        [:div @(re-frame/subscribe [::subs/apdu-result])]]]]]))
