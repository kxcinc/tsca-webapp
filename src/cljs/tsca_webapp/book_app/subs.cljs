(ns tsca-webapp.book-app.subs
  (:require [re-frame.core :as re]
            [cljs.core.match :refer-macros [match]]
            [tsca-webapp.common.subs-parts :as common]))

(re/reg-sub
 ::parameters
 :<- [::common/routing-params]
 (fn [{:keys [bahash]}]
   (match [bahash]
          ["MOCK_bookhash_proto0_funny"]  [{:title "Initial Balance"}]
          ["MOCK_bookhash_proto0_frozen"] [{:title "Fund Amount"}
                                           {:title "Unfrozen Timestamp"}
                                           {:title "Fund Owner"}]
          :else [])))

(re/reg-sub
 ::button-label
 :<- [::common/routing-params]
 (fn [{:keys [bahash]}]
   (match [bahash]
          ["MOCK_bookhash_proto0_funny"]  "make fun!"
          ["MOCK_bookhash_proto0_frozen"] "withdraw"
          :else "unknown")))

