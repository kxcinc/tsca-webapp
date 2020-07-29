(ns tsca-webapp.book-app.events
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-db
 ::open
 (fn-traced
  [db _]
  db))

(re-frame/reg-event-fx
 ::change-iframe-url
 (fn-traced [{:keys [db]} [_ url]]
            {:dom {:type :iframe :id "assistant-modal" :url url}}))

