(ns tsca-webapp.common.view-parts
  (:require
   [reagent.core]                       ; need this due to override atom
   [re-frame.core :as re-frame]
   [clojure.string :as string]))

(defn input [state key-path]
  [:input {:class "form-input"
           :type "text"
           :value (get-in @state key-path)
           :on-change   #(swap! state assoc-in key-path (-> % .-target .-value))}])

(defn input-with-validate [state validation-state key-path validate-path invalid-message]
  (let [ok? (get-in @validation-state validate-path)]
    [:div
     [:input.form-input
      {:class (if ok? "is-success" "is-error")
       :type "text"
       :value (get-in @state key-path)
       :on-change   #(swap! state assoc-in key-path (-> % .-target .-value))}]
     [:div.form-input-hint (if ok? "　" invalid-message)]]))

(defn input-with-trigger [id trigger-event r-atom]
  (let [on-key-down #(when (= (.-which %) 13)
                       (let [entered (-> @r-atom string/trim)]
                         (when (seq entered)
                           (re-frame/dispatch [trigger-event entered]))))]
    (fn []
      [:input {:class "form-input"
               :type "text"
               :id id
               :on-change   #(reset! r-atom (-> % .-target .-value))
               :on-key-down on-key-down}])))

(defn checkbox [on-change label]
  [:div.form-switch
   [:input {:type "checkbox" :on-change on-change}]
   [:i.form-icon] label])

(defn labeled-checkbox [index key-prefix label switch-label on-change show-index?]
  [:div.columns {:key (str key-prefix "-" index)}
   [:div.column.col-1.text-right (if show-index? (str (inc index) ". ") "- ")]
   [:div.column.col-8 label]
   [:label.column.col-3
    (checkbox  #(on-change % index) switch-label)]])

(defn agreement-checkboxes [title labels show-index? ratom path switch-label]
  (let [toggle (fn [e index]
                 (swap! ratom assoc-in [path index] (-> e .-target .-checked)))]
    (cons title
          (interpose
           [:div.gap]
           (->> labels
                (map-indexed
                 (fn [i l]
                   (labeled-checkbox i path l switch-label toggle show-index?))))))))

(defn- conditional-button [target subs-key label on-click]
  (let [expected (re-frame/subscribe [subs-key])]
    (fn []
      [:button.btn.btn-lg.btn-primary
       {:class (when (not= @expected @target) "disabled")
        :on-click on-click}
       (seq label)])))
