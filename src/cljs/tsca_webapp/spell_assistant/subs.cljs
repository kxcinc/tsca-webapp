(ns tsca-webapp.spell-assistant.subs
  (:require [re-frame.core :as re-frame]
            [tsca-webapp.common.subs-parts :as common]))

(re-frame/reg-sub
 ::spell
 (fn [db _]
   (:spell db)))

(re-frame/reg-sub
 ::spell-status
 :<- [::spell]
 (fn [spell _]
   (:status spell)))

(re-frame/reg-sub
 ::spell-processing?
 :<- [::spell-status]
 (fn [status _]
   (= status :processing)))

(re-frame/reg-sub
 ::spell-button-class
 :<- [::spell-processing?]
 (fn [processing? _]
   (common/button-class processing?)))

(def assistant-terms
  ["I have read through carefully all the materials above and completely understand the features, functions, and associated caveats of the described contract template. "
   "I understand and acknowledge that this service is provided as a technical demonstration and the service providers— including the TSCA team and the provider of this contract template—disclaims all warranties with regards to this service including all implied warranties of merchantability and fitness. in no events shall the providers be liable for any special, direct, indirect, or consequential damages or any damages whatsoever resulting from loss of use, data or profits, blah, blah, blah (legal language to be determined)"
   "I understand and acknowledge that the TSCA team strongly advice against using this service OUTSIDE the Carthagenet test network of the Tezos blockchain. Any loss of tokens of any kind is of the sole responsibility of myself and in no event the TSCA team or the provider of the contract template shall be held of liability of any kind."])

(re-frame/reg-sub
 ::assistant-terms
 (fn [db]
   assistant-terms))

(re-frame/reg-sub
 ::initial-agreements-assistant
 (fn [db]
   (common/make-array-same-element {:terms assistant-terms} :terms false)))

(re-frame/reg-sub
 ::expected-agreements-assistant
 (fn [db]
   (common/make-array-same-element {:terms assistant-terms} :terms true)))

(re-frame/reg-sub
 ::estimate
 (fn [db _]
   (:estimate db)))

(re-frame/reg-sub
 ::estimate-status
 :<- [::estimate]
 (fn [estimate]
   (:status estimate)))

(re-frame/reg-sub
 ::estimate-processing?
 :<- [::estimate-status]
 (fn [status]
   (= status :processing)))

(re-frame/reg-sub
 ::estimate-value
 :<- [::estimate]
 (fn [estimate]
   (:value estimate)))

(re-frame/reg-sub
 ::estimate-state
 :<- [::estimate-processing?]
 :<- [::estimate-value]
 (fn [[processing? value] _]
   (if processing?
     :processing
     (if value :ready :not-yet))))


(re-frame/reg-sub
 ::ledger-ready?
 :<- [::estimate-value]
 (fn [value]
   (boolean value)))

(re-frame/reg-sub
 ::ledger-button-class
 :<- [::ledger-ready?]
 (fn [ready?]
   (common/button-class (not ready?))))
