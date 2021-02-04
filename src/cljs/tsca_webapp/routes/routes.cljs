(ns tsca-webapp.routes.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:require [tsca-webapp.mock :as mock]
            [secretary.core :as secretary]
            [re-frame.core :as re-frame]
            [tsca-webapp.book.events :as book]
            [tsca-webapp.spell-assistant.events :as sa]
            [tsca-webapp.chain-clerk.events :as cc]))

(declare routing-table)

(defn- url-path []
  (str js/window.location.pathname js/window.location.search))

(defn- initial-dispatch []
  (let [path (url-path)]
    (.replaceState js/history path nil path)
    (set! (.-onpopstate js/window)
          (fn [e]
            (when-let [path (.-state e)]
              (secretary/dispatch! path))))
    (secretary/dispatch! path)))

(defn- dispatch [event-id page-key params]
  (re-frame/dispatch [event-id page-key params true]))

(defn- generate-url [[_ page-key params]]
  (let [[route-func] (get routing-table page-key)]
    (if route-func
      (route-func params)
      (throw (str "Unknown page:" page-key "(" params ")")))))

(defn rewrite-url [event]
  (let [url (generate-url event)]
    (-> js/history
        (.pushState url nil url))))

(defn page-open-event [page-key]
  (let [[_ page-open-event-id] (get routing-table page-key)]
    (when page-open-event-id [page-open-event-id])))

(defn app-routes [event-id]
  (secretary/set-config! :prefix "")

  (defroute top "/" []
    (dispatch event-id :home-panel nil ))

  (defroute book-top "/:bookhash" {:as params}
    (dispatch event-id :book-top params))

  (defroute spell-assistant  "/widgets/spellassistant/:tmplhash/:label" {:as params}
    (dispatch event-id :spell-assistant params))
  (defroute spell-assistant-with-cors  "/widgets/o/spellassistant/:tmplhash/:label" {:as params}
    ;; don't dispatch :spell-assistant here to generete url from key
    (dispatch event-id :spell-assistant-with-cors params))

  (defroute chain-clerk "/widgets/chainclerks/tezos" {:as params}
    (dispatch event-id :clerk-panel params))

  (defroute cheat-sa "/sa/" []
    (let [params {:query-params
                  {:networks mock/testnet
                   :for      mock/target-spec-frozen}
                  :label    "genesis"
                  :tmplhash mock/tmplhash-frozen}]
      (dispatch event-id :spell-assistant params)))
  (defroute cheat-clerk "/clerk/" []
    (let [params {:query-params
                  {:networks mock/testnet
                   :for      mock/target-spec-frozen
                   :spell    mock/spell-frozen
                   :sahash   mock/sahash-frozen}}]
      (dispatch event-id :clerk-panel params)))

  (defroute cheat-ledger "/ledger/" []
    (dispatch event-id :ledger-panel nil))

  (def routing-table {:home-panel                [top      ::book/open-list]
                      :book-top                  [book-top ::book/open]
                      :spell-assistant           [spell-assistant ::sa/generate-verifier]
                      :spell-assistant-with-cors [spell-assistant-with-cors ::sa/generate-verifier]
                      :clerk-panel               [chain-clerk ::cc/load-description]})
  (initial-dispatch))

