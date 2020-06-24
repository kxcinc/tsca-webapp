(ns tsca-webapp.recaptcha.subs
  (:require-macros [tsca-webapp.settings :refer [recaptcha-secret-key]])
  (:require [re-frame.core :as re-frame]
            [tsca-webapp.common.subs-parts :as common]))

(re-frame/reg-sub
 ::recaptcha
 (fn [db _]
   (:recaptcha db)))

(re-frame/reg-sub
 ::recaptcha-status
 :<- [::recaptcha]
 (fn [{:keys [status]} _]
   (or status "-")))

(re-frame/reg-sub
 ::token
 :<- [::recaptcha]
 (fn [{:keys [token]} _]
   token))

(re-frame/reg-sub
 ::show-message?
 :<- [::token]
 (fn [token _]
   (boolean token)))

(re-frame/reg-sub
 ::instruction-command
 :<- [::token]
 (fn [token _]
   (str "curl -X POST -d\"secret=" (recaptcha-secret-key)
        "&response=" token
        "\" https://www.google.com/recaptcha/api/siteverify")))
