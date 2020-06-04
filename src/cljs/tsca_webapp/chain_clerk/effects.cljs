(ns tsca-webapp.chain-clerk.effects
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   ["../common/mock.js" :as mock]))

(re-frame/reg-fx
 ::on-move
 (fn-traced
  [{:keys [tag step]}]
  (prn "pass:" tag step)))

