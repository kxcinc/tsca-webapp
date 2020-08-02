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

(defcommand forge-operation [js-ops-model]
  (aii.Proto0.forgeOperation js-ops-model))

(defcommand calculate-address-from-public-key [public-key]
  (aii.TezosUtilities.calculateAddressFromPublicKey public-key))

(defcommand get-spell-verifier [sahash]
  (aii.Proto0.getSpellVerifier #js {:sahash sahash}))

(defn generate-spell-verifier [sahash]
  (-> (get-spell-verifier "MOCK_sahash_proto0_frozen_genesis")
      (.then #(:verifier %))))

(defcommand simulate-operation [js-network txn js-simprivinfo]
  (aii.Proto0.simulateOperation #js {:network js-network
                                     :txn txn
                                     :simprivinfo js-simprivinfo}))

(defn- simulate [js-ops]
  (let [js-network (.-network js-ops)]
    (-> (forge-operation js-ops)
        (.then (fn [{:keys [unsignedtxn simprivinfo]}]
                 (let [js-simprivinfo (clj->js simprivinfo) ;; fixme: wasted conversion
                       ]
                   (-> (simulate-operation js-network unsignedtxn js-simprivinfo)
                       (.then (fn [ret]
                                (if (:succeeded ret)
                                  (-> ret
                                      (dissoc :succeeded)
                                      (assoc :simprivinfo js-simprivinfo))
                                  (js/Promise.reject (:error ret))))))))))))

(defcommand check-operation-injection [inject-token]
  (aii.Proto0.checkOperationInjection #js {:injtoken inject-token}))

(defn- same-fee? [{:keys [networkfees templatefees rawamount]} fees]
  (and (= networkfees (:networkfees fees))
       (= templatefees (:templatefees fees))
       (= rawamount (:rawamount fees))))

(defn- confirm-simulation [adjustedtxn source-address js-network fees js-simprivinfo]
  (-> (simulate-operation js-network adjustedtxn js-simprivinfo)
      (.then (fn [ret]
               (if (:succeeded ret)
                 (if (same-fee? ret fees)
                   (:adjustedtxn ret)
                   (js/Promise.reject "fee isn't same"))
                 (js/Promise.reject (:error ret)))))))

(defcommand inject-operation [txn public-key source-address signature js-network]
  (aii.Proto0.injectOperation #js {:unsignedtxn txn
                                   :signer public-key
                                   :srcaddr source-address
                                   :signature signature
                                   :network js-network}))

(defn- inject [txn public-key source-address signature js-network]
  (-> (inject-operation txn public-key source-address signature js-network)
      (.then (fn [{:keys [injtoken timeout minqueryinterval]}]
               {:injection-token injtoken :interval minqueryinterval}))))

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

(defcommand generate-description [sahash]
  (throw "not implemented"))

(defcommand generate-cli-instructions [sahash spell]
  [{:prompt "dummy"
    :line (str "tezos-client transfer 10 from "
               "tz1NQ5Fk7eJCe1zGmngv2GRnJK9G1nEnQahQ"
               " to KT1CUTjTqf4UMf6c9A8ZA4e1ntWbLVTnvrKG --arg \"0x030292837483\" --fee 0.572 --burn-cap 2.3")}])

(re-frame/reg-fx
 :aii-initialize
 (fn [callback-ids]
   (task/callback callback-ids (initialize))))

(defn- repeat-until [f pred interval]
  (js/Promise. (fn [resolve reject]
                 (letfn [(step []
                           (try
                             (-> (f)
                                 (.then (fn [ret]
                                          (if (pred ret)
                                            (resolve ret)
                                            (js/setTimeout #(step) interval))))
                                 (.catch (fn [ex] (reject ex))))
                             (catch :default e (reject e))))]
                   (step)))))

(def suffix "+successful"
  ;; "+timeout"
  ;; "+failed"
  ;; "+toofreq"
  )
(def dummy-suffix (atom (cycle ["" "" "" suffix])))

(defn- waiting-for-done [injection-token interval]
  (-> (repeat-until #(let [suffix (ffirst (swap-vals! dummy-suffix rest))]
                       (check-operation-injection (str injection-token suffix)))
                    (fn [result]
                      (if (= (:status result) "progressing")
                        (js/console.log (str "progressing:" injection-token
                                             "(" interval ")")
                                        (clj->js result))
                        result))
                    interval)
      (.then (fn [result]
               (if (= (:status result) "successful")
                 result
                 (do
                   (js/console.error "injection error" (or (:logs result) (:reason result)))
                   (js/Promise.reject {:type (keyword (str "op-" (:status result)))
                                       :reason (:reason result)})))))))

(defn- process-single [command]
  (match [command]
         [{:type :all-book}] (all-book-info)
         [{:type :book-info :bookhash bookhash}] (book-info-and-provider bookhash)
         [{:type :book-charge :bookhash bookhash}] (book-charge bookhash)
         [{:type :book-status :bookhash bookhash}] (book-status bookhash)
         [{:type :book-references :bookhash bookhash}] (book-references bookhash)
         [{:type :provider-info :providerident providerident}] (provider-info providerident)
         [{:type :source-address :public-key pk}] (calculate-address-from-public-key pk)
         [{:type :simulate :ops ops}] (simulate ops)
         [{:type :spell-verifier :sahash sahash}] (generate-spell-verifier sahash)
         [{:type :description :sahash sahash}] (generate-description sahash)
         [{:type :cli-instructions
           :sahash sahash :spell spell}]    (generate-cli-instructions sahash spell)
         [{:type :re-simulate :adjusted-txn txn :simprivinfo js-simprivinfo
           :source-address source :network js-network :fee fee}] (confirm-simulation txn source js-network fee js-simprivinfo)
         [{:type :inject-operation :signature signature
           :txn txn :public-key public-key :source-address source-address
           :network js-network}] (inject txn public-key source-address signature js-network)
         [{:type :confirm-injection :injection-token injection-token :interval interval}] (waiting-for-done injection-token interval)
         :else (js/Promise.reject (str "unknown command" command))))

(re-frame/reg-fx
 :aii
 (fn [{:keys [commands] :as callback-ids}]
   (let [promise (js/Promise.all (map process-single commands))]
     (task/callback callback-ids (.then promise #(mock/sleep 1000 %))))))
