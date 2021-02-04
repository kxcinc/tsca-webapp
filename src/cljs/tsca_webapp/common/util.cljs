(ns tsca-webapp.common.util
  (:require [oops.core :refer [oset!]]))

(def loading-interval 250)

(defn- wait-until [f]
  (js/Promise.
   (fn [resolve reject]
     (letfn [(trial [] (if-let [obj (f)]
                         (resolve obj)
                         (js/setTimeout trial loading-interval)))]
       (trial)))))

(defn- load-script [url object-name]
  (let [el (doto (js/document.createElement "script")
             (oset! :src url))]
    (js/document.body.appendChild el)
    (wait-until #(aget js/window object-name))))

(defn mutez->tez [v]
  (/ v 1000000))

(defn format-as-tez [mutez]
  (str (mutez->tez mutez) "ꜩ"))

(defn str->json [s]
  (when-not (empty? s)
    (js/JSON.parse s)))
