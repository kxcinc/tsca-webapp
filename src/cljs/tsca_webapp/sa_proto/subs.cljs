(ns tsca-webapp.sa-proto.subs
  (:require [re-frame.core :as re]
            [tsca-webapp.common.subs-parts :as common]
            [clojure.string :as s]))

(re/reg-sub
 ::routing-params
 (fn [db]
   (:routing-params db)))

(re/reg-sub
 ::label
 :<- [::routing-params]
 (fn [params]
   (:label params)))

(re/reg-sub
 ::target-spec
 :<- [::routing-params]
 (fn [params]
   (get-in params [:query-params :for])))

(re/reg-sub
 ::networks
 :<- [::routing-params]
 (fn [params]
   (some-> (get-in params [:query-params :networks])
           (s/split ","))))




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
 (fn [db]
   (common/make-array-same-element {:terms assistant-terms} :terms false)))

(re/reg-sub
 ::expected-agreements-assistant
 (fn [db]
   (common/make-array-same-element {:terms assistant-terms} :terms true)))
