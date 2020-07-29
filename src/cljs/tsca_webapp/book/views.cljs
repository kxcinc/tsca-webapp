(ns tsca-webapp.book.views
  (:require
   [reagent.core :as reagent]
   [tsca-webapp.mock :as mock]
   [clojure.string :as s]
   [re-frame.core :as re-frame]
   [tsca-webapp.book.subs :as subs]
   [tsca-webapp.routes.events :as routes]
   [tsca-webapp.book.events :as events]
   [tsca-webapp.routes.routes :as rt]
   [tsca-webapp.common.view-parts :as common]))

(defn- show-book-list []
  [:div
   (for [{:keys [bookhash title synopsis]} @(re-frame/subscribe [::subs/books-summary])]
     [:div.p {:key bookhash}
      [:a.c-hand {:on-click #(re-frame/dispatch [::routes/set-active-panel :book-top
                                          {:bookhash bookhash}])}
       [:h3 title]]
      [:div synopsis]])])

(defn home-panel []
  (let [loaded (re-frame/subscribe [::subs/books-loaded?])]
    [:div.docs-content
     [:h1 "TSC Agency"]
     [:div.divider]
     (if @loaded
       [show-book-list]
       [:h4 @(re-frame/subscribe [::subs/loading-message])])]))

(defn- term-block [xs indexed? ratom path switch-label]
  (common/agreement-checkboxes xs indexed? ratom path switch-label))


(defn- show-modal [modal-atom]
  (let [url (rt/sa-proto0 {:label "genesis"
                           :query-params {:for mock/target-spec-frozen}})]
    (reset! modal-atom {:show true :url url})
    (re-frame/dispatch [::events/change-iframe-url url])))

(defn- close-modal [modal-atom]
  (reset! modal-atom {:show false :url nil}))

(defn- agreement-button [agreements modal-atom subs-key label]
  (common/conditional-button agreements subs-key label #(show-modal modal-atom)))

(defn- info-icon [popover]
  [:div.popover.popover-bottom
   [:span.bg-primary.shape.s-circle.text-center.text-small.text-bold "i"]
   [:div.popover-container.popover-large
    [:div.card
     [:div.card-body popover]]]])

(defn- link-icon [link-url]
  [:a {:href link-url :target "_blank"} [:i.icon.icon-link]])

(defn- text-with-link-icon [{:keys [value url]}]
  [:div value " " (link-icon url)])

(defn- fee-block [{:keys [provider agency]}]
  [:div
   [:div (+ provider agency) " ꜩ in total per origination"]
   [:div [:span.text-small "( " provider "ꜩ + " agency "ꜩ )"]
    (info-icon [:div
                [:div provider " ꜩ payable to the Template Provider, "]
                [:div agency " ꜩ payable to the Agency"]])]])

(defn- term-pair [key-prefix index label]
  [:div.columns {:key (str key-prefix "-" index)}
   [:div.column.col-1.text-right "-"]
   [:div.column.col-11 label]])

(defn- book-header
  [{:keys [title synopsis basic-facts template-details bookhash tmplversion]}
   modal-atom]
  (let [charge @(re-frame/subscribe [::subs/book-charge])
        initial-agreements @(re-frame/subscribe [::subs/initial-agreements])
        agreements (reagent/atom initial-agreements)]
    (fn []
      [:div.docs-content
       [:h1 "Book: " title]
       [:div.p synopsis]

       [:div.card
        [:div.card-header [:h2 "Basic Facts"]]
        [:div.card-body
         [:div.columns
          [:div.column.col-2.text-bold "Provider"] [:div.column.col-4 (text-with-link-icon @(re-frame/subscribe [::subs/book-provider]))]
          [:div.column.col-2.text-bold "Contract Complexity"] [:div.column.col-4 (text-with-link-icon @(re-frame/subscribe [::subs/book-contract-complexity]))]
          [:div.column.col-2.text-bold "Certification Status"] [:div.column.col-4 (text-with-link-icon @(re-frame/subscribe [::subs/book-certification-status]))]
          [:div.column.col-2.text-bold "Template Fees"] [:div.column.col-4 (fee-block charge)]]
         [:div.gap]
         [:div.columns
          [:div.column.col-xl-12.col-6.text-gray (str "book hash: " bookhash)]
          [:div.column.col-xl-12.col-6.text-gray (str "template version: " tmplversion)]]]]
       [:div.gap]
       [:div.card
        [:div.card-header [:h2 "Contract Details"]]
        [:div.card-body
         [:h5 "Contract Parameters"]
         (->> (:contract-parameters template-details)
              (map-indexed (fn [i {:keys [ident desc]}]
                             [:div.columns {:key ident}
                              [:b.column.col-xl-12.col-2.monospace (str " " (inc i) ". " ident)]
                              [:div.column.col-xl-12.col-9 desc]])))
         [:div.gap]
         [:h5 "Contract Terms in English"]
         (term-block (:contract-terms template-details) true
                     agreements :contract-terms "I understand")
         [:div.gap]
         [:h5 "Caveats"]
         (term-block (:caveats template-details) false
                     agreements :caveats "I understand")
         [:div.gap]
         [:h5 "Formal Specification"]
         [:div
          (map-indexed (fn [index {:keys [title synopsis link]}]
                         (term-pair "specification" index
                                    [:div [:b [:a {:href link :target "_blank"} title]]
                                     " "
                                     synopsis]))
                       @(re-frame/subscribe [::subs/specifictions]))]]]
       [:div.gap]
       [:div.column
        [agreement-button agreements modal-atom ::subs/expected-agreements
         [:div "Launch " [:i.icon.icon-upload]]]]])))

(defn- assistnt-modal [modal-atom]
  (let [{:keys [show url]} @modal-atom]
    (when show
      [:div.modal.active
       [:div.modal-overlay]
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

(defn book-top []
  (let [modal (reagent/atom {:show false :url nil})
        loaded      (re-frame/subscribe [::subs/books-loaded?])
        book        (re-frame/subscribe [::subs/book-info])]
    (if @loaded
      [:div
       [book-header @book modal]
       [assistnt-modal modal]]
      [:h4 @(re-frame/subscribe [::subs/loading-message])])))
