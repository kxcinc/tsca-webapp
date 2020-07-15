(ns tsca-webapp.routes.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:require [tsca-webapp.mock :as mock]
            [secretary.core :as secretary]
            [re-frame.core :as re-frame]
            [tsca-webapp.book.events :as book]))

(declare routing-table)

(defn- clj->str [o]
  (-> o clj->js js/JSON.stringify))

(defn- url-path []
  (str js/window.location.pathname js/window.location.search))

(defn- initial-dispatch []
  (secretary/dispatch! (url-path)))

(defn- page-initial-event [page-key]
  (let [[_ event-id] (get routing-table page-key)]
    (when event-id [event-id])))

(defn- dispatch [event-id page-key params]
  (re-frame/dispatch [event-id page-key params (page-initial-event page-key) true]))

(defn app-routes [event-id]
  (secretary/set-config! :prefix "")

  (defroute top "/" []
    (dispatch event-id :home-panel nil ))

  (defroute book-top "/:bookhash" {:as params}
    (dispatch event-id :book-top params))

  (defroute sa-proto0  "/widgets/spellassistant/proto0/frozen/:label" {:as params}
    (dispatch event-id :spell-assistant params))

  (defroute chain-clerk "/widgets/chainclerks/tezos" {:as params}
    (dispatch event-id :clerk-panel params))

  (defroute cheat-clerk "/clerk/" []
    (let [params {:query-params
                  {:networks (clj->str {:netident "testnet" :chainid "NetXjD3HPJJjmcd"})
                   :for mock/target-spec-frozen
                   :spell mock/spell-frozen
                   :sahash mock/sahash-frozen}}]
      (dispatch event-id :clerk-panel params)))

  (defroute cheat-ledger "/ledger/" []
    (dispatch event-id :ledger-panel nil))

  (def routing-table {:home-panel      [top      ::book/open-list]
                      :book-top        [book-top ::book/open]
                      :spell-assistant [sa-proto0]
                      :clerk-panel     [chain-clerk]})
  (initial-dispatch))

(defn- generate-url [[_ page-key params]]
  (let [[route-func] (get routing-table page-key)]
    (if route-func
      (route-func params)
      (throw (str "Unknown page:" page-key "(" params ")")))))

(defn rewrite-url [event]
  (let [url (generate-url event)]
    (-> js/history
        (.pushState url nil url))))


