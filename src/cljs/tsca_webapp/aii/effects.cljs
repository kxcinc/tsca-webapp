(ns tsca-webapp.aii.effects
  (:require
   [re-frame.core :as re-frame]
   [cljs.core.match :refer-macros [match]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [tsca-webapp.task.effects :as task]
   [oops.core :refer [oset!]]
   ["../common/mock.js" :as mock])
  (:require-macros [tsca-webapp.aii :refer [defcommand]]))

(declare aii)
(def loading-interval 250)
;; (def aii-url "https://devapi.tsca.kxc.io/aii-jslib.js")
(def aii-url "/js/aii-jslib.js")

(defn- load-script [url object-name]
  (let [el (doto (js/document.createElement "script")
             (oset! :src url))]
    (js/document.body.appendChild el)
    (js/Promise.
     (fn [resolve reject]
       (letfn [(trial [] (if-let [obj (aget js/window object-name)]
                           (resolve obj)
                           (js/setTimeout trial loading-interval)))]
         (trial))))))

(defn- initialize []
  (-> (load-script aii-url "TSCAInternalInterface")
      (.then (fn [obj]
               (def aii obj)))))


(defcommand bookhash-list []
  (aii.RefMaster.listAdvertizedBooks))

(defcommand book-info [bookhash]
  (aii.InfoBank.getBookEntry (js/String bookhash)))

(defcommand book-status [bookhash]
  (aii.RefMaster.getBookStatus (js/String bookhash)))

(defcommand provider-info [providerident]
  (aii.RefMaster.getProviderInfo  (js/String providerident)))

(defn book-info-and-provider [bookhash]
  (-> (book-info bookhash)
      (.then (fn [info]
               (-> (provider-info (:provider info))
                   (.then (fn [provider-detail]
                            (assoc info :provider-detail provider-detail))))))))

(defcommand forge-operation [js-ops-model]
  (aii.Proto0.forgeOperation js-ops-model))

(defcommand calculate-address-from-public-key [public-key]
  (aii.TezosUtils.calculateaddressfromledgerpublickey public-key))

(defcommand get-spell-verifier [sahash]
  (aii.Proto0.getSpellVerifier (js/String sahash)))

(defn generate-spell-verifier [sahash]
  (-> (get-spell-verifier sahash)
      (.then #(:verifier %))))

(defn- build-process-request [network for spell user-info]
  {:request-api aii.Proto0.processGenesisRequest
   :simulate-api aii.Proto0.simulateGenesis
   :request {:network network
             :template (.-tmplhash for)
             :requester (:source-address user-info)
             :name (:name user-info)
             :email (:e-mail user-info)
             :spell spell}})

(comment
  (let [{:keys [api request]} (build-process-request (js/JSON.parse m/testnet)
                                                     {:spellkind "spellofgenesis"
                                                      :tmplhash "tmpL1Q7GJiqzmwuS3SkSiWbreXWsxWrk3Y"}
                                                     m/spell-frozen
                                                     {:name "Ichiro Sakai"
                                                      :e-mail "i.sakai@example.org"
                                                      :source-address "tz1gmHNqdXuM5bf8FU9dNaACuAqgRA9VLGUm"
                                                      ;; "tz1NQ5Fk7eJCe1zGmngv2GRnJK9G1nEnQahQ"
                                                      })]
    (def api api)
    (def request (clj->js request)))
  
  (.then (api request)
         #(def proc %))

  (-> (TSCAInternalInterface.Proto0.simulateGenesis request proc)
      (.then
       (fn [[result-type message]]
         (if (= result-type "SimulationFailed")
           (js/Promise.reject (get (js->clj message) "error_message"))
           message)))
      (.then #(def sim-result (js->clj %)))
      (.catch prn))

  (aii.Helpers.formatCliInstructionIntoString (.-cli_instructions proc))

  (str (get sim-result "watermark")
       (get sim-result "unsigned_transaction")))


(defn- simulate [network for spell user-info]
  (let [{:keys [request-api simulate-api request]} (build-process-request network for spell user-info)
        request (clj->js request)]
    (-> (request-api request)
        (.then (fn [proc]
                 (let [instruction (aii.Helpers.formatCliInstructionIntoString (.-cli_instructions proc))]
                   (prn "proc" simulate-api)
                   (-> (simulate-api request proc)
                       (.then (fn [[result-type result]]
                                (if (= result-type "SimulationFailed")
                                  (js/Promise.reject (get (js->clj result) "error_message"))
                                  (assoc (js->clj result :keywordize-keys true)
                                         :instruction instruction)))))))))))

(defcommand check-operation-injection [inject-token]
  (aii.Proto0.checkOperationInjection #js {:injtoken inject-token}))

(defn- same-fee? [{:keys [networkfees templatefees rawamount]} fees]
  (and (= networkfees (:networkfees fees))
       (= templatefees (:templatefees fees))
       (= rawamount (:rawamount fees))))

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
      (.then (fn [books]
               (->> books
                    (map #(:bookhash %))
                    (map book-info)
                    (js/Promise.all))))))

(defcommand generate-description [for spell sahash]
  (clojure.string/join "\n" [(str "sahash: " sahash)
                             (str "spell: " spell)
                             (str "for: " for)]))

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
         [{:type :book-status :bookhash bookhash}] (book-status bookhash)
         [{:type :provider-info :providerident providerident}] (provider-info providerident)
         [{:type :source-address :public-key pk}] (calculate-address-from-public-key pk)
         [{:type :simulate :network network :for for :spell spell :user-info user-info}] (simulate network for spell user-info)
         [{:type :cli-instructions
           :sahash sahash :spell spell}]    (generate-cli-instructions sahash spell)
         [{:type :spell-verifier :sahash sahash}] (generate-spell-verifier sahash)
         [{:type :description :sahash sahash :for for :spell spell}] (generate-description for spell sahash)
         [{:type :inject-operation :signature signature
           :txn txn :public-key public-key :source-address source-address
           :network js-network}] (inject txn public-key source-address signature js-network)
         :else (js/Promise.reject (str "unknown command" command))))

(re-frame/reg-fx
 :aii
 (fn [{:keys [commands] :as callback-ids}]
   (let [promise (js/Promise.all (map process-single commands))]
     (task/callback callback-ids promise))))
