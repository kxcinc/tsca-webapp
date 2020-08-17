(ns tsca-webapp.bil.effects
  (:require
   [re-frame.core :as re-frame]
   [cljs.core.match :refer-macros [match]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [tsca-webapp.task.effects :as task]
   ["../common/mock.js" :as mock])
  (:require-macros [tsca-webapp.aii :refer [defcommand]]))

(declare bil)
(def loading-interval 250)

(defn- initialize []
  (js/Promise.
   (fn [resolve reject]
     (letfn [(trial [] (if js/window.TSCABookappInterface
                         (do (def bil js/window.TSCABookappInterface)
                             (resolve bil))
                         (js/setTimeout trial loading-interval)))]
       (trial)))))

(re-frame/reg-fx
 :bil-initialize
 (fn [callback-ids]
   (task/callback callback-ids (initialize))))

(defn- parse-period [source]
  (-> (.split source ";")
      second))

(defn process-single [_]
  (let [avatar (-> (bil.avatars) first bil.avatarInfo)]
    {:fund-amount          (.-balance avatar)
     :original-fund-amount (.-amount (bil.genesisInfo))
     :frozen-until         (parse-period (.-storage avatar))}))

(re-frame/reg-fx
 :bil
 (fn [{:keys [commands] :as callback-ids}]
   (let [promise (js/Promise.all (map process-single commands))]
     (task/callback callback-ids (.then promise #(mock/sleep 1000 %))))))
