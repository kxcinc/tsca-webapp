(ns tsca-webapp.ledger.effects
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   ["buffer" :as buffer]
   ["@ledgerhq/hw-transport-u2f" :as u2f]
   ))

(defn- hex-str->buffer [hex-str]
  (->> hex-str
       (re-seq #"(?i)[\da-f]{2}")
       (map #(js/parseInt % 16))
       (js/Uint8Array.)
       (buffer/Buffer.from)))

(defn- buffer->hex-str [buf]
  (-> buf.buffer
      (.slice buf.byteOffset (+ buf.byteOffset buf.byteLength))
      js/Uint8Array.
      (js/Array.prototype.map.call #(-> (str "00" (.toString % 16))
                                        (.slice -2)))
      (.join " ")))

(def scramble-key "xtz")

(defn- transport-through-apdu [command-buf]
  (-> (.-default u2f)
      (.open)
      (.then (fn [transport]
               (js/console.log transport)
               (.setScrambleKey transport scramble-key)
               (.setUnwrap transport false)
               (.exchange transport command-buf)))))

(defn try-promise [body]
  (js/Promise. (fn [resolve reject]
                 (try (resolve (body))
                    (catch :default e
                      (reject e)) ))))

(re-frame/reg-fx
 :apdu
 (fn [{:keys [command received-event-id error-event-id]}]
   (-> (try-promise #(hex-str->buffer command))
       (.then transport-through-apdu)
       (.then #(re-frame/dispatch [received-event-id (buffer->hex-str %)]))
       (.catch #(re-frame/dispatch [error-event-id %])))))

