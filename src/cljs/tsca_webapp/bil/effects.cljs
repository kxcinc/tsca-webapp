(ns tsca-webapp.bil.effects
  (:require
   [re-frame.core :as re-frame]
   [cljs.core.match :refer-macros [match]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [tsca-webapp.task.effects :as task]
   [tsca-webapp.common.util :as util])
  (:require-macros [tsca-webapp.aii :refer [defcommand]]))

(declare bil)

(def bil-url "/_tscalibs/bookapp-interface.js")

(defn- initialize []
  (-> (util/load-script bil-url "TSCABookappInterface")
      (.then (fn [obj]
               (def bil obj)))))

(re-frame/reg-fx
 :bil-initialize
 (fn [callback-ids]
   (task/callback callback-ids (initialize))))

(defn- parse-period [source]
  (-> (.split source ";")
      second))



(defn load-initial-values []
  (-> (bil.interpretSpiritStatus "basic.json")
      (.then (fn [[_ result-json]] result-json))
      (.then js/JSON.parse)
      (.then #(js->clj % :keywordize-keys true))))

(defn display-spell-assistant [salabel dom-id]
  (-> (util/wait-until #(js/document.getElementById dom-id))
      (.then (fn [dom]
               (bil.displaySpellAssistant salabel dom)))))

(defmulti dispatch :type)
(defmethod dispatch :initial-values [_]
  (load-initial-values))

(defmethod dispatch :display-spell-assistant [{:keys [salabel dom-id]}]
  (display-spell-assistant salabel dom-id))

(re-frame/reg-fx
 :bil
 (fn [{:keys [commands] :as callback-ids}]
   (task/callback callback-ids (js/Promise.all (map dispatch commands)))))
