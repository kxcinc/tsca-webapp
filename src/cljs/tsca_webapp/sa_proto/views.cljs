(ns tsca-webapp.sa-proto.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [tsca-webapp.common.view-parts :as common]
   [tsca-webapp.sa-proto.subs :as subs]
   [tsca-webapp.routes.events :as routes]
   [tsca-webapp.task.events :as task]))

(defn- to-json [obj]
  (-> (reduce-kv (fn [acc k v]
                   (let [key (clojure.string/replace (name k) "-" "_")]
                     (assoc acc key v))) {} obj)
      clj->js
      js/JSON.stringify))

(defn- build-serializer [xs]
  (fn [obj]
    (-> (reduce (fn [m {:keys [field convert]}]
                  (let [v (get obj field)]
                    (assoc m field
                           (case convert
                             :number (cljs.reader/read-string v)
                             v))))
                {}
                xs)
        to-json)))

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

(defn- info [state validation serializer]
  [:div.panel
   [:div.panel-body
    (let [st @state]
      [:pre
       (str (try (serializer (:entering st))
                 (catch :default e "error!")) "\n" (everything-valid? @validation))])
    [:div "networks:" (str @(re-frame/subscribe [::subs/networks]))]
    [:div "target spec:" @(re-frame/subscribe [::subs/target-spec])]]])

(defn- proceed-button [validation]
  [:button.btn.btn-primary
      {:disabled (not (everything-valid? @validation))
       :on-click #(re-frame/dispatch [::routes/set-active-panel :clerk-panel])}
   "Proceed"])

(defn- forms [xs]
  (let [validator (build-validator xs)
        validation (reagent/atom (validator {}))
        state (doto (reagent/atom {})
                (add-watch :entering (fn [x state old new]
                                       (reset! validation (validator new)))))]
    [:div.form-horizontal
     (for [{:keys [label field validate-by invalid-message]} xs]
       [:div.form-group {:key field}
        [:div.col-3.col-md-12
         [:label.form-label {:for "input-text"} label]]
        [:div {:class "col-9 col-md-12"}
         (if validate-by
           [common/input-with-validate state validation
            [:entering field] [field]
            invalid-message]
           [common/input state [:entering field]])]])
     [:div.gap]
     [proceed-button validation]
     [:div.gap]
     [info state validation (build-serializer xs)]]))

(defn- not-empty? [str]
  (not (empty? str)))

(defn- positive-number? [str]
  (boolean (and str (re-matches #"\d+(\.\d+)?" str))))

(defn- iso8601? [str]
  (boolean (and str (re-matches #"^([\+-]?\d{4}(?!\d{2}\b))((-?)((0[1-9]|1[0-2])(\3([12]\d|0[1-9]|3[01]))?|W([0-4]\d|5[0-2])(-?[1-7])?|(00[1-9]|0[1-9]\d|[12]\d{2}|3([0-5]\d|6[1-6])))([T\s]((([01]\d|2[0-3])((:?)[0-5]\d)?|24\:?00)([\.,]\d+(?!:))?)?(\17[0-5]\d([\.,]\d+)?)?([zZ]|([\+-])([01]\d|2[0-3]):?([0-5]\d)?)?)?)?$" str))))

(defn- withdraw []
  [{:label "Amount"      :field :amount       :validate-by positive-number? :convert :number
    :invalid-message "should be positive number"}
   {:label "Beneficiary" :field :beneficiary  :validate-by not-empty?
    :invalid-message "required"}])

(defn- genesis []
  [{:label "Fund Owners" :field :fund-owners :validate-by not-empty?
    :invalid-message "required"}
   {:label "Fund Amount" :field :fund-ammount :validate-by positive-number? :convert :number
    :invalid-message "positive number ony"}
   {:label "Unfrozen till" :field :unfrozen :validate-by iso8601?
    :invalid-message "ISO8601 format (e.g. 2020-07-02T00:00:00+09 )"}])

(defn main [agreed?]
  [:div {:style {:display (when (not @agreed?) "none")}}
   (case @(re-frame/subscribe [::subs/label])
     "withdraw" (forms (withdraw))
     "genesis" (forms (genesis))
     [:div "unknown label: " @(re-frame/subscribe [::subs/label])])])

(defn- assistant-term [agreed?]
  (let [initial-agreements @(re-frame/subscribe [::subs/initial-agreements-assistant])
        agreements (reagent/atom initial-agreements)
        terms @(re-frame/subscribe [::subs/assistant-terms])]
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
     [main agreed?]
     [assistant-term agreed?]]))
