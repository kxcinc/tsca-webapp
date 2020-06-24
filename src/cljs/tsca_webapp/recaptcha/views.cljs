(ns tsca-webapp.recaptcha.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [tsca-webapp.recaptcha.events :as events]
   [tsca-webapp.recaptcha.subs :as subs]))

(defn top []
  [:div
   [:div "status: " @(re-frame/subscribe [::subs/recaptcha-status])]
   [:div "This site is protected by reCAPTCHA and the Google "
    [:a {:href "https://policies.google.com/terms"} "Terms of Service"]
    " and "
    [:a {:href "https://policies.google.com/privacy"} "Privacy Policy"]
    " apply."]
   [:div.gap]
   [:button.btn
    {:on-click #(re-frame/dispatch [::events/check])}
    "check"]
   [:div.gap]
   (when @(re-frame/subscribe [::subs/show-message?])
     (seq
      [[:div "Token is: " @(re-frame/subscribe [::subs/token])]
       [:div.gap]
       [:div "Now you should run this command:"
        [:pre {:style {:white-space "pre-wrap"
                       :border "1px solid black"
                       :padding "8px"
                       :margin "8px"}}
         @(re-frame/subscribe [::subs/instruction-command])]]

       [:div "then you will get something like:"
        [:pre {:style {:white-space "pre-wrap"
                       :border "1px solid black"
                       :padding "8px"
                       :margin "8px"}}
         "{
        \"success\": true,
        \"challenge_ts\": \"2020-06-24T11:58:24Z\",
        \"hostname\": \"localhost\",
        \"score\": 0.9,
        \"action\": \"submit\"
}"]]]))])

