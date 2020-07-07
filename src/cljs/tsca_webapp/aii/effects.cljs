(ns tsca-webapp.aii.effects
  (:require
   [re-frame.core :as re-frame]
   [cljs.core.match :refer-macros [match]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [tsca-webapp.task.effects :as task]
   ["../common/mock.js" :as mock])
  (:require-macros [tsca-webapp.aii :refer [defcommand]]))

(declare aii)
(def loading-interval 250)

(defn- initialize []
  (js/Promise.
   (fn [resolve reject]
     (letfn [(trial [] (if js/window.TSCAInternalInterface
                         (do (def aii js/window.TSCAInternalInterface)
                             (resolve aii))
                         (js/setTimeout trial loading-interval)))]
       (trial)))))


(defcommand bookhash-list []
  (aii.RefMaster.listAdvertizedBooks))

(defcommand book-info [bookhash]
  (aii.InfoBank.getBook #js {:bookhash bookhash}))

(defcommand book-charge [bookhash]
  (aii.RefMaster.getBookCharges #js {:bookhash bookhash}))

(defcommand book-status [bookhash]
  (aii.RefMaster.getBookStatus #js {:bookhash bookhash}))

(defcommand book-references [bookhash]
  (aii.RefMaster.getBookReferences #js {:bookhash bookhash}))

(defcommand provider-info [providerident]
  (aii.RefMaster.getProviderInfo #js {:provider providerident}))

(defn book-info-and-provider [bookhash]
  (-> (book-info bookhash)
      (.then (fn [info]
               (-> (provider-info (:provider info))
                   (.then (fn [provider-detail]
                            (assoc info :provider-detail provider-detail))))))))

(defcommand template-current-version [tmplhash]
  (aii.RefMaster.templateCurrentVersion #js {:tmplhash tmplhash}))

(defn all-book-info []
  (-> (bookhash-list)
      (.then #(:books %))
      (.then (fn [books]
               (->> books
                    (map #(:bookhash %))
                    (map book-info)
                    (js/Promise.all))))))

(re-frame/reg-fx
 :aii-initialize
 (fn [callback-ids]
   (task/callback callback-ids (initialize))))


(defn- process-single [command]
  (match [command]
         [{:type :all-book}] (all-book-info)
         [{:type :book-info :bookhash bookhash}] (book-info-and-provider bookhash)
         [{:type :book-charge :bookhash bookhash}] (book-charge bookhash)
         [{:type :book-status :bookhash bookhash}] (book-status bookhash)
         [{:type :book-references :bookhash bookhash}] (book-references bookhash)
         [{:type :provider-info :providerident providerident}] (provider-info providerident)
         :else (js/Promise.reject (str "unknown command" command))))

(re-frame/reg-fx
 :aii
 (fn [{:keys [commands] :as callback-ids}]
   (let [promise (js/Promise.all (map process-single commands))]
     (task/callback callback-ids (.then promise #(mock/sleep 1000 %))))))
