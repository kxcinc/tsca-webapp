(ns tsca-webapp.recaptcha.effects
  (:require-macros [tsca-webapp.settings :refer [recaptcha-site-key]])
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [tsca-webapp.recaptcha.events :as events]))

(def tsca_site_key (recaptcha-site-key))

(defn recaptcha-url [site_key]
  (str "https://www.google.com/recaptcha/api.js" "?render=" site_key))

(defn load-grecaptcha []
  (if js/window.grecaptcha
    (js/Promise.resolve js/window.grecaptcha)
    (let [el (.createElement js/document "script")]
      (set! (.. el -src) (recaptcha-url tsca_site_key))
      (.appendChild js/document.body el)
      (js/Promise. (fn [resolve reject]
                     (.addEventListener el "load" #(resolve js/window.grecaptcha)))))))

(defn send-challenge []
  (-> (load-grecaptcha)
      (.then (fn [captcha]
               (-> captcha
                   (.ready
                    (fn []
                      (-> captcha
                          (.execute tsca_site_key #js {:action "submit"})
                          (.then (fn [token]
                                   (js/console.log "token:" token)
                                   (re-frame/dispatch [::events/success token])))
                          (.catch (fn [ex]
                                    (js/console.error "error:" ex)
                                    (re-frame/dispatch [::events/failure (str ex)])))))))))))

(re-frame/reg-fx
 :recaptcha
 (fn-traced
  [{:keys [received-event-id error-event-id]}]
  (send-challenge)))
