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

;; #js {:network js-network
;;      :target js-target
;;      :spell js-spell}
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
  (prn "x" js-ops)
  (let [js-network (.-network js-ops)]
    (-> (forge-operation js-ops)
        (.then (fn [{:keys [unsignedtxn simprivinfo]}]
                 (simulate-operation js-network unsignedtxn
                                     ;; todo: wasted conversion
                                     (clj->js simprivinfo))))
        (.then (fn [ret]
                 (prn "y" ret)
                 (if (:succeeded ret) (dissoc ret :succeeded)
                     (js/Promise.reject (:error ret))))))))

(defcommand inject-operation [unsignedtxn signer srcaddr signature network]
  (aii.Proto0.injectOperation #js {:unsignedtxn unsignedtxn
                                   :signer signer
                                   :srcaddr srcaddr
                                   :signature signature
                                   :network network}))

(defcommand check_operation_injection [inj-token]
  (aii.Proto0.checkOperationInjection #js {:injtoken inj-token}))

;; (defn- str->js [string]
;;   (-> string js/JSON.parse))

;; (-> (simulate
;;      #js {:target (clj->js {:spellkind "spellofgenesis"
;;                             :tmplversion "MOCK_tmplversion_proto0_frozen"})
;;           :spell (str->js "{\"fund_owners\":[\"abc\"],\"fund_amount\":10,\"unfrozen\":\"2020-07-04T00:00:00Z\"}")
;;           :network (clj->js {:netident "testnet" :chainid "NetXjD3HPJJjmcd"})})
;;     (.then prn))

;; {:succeeded true, :networkfees {:fee 0.572, :burn 2.3}, :templatefees {:agency 0.5, :provider 1.5}, :rawamount 10, :adjustedtxn "03ef3ccca0cf0a4c1706cdd89d30f3ac3cef34fc78c23f140231c969501bc387d16c001e44b16562327dd33068e4731764191461bcccbe830af2ef25c35000a08d0600000d3c0b644fdf9081e80f73dd3a4735c5632c81da00"}

;; {:succeeded false, :error "spell not valid according to MOCK_sahash_proto0_frozen_genesis"}

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
         [{:type :source-address :public-key pk}] (calculate-address-from-public-key pk)
         [{:type :simulate :ops ops}] (simulate ops)
         [{:type :spell-verifier :sahash sahash}] (generate-spell-verifier sahash)
         ;; [{:type :re-simulate :adjusted-txn txn :simprivinfo simprivinfo
         ;;   :network network :fee fee}] (confirm-simulation txn simprivinfo network fee)
         :else (js/Promise.reject (str "unknown command" command))))

(re-frame/reg-fx
 :aii
 (fn [{:keys [commands] :as callback-ids}]
   (let [promise (js/Promise.all (map process-single commands))]
     (task/callback callback-ids (.then promise #(mock/sleep 1000 %))))))
