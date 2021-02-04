(ns tsca-webapp.spell-assistant.subs
  (:require [re-frame.core :as re]
            [cljs.core.match :refer-macros [match]]
            [tsca-webapp.mock :as mock]
            [tsca-webapp.common.subs-parts :as common]
            [clojure.string :as s]))

(re/reg-sub
 ::label
 :<- [::common/routing-params]
 (fn [params]
   (:label params)))

(def assistant-terms
  ["I have read through carefully all the materials above and completely understand the features, functions, and associated caveats of the described contract template. "
   "I understand and acknowledge that this service is provided as a technical demonstration and the service providers— including the TSCA team and the provider of this contract template—disclaims all warranties with regards to this service including all implied warranties of merchantability and fitness. in no events shall the providers be liable for any special, direct, indirect, or consequential damages or any damages whatsoever resulting from loss of use, data or profits, blah, blah, blah (legal language to be determined)"
   "I understand and acknowledge that the TSCA team strongly advice against using this service OUTSIDE the Carthagenet test network of the Tezos blockchain. Any loss of tokens of any kind is of the sole responsibility of myself and in no event the TSCA team or the provider of the contract template shall be held of liability of any kind."])

(re/reg-sub
 ::assistant-terms
 (fn [db]
   assistant-terms))

(re/reg-sub
 ::initial-agreements-assistant
 :<- [::assistant-terms]
 (fn [terms _]
   {:terms (mapv (fn [_] false) terms)}))

(re/reg-sub
 ::expected-agreements-assistant
 :<- [::assistant-terms]
 (fn [terms _]
   {:terms (mapv (fn [_] true) terms)}))

(re/reg-sub
 ::screen
 (fn [db]
   (:screen db)))

(re/reg-sub
 ::verifier-state
 :<- [::screen]
 (fn [screen]
   :verifier-loaded))
(re/reg-sub
 ::verifier
 :<- [::screen]
 (fn [screen]
   (:verifier screen)))

(re/reg-sub
 ::title
 :<- [::verifier]
 (fn [{:keys [form_title]}]
   form_title))

(re/reg-sub
 ::description
 :<- [::verifier]
 (fn [{:keys [form_desc]}]
   form_desc))

(re/reg-sub
 ::proceed-button-label
 :<- [::verifier]
 (fn [{:keys [form_major_button]}]
   (first form_major_button)))

(re/reg-sub
 ::spell-builder
 :<- [::label]
 (fn [label]
   (case label
     "withdraw" (fn [o]
                  (let [{:strs [amount beneficiary]} (js->clj o)]
                   (str "(frozen0.withdraw "
                         amount "tz "
                         beneficiary
                        ")")))
     "genesis" (fn [o]
                 (let [{:strs [initbal unfrozen owners]} (js->clj o)]
                   (str "(frozen0.gen "
                        initbal "tz "
                        "(" (s/join " " owners) ") "
                        unfrozen
                        ")")))
     "unknown")
   ))

(defn- not-empty? [str]
  (not (empty? str)))

(defn- positive-number? [str]
  (boolean (and str (re-matches #"\d+(\.\d+)?" str))))

(defn- iso8601? [str]
  (boolean (and str (re-matches #"^([\+-]?\d{4}(?!\d{2}\b))((-?)((0[1-9]|1[0-2])(\3([12]\d|0[1-9]|3[01]))?|W([0-4]\d|5[0-2])(-?[1-7])?|(00[1-9]|0[1-9]\d|[12]\d{2}|3([0-5]\d|6[1-6])))([T\s]((([01]\d|2[0-3])((:?)[0-5]\d)?|24\:?00)([\.,]\d+(?!:))?)?(\17[0-5]\d([\.,]\d+)?)?([zZ]|([\+-])([01]\d|2[0-3]):?([0-5]\d)?)?)?)?$" str))))

(defn- determine-params [typ]
  (match [typ]
         [["Atom" ["Amount"]]] {:validate-by positive-number?}
         [["Atom" ["Timestamp"]]] {:validate-by iso8601?
                                   :datetime true}
         [["Atom" ["Tzaddress"]]] {:validate-by not-empty?}
         [["List" ["Tzaddress"]]] {:validate-by not-empty?
                                   :convert :comma-separated}))

(defn- parse-fields-info [xs]
  (for [{:keys [code name desc typ requirement_desc]} xs]
    (merge {:label name :field code
            :desc desc
            :invalid-message requirement_desc
            :validate-by not-empty?}
           (determine-params typ))))

(re/reg-sub
 ::forms
 :<- [::verifier]
 (fn [{:keys [form_fields]}]
   (parse-fields-info form_fields)))

(re/reg-sub
 ::aii-error
 :<- [::screen]
 (fn [{:keys [aii-error]}]
   aii-error))

(re/reg-sub
 ::aii-invalid?
 :<- [::aii-error]
 (fn [x]
   (boolean x)))

(re/reg-sub
 ::aii-error-message
 :<- [::aii-error]
 (fn [{:keys [message]}]
   message))

(defn- name-by-code [forms code]
  (or (some->> forms
               (filter #(= (:code %) code))
               first
               :name)
      code))

(re/reg-sub
 ::aii-error-hints
 :<- [::aii-error]
 :<- [::verifier]
 (fn [[{:keys [hints]} {forms :form_fields}]]
   (map (fn [[code desc]]
          (str (name-by-code forms code)
               " - "
               desc))
        hints)))

