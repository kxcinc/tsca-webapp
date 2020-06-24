(ns tsca-webapp.recaptcha.events
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-fx
 ::check
 (fn-traced [coeffects [_ command]]
            (let [db (:db coeffects)]
              {:db        (-> db
                              (assoc [:recaptcha] {:status :sending}))
               :recaptcha {:received-event-id ::success
                           :error-event-id    ::failure}})))
(re-frame/reg-event-db
 ::success
 (fn-traced [db [_ token]]
            (-> db
                (assoc-in [:recaptcha :status] :success)
                (assoc-in [:recaptcha :token] token)
                (assoc-in [:recaptcha :error] nil))))

(re-frame/reg-event-db
 ::failure
 (fn-traced [db [_ ex]]
            (-> db
                (assoc-in [:recaptcha :status] :failure)
                (assoc-in [:recaptcha :error] ex))))
