(ns tsca-webapp.book.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [tsca-webapp.book.subs :as subs]
   [tsca-webapp.common.view-parts :as common]))

(defn home-panel []
  [:div.docs-content
   [:h1 "TSC Agency"]
   [:div.divider]
   (for [{:keys [bookhash title synopsis]} @(re-frame/subscribe [::subs/books-summary])]
     [:div.p {:key bookhash}
      [:a {:href (str "#/" bookhash)}
       [:h3 title]]
      [:div synopsis]])])

(defn- term-block [title xs indexed? ratom path switch-label]
  (common/agreement-checkboxes
   [:h5 title] xs indexed? ratom path switch-label))

(defn- agreement-button [agreements agreed? subs-key label]
  (common/conditional-button agreements subs-key label #(reset! agreed? true)))

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

(defn- fee-block [{:keys [template-provider agency]}]
  [:div
   [:div (+ template-provider agency) " ꜩ in total per origination"]
   [:div [:span.text-small "( " template-provider "ꜩ + " agency "ꜩ )"]
    (info-icon [:div
                [:div template-provider " ꜩ payable to the Template Provider, "]
                [:div agency " ꜩ payable to the Agency"]])]])

(defn- book-header
  [{:keys [title synopsis basic-facts template-details bookhash tmplhash]}
   show-assistant-atom]
  (let [initial-agreements @(re-frame/subscribe [::subs/initial-agreements])
        agreements (reagent/atom initial-agreements)]
    (fn []
      [:div.docs-content
       [:h1 "Book: " title]
       [:div.p synopsis]

       [:div.card
        [:div.card-header [:h2 "Basic Facts"]]
        [:div.card-body
         [:div.columns 
          (for [[label v] [["Provider" (text-with-link-icon (:provider basic-facts))]
                           ["Contract Complexity" (text-with-link-icon (:contract-complexity basic-facts))]
                           ["Certification Status" (text-with-link-icon (:certification-status basic-facts))]
                           ["Template Fees" (fee-block (:template-fees basic-facts))]]]
            (seq [[:div.column.col-2.text-bold label]
                  [:div.column.col-4 v]]))]
         [:div.gap]
         [:div.columns
          [:div.column.col-xl-12.col-4.text-gray (str "book hash: " bookhash)]
          [:div.column.col-xl-12.col-4.text-gray (str "template hash: " tmplhash)]]]]
       [:div.gap]
       [:div.card
        [:div.card-header [:h2 "Contract Details"]]
        [:div.card-body
         [:h5 "Contract Parameters"]
         (->> (:contract-parameters template-details)
              (map-indexed (fn [i {:keys [param-name description]}]
                             [:div.columns {:key param-name}
                              [:div.column.col-xl-12.col-2.monospace (str " " (inc i) ". " param-name)]
                              [:div.column.col-xl-12.col-9 description]])))
         [:div.gap]
         (term-block "Contract Terms in English" (:contract-terms template-details) true
                     agreements :contract-terms "I understand")
         [:div.gap]
         (term-block "Caveats" (:caveats template-details) false
                     agreements :caveats "I understand")
         [:div.gap]
         [:h5 "Formal Specification"]
         [:div.flexbox (map-indexed (fn [index {:keys [title url]}]
                                      [:a {:key index :href url :target "_blank"} title])
                                    (:formal-specifications template-details))]]]
       [:div.gap]
       [:div.column
        [agreement-button agreements show-assistant-atom ::subs/expected-agreements
         ["Launch " [:i.icon.icon-upload]]]]])))

(defn- assistnt-modal [button-visible?]
  (when @button-visible?
    [:div.modal.active
     [:div.modal-container.modal-large
      [:div.modal-body
       [:iframe {:src "#/sr/"}]]
      [:div.modal-footer
       [:button.btn
        {:on-click #(reset! button-visible? false)}
        "close"]]]]))

(defn book-top []
  (let [show-modal? (reagent/atom nil)
        book @(re-frame/subscribe [::subs/book-info])]
    [:div
     [book-header book show-modal?]
     [assistnt-modal show-modal?]]))
