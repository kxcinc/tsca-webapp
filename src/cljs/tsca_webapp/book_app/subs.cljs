(ns tsca-webapp.book-app.subs
  (:require [re-frame.core :as re]
            [cljs.core.match :refer-macros [match]]
            [tsca-webapp.common.subs-parts :as common]))

(re/reg-sub
 ::book-app
 (fn [{:keys [book-app]}]
   book-app))

(re/reg-sub
 ::status
 :<- [::book-app]
 (fn [{:keys [status]}]
   (or (#{:done :error} status)
       :loading)))

(re/reg-sub
 ::parameters
 :<- [::common/routing-params]
 :<- [::book-app]
 (fn [[{:keys [bahash]} {:keys [values]}]]
   (match [bahash]
          ["MOCK_bookhash_proto0_funny"]
          [{:title "Initial Balance" :value ":smile:"}]

          ["MOCK_bookhash_proto0_frozen"]
          [{:title "Fund Balance" :value (:fund-amount values)}
           {:title "Frozen Until" :value (:frozen-until values)}
           {:title "Original Fund Amount" :value (:original-fund-amount values)}]
          :else [])))

(re/reg-sub
 ::button-label
 :<- [::common/routing-params]
 (fn [{:keys [bahash]}]
   (match [bahash]
          ["MOCK_bookhash_proto0_funny"]  "make fun!"
          ["MOCK_bookhash_proto0_frozen"] "withdraw"
          :else "unknown")))

