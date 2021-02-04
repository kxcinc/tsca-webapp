(ns tsca-webapp.spell-assistant.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as r]
   [tsca-webapp.common.view-parts :as common]
   [tsca-webapp.spell-assistant.events :as events]
   [tsca-webapp.spell-assistant.subs :as subs]
   [clojure.string :as s]))

(defn- build-serializer [xs]
  (letfn [(to-snakecase [k] (s/replace (name k) "-" "_"))
          (to-js [obj] (-> (reduce-kv
                            #(assoc %1 (to-snakecase %2) %3)
                            {} obj)
                           clj->js))]
    (fn [obj]
      (-> (reduce (fn [m {:keys [field convert]}]
                    (let [v (get obj field)]
                      (assoc m field
                             (case convert
                               :number (cljs.reader/read-string v)
                               :comma-separated (s/split v #",")
                               v))))
                  {}
                  xs)
          to-js))))

(defn- build-validator [xs]
  (fn [obj]
    (reduce (fn [validated {:keys [field validate-by]}]
              (if validate-by
                (->> (get-in obj [:entering field])
                     validate-by
                     (assoc validated field))
                (assoc validated field true)))
            {}
            xs)))

(defn- everything-valid? [obj]
  (->> obj vals
       (every? identity)))

(defn- everything-valid-and-aii? [obj]
  (and (everything-valid? obj)
       (get-in obj [:aii-valid :valid?])))

(defn- proceed-button [state validation serializer]
  [:button.btn.btn-primary
   {:disabled (not (everything-valid-and-aii? @validation))
    :on-click #(r/dispatch [::events/proceed-to-chain-clerk
                                   (:entering @state)])}
   @(r/subscribe [::subs/proceed-button-label])])

(defn- aii-validate [serializer state validation]
  (swap! validation assoc :aii-valid {:valid? true :message nil}))

(defn- aii-error-display []
  (when @(r/subscribe [::subs/aii-invalid?])
    [:div
     [:div.gap]
     [:div.panel
      [:div.panel-body
       [:div.text-error @(r/subscribe [::subs/aii-error-message])]
       (map (fn [e] [:div e])
            @(r/subscribe [::subs/aii-error-hints]))]]]))

(defn- forms [xs]
  (let [validator (build-validator xs)
        validation (reagent/atom (validator {}))
        serializer (build-serializer xs)
        state (reagent/atom {})
        _ (add-watch state :entering
                     (fn [x state old new]
                       (aii-validate serializer state validation)
                       (swap! validation merge (validator new))))]
    [:div.form-horizontal
     [:h1 @(r/subscribe [::subs/title])]
     [:h5 @(r/subscribe [::subs/description])]
     [:div.gap]
     (for [{:keys [label field validate-by invalid-message datetime]} xs]
       [:div.form-group {:key field}
        [:div.col-3.col-md-12
         [:label.form-label {:for "input-text"} label]]
        [:div {:class "col-9 col-md-12"}
         (if validate-by
           [common/input-with-validate state validation
            [:entering field] [field]
            invalid-message datetime]
           [common/input state [:entering field]])]])
     [:div.gap]
     [proceed-button state validation serializer]
     [aii-error-display]]))

(defn main [agreed?]
  [:div {:style {:display (when (not @agreed?) "none")}}
   (forms @(r/subscribe [::subs/forms]))])

(defn body [agreed?]
  (case @(r/subscribe [::subs/verifier-state])
          :verifier-loading nil
          :verifier-loading-error [:h4 "unexpected error!"]
          [main agreed?]))

(defn- assistant-term [agreed?]
  (let [initial-agreements @(r/subscribe [::subs/initial-agreements-assistant])
        terms              @(r/subscribe [::subs/assistant-terms])
        agreements         (reagent/atom initial-agreements)]
    [:div {:style {:display (when @agreed? "none")}}
     [:div.text-center.h3 "Terms Of The Service"]
     (common/agreement-checkboxes terms false agreements :terms "agree")
     [:div.gap]
     [:div.text-center
      [common/conditional-button agreements ::subs/expected-agreements-assistant
       [:div "Use this template and create a Smart Contract now!" " "]
       #(reset! agreed? true)]]]))

(defn top []
  (let [agreed? (reagent/atom false)]
    [:div.docs-content
     [assistant-term agreed?]
     [body agreed?]]))
