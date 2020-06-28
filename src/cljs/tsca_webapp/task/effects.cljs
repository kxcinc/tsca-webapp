(ns tsca-webapp.task.effects
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   ["../common/mock.js" :as mock]))

(defonce processes (atom {}))

(defn- call-after [f]
  (js/setTimeout f 0))

(defn- register-process [id promise callback-event-id cancel-func cancel-event-id]
  (let [runner (fn [table]
                 (when-let [m (get table id)]
                   (if-let [f (:cancel-func m)]
                     (do
                       (js/console.log (str id " is already registered. try cancel now"))
                       (f))
                     (js/console.log (str id " is already registered."))))
                 (call-after
                  (fn []
                    (-> promise
                        (.then (fn [result]
                                 (swap! processes #(dissoc % id))
                                 (re-frame/dispatch [callback-event-id result]))))))
                 (assoc table id {:promise promise
                                  :callback-event-id callback-event-id
                                  :cancel-func cancel-func
                                  :cancel-event-id cancel-event-id}))]
    (swap! processes runner)))

(defn cancel-process [id]
  (swap! processes (fn [table]
                     (when-let [{:keys [cancel-func cancel-event-id]} (get table id)]
                       (when cancel-func (cancel-func))
                       (re-frame/dispatch [cancel-event-id]))
                     (dissoc table id))))

(defn cancel-all []
  (swap! processes (fn [table]
                     (doseq [id (keys table)]
                       (when-let [f (-> table id :cancel-func)]
                         (f))
                       (when-let [event-id (-> table id :cancel-event-id)]
                         (re-frame/dispatch [event-id])))
                     {})))

(re-frame/reg-fx
 :process-cancel
 (fn [_]
   (cancel-all)))

(re-frame/reg-fx
 :spell
 (fn [{:keys [params done-event-id cancel-event-id]}]
   (let [x (mock/cancelableSleep 1000 (clj->js params))]
     (register-process :spell-process x.promise done-event-id x.cancel cancel-event-id))))

(re-frame/reg-fx
 :estimate
 (fn [{:keys [params done-event-id cancel-event-id]}]
   (let [x (mock/cancelableSleep 1500 123.4)]
     (register-process :spell-process x.promise done-event-id x.cancel cancel-event-id))))

(re-frame/reg-fx
 :ledger
 (fn [{:keys [find done-event-id cancel-event-id]}]
   (let [x (mock/cancelableSleep (if (= find :confirming) 5000 2000) "tzxxxxxxxx")]
     (register-process find x.promise done-event-id x.cancel cancel-event-id))))
