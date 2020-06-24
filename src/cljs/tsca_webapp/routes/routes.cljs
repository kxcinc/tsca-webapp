(ns tsca-webapp.routes.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import [goog History]
           [goog.history EventType])
  (:require
   [secretary.core :as secretary]
   [goog.events :as gevents]
   [re-frame.core :as re-frame]
   [tsca-webapp.routes.events :as events]))

(defn hook-browser-navigation! []
  (doto (History.)
    (gevents/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn app-routes []
  (secretary/set-config! :prefix "#")
  ;; --------------------
  ;; define routes here
  (defroute "/" []
    (re-frame/dispatch [::events/set-active-panel :home-panel])
    )

  (defroute "/about" []
    (re-frame/dispatch [::events/set-active-panel :about-panel]))

  (defroute "/ledger" []
    (re-frame/dispatch [::events/set-active-panel :ledger-panel]))

  (defroute "/:bookhash" {:as params}
    (re-frame/dispatch [::events/set-active-panel :book-top params]))

  (defroute "/sr/" []
    (re-frame/dispatch [::events/set-active-panel :spell-runner-panel]))

  (defroute "/clerk/" []
    (re-frame/dispatch [::events/set-active-panel :clerk-panel]))

  (defroute "/recaptcha/" []
    (re-frame/dispatch [::events/set-active-panel :recaptcha-panel]))


  ;; --------------------
  (hook-browser-navigation!))
