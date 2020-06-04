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
        [:label {:class "form-label" :for "input-apdu"} "APDU"]]
       [:div {:class "col-9 col-sm12"}
        [common/input "input-apdu" :ledger-sending-apdu entering-command]]]
      [:div {:class "form-group"}
       [:button {:class @(re-frame/subscribe [::subs/apdu-sending-button-class])
                 :on-click #(re-frame/dispatch [::events/ledger-sending-apdu @entering-command])}
        "Send"]]

      [:div {:class "form-group"}
       [:div {:class "col-3 col-sm12"}
        [:label {:class "form-label"} "Result"]]
       [:div {:class "col-9 col-sm12"}
        [:div @(re-frame/subscribe [::subs/apdu-result])]]]]]))
