(ns tsca-webapp.routes.views
  (:require
   [re-frame.core :as re-frame]
   [tsca-webapp.routes.subs :as subs]
   [tsca-webapp.book.views :as book]
   [tsca-webapp.chain-clerk.views :as clerk]
   [tsca-webapp.spell-assistant.views :as assistant]
   [tsca-webapp.ledger.views :as ledger]
   [tsca-webapp.recaptcha.views :as recaptcha]))

(defn about-panel []
  [:div
   [:h1 "This is the About Page."]

   [:div
    [:a {:href "#/"}
     "go to Home Page"]]])

(defn- panels [panel-name]
  (case panel-name
    :about-panel        [about-panel]
    :ledger-panel       [ledger/try-ledger]
    :home-panel         [book/home-panel]
    :book-top           [book/book-top]
    :spell-runner-panel [assistant/spell-assistant-top]
    :clerk-panel        [clerk/clerk-top]
    :recaptcha-panel    [recaptcha/top]
    [:h1 "not found"]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [show-panel @active-panel]))
