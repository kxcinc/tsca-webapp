(ns tsca-webapp.routes.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import [goog History]
           [goog.history EventType])
  (:require
   [clojure.string :as s]
   [tsca-webapp.mock :as mock]
   [secretary.core :as secretary]
   [goog.events :as gevents]
   [re-frame.core :as re-frame]
   [tsca-webapp.routes.events :as events]
   [tsca-webapp.book.events :as book]))

;; it must be created initially and only once
(defonce history (History.))

(defn hook-browser-navigation! []
  (doto history
    (gevents/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn- comma-separated [string]
  (->> (s/split string #",")
       (filter #(not (empty? %)))))

(defn- clj->str [o]
  (-> o clj->js js/JSON.stringify))

(defn app-routes []
  (secretary/set-config! :prefix "#")
  ;; --------------------
  ;; define routes here
  (defroute "/" []
    (re-frame/dispatch [::events/set-active-panel :home-panel
                        nil
                        [::book/open-list]]))

  (defroute "/widgets/spellassistant/proto0/frozen/:label" {:as params}
    (re-frame/dispatch [::events/set-active-panel :spell-assistant params]))

  (defroute "/widgets/chainclerks/tezos" {:as params}
    (re-frame/dispatch [::events/set-active-panel :clerk-panel params]))

  (defroute "/clerk/" []
    (let [params {:query-params
                  {:networks (clj->str {:netident "testnet" :chainid "NetXjD3HPJJjmcd"})
                   :for mock/target-spec-frozen
                   :spell mock/spell-frozen
                   :sahash mock/sahash-frozen}}]
      (re-frame/dispatch [::events/set-active-panel :clerk-panel params])))

  (defroute "/about" []
    (re-frame/dispatch [::events/set-active-panel :about-panel]))

  (defroute "/ledger" []
    (re-frame/dispatch [::events/set-active-panel :ledger-panel]))

  (defroute "/:bookhash" {:as params}
    (re-frame/dispatch [::events/set-active-panel :book-top params [::book/open params]]))

  (defroute "/sr/" []
    (re-frame/dispatch [::events/set-active-panel :spell-runner-panel]))


  ;; --------------------
  (hook-browser-navigation!))
