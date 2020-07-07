(ns tsca-webapp.book.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [tsca-webapp.book.subs :as subs]
   [tsca-webapp.common.view-parts :as common]))

(defn- show-book-list []
  [:div
   (for [{:keys [bookhash title synopsis]} @(re-frame/subscribe [::subs/books-summary])]
     [:div.p {:key bookhash}
      [:a {:href (str "#/" bookhash)}
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
   show-assistant-atom]
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
          (for [[label v] [["Provider" (text-with-link-icon @(re-frame/subscribe [::subs/book-provider]))]
                           ["Contract Complexity" (text-with-link-icon @(re-frame/subscribe [::subs/book-contract-complexity]))]
                           ["Certification Status" (text-with-link-icon @(re-frame/subscribe [::subs/book-certification-status]))]
                           ["Template Fees" (fee-block charge)]]]
            (seq [[:div.column.col-2.text-bold label]
                  [:div.column.col-4 v]]))]
         [:div.gap]
         [:div.columns
          [:div.column.col-xl-12.col-4.text-gray (str "book hash: " bookhash)]
          [:div.column.col-xl-12.col-4.text-gray (str "template version: " tmplversion)]]]]
       [:div.gap]
       [:div.card
        [:div.card-header [:h2 "Contract Details"]]
        [:div.card-body
         [:h5 "Contract Parameters"]
         (->> (:contract-parameters template-details)
              (map-indexed (fn [i {:keys [ident desc]}]
                             [:div.columns {:key ident}
                              [:div.column.col-xl-12.col-2.monospace (str " " (inc i) ". " ident)]
                              [:div.column.col-xl-12.col-9 desc]])))
         [:div.gap]
         (term-block "Contract Terms in English" (:contract-terms template-details) true
                     agreements :contract-terms "I understand")
         [:div.gap]
         (term-block "Caveats" (:caveats template-details) false
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
        loaded      (re-frame/subscribe [::subs/books-loaded?])
        book        (re-frame/subscribe [::subs/book-info])]
    (if @loaded
      [:div
       [book-header @book show-modal?]
       [assistnt-modal show-modal?]]
      [:h4 @(re-frame/subscribe [::subs/loading-message])])))

;; {:bookhash "MOCK_bookhash_proto0_funny",
;;  :tmplversion "MOCK_tmplversion_proto0_funny",
;;  :spellAssistants {:genesis "MOCK_sahash_proto0_funny_genesis"
;;                    , :iamfunny
;;                    "MOCK_sahash_proto0_funny_iamfunny"}
;;  , :bookapp "MOCK_bahash_proto0_funny0",
;;  :provider "prvd_proto0_tsca"
;;  , :basicinfo {:title "Book of Funny"
;;   , :synopsis
;;   "Book of Funny is a demo-purpose contract template that creates a contract that sends back to the invoker half of its current balance every time it is invoked. It could be invoked by anyone so basically the originator is likely to be losing tokens. Do not use this template on networks with real tokens."},
;;  :detailedinfo {:parameters [{:ident "initial_balance",
;;                               :desc "the initial balance of the contract to be originated"}]
;;                 :englishterms [{:contents "Any implicit account could submit a transaction to the blockchain which results in an invocation to the originated contract. When such an invocation being successful, the originated contract will transfer half of its current balance back to the implicit account who submitted the transaction."
;;                                 , :mandatory_consensus true}],
;;                 :caveats [{:contents "DO NOT USE ON THE MAINNET: contracts originated from this template gives away its initial balance to anyone who requests, and is created solely for the purpose of demonstration. Therefore it is strongly advised against using this template on chains that doesn’t not have free tokens otherwise, especially the Tezos Mainnet."
;;                            , :mandatory_consensus true}]}}
