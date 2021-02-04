(ns tsca-webapp.aii.effects
  (:require
   [re-frame.core :as re-frame]
   [cljs.core.match :refer-macros [match]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [tsca-webapp.task.effects :as task]
   [tsca-webapp.common.util :as util]
   [tsca-webapp.mock :as m])
  (:require-macros [tsca-webapp.aii :refer [defcommand]]))

(declare aii)

;; (def aii-url "https://devapi.tsca.kxc.io/aii-jslib.js")
;; (def aii-url "/js/aii-jslib.js")
(def aii-url "/_tscalibs/aii-jslib.js")

(defn bookapp-url [spirit-hash]
  (aii.Proto0.bookAppUrlForSpirit spirit-hash))


(defn- initialize []
  (-> (util/load-script aii-url "TSCAInternalInterface")
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

(defcommand get-spell-verifier [label tmplhash]
  (aii.SpellAssistant.getSpellAssistant (js/String label)
                                        (js/String tmplhash)))

(defn generate-spell-verifier [label tmplhash]
  (-> (get-spell-verifier label tmplhash)
      (.then (fn [{:keys [spell_assistant tmplversion]}]
               {:tmplversion tmplversion
                :verifier spell_assistant}))))

(defcommand process-spell-assistant [salabel tmplversion fieldValues]
  (let [param (-> {:tmplversion tmplversion
                   :salabel salabel
                   :fieldValues fieldValues}
                  clj->js
                  js/JSON.stringify)]
    (aii.SpellAssistant.processSpellAssistant (js/JSON.parse param))))

(defn verify-spell-params [salabel tmplversion fieldValues]
  (-> (process-spell-assistant salabel tmplversion fieldValues)
      (.then #(match [%]
                     [["SpellAssistantProcessed" {:spell spell}]] spell

                     [["SpellAssistantProcessError" {:message message
                                                     :fix_hints hints}]]
                     (js/Promise.reject {:message message
                                         :hints hints})

                     :else (js/Promise.reject {:message "unknown format"
                                               :error %})))))

(defn- build-process-request [network for spell user-info]
  (if (= (.-spellkind for) "spellofgenesis")
    {:request-api aii.Proto0.processGenesisRequest
     :simulate-api aii.Proto0.simulateGenesis
     :request {:network network
               :template (.-tmplhash for)
               :requester (:source-address user-info)
               :name (:name user-info)
               :email (:e-mail user-info)
               :spell spell}}

    {:request-api aii.Proto0.processInvocationRequest
     :simulate-api aii.Proto0.simulateInvocation
     :request {:network nil
               :spirit (.-sprthash for)
               :requester (:source-address user-info)
               :name (:name user-info)
               :email (:e-mail user-info)
               :spell spell}}))

(defn- book-app-info [proc]
  (or (some->
       (.-sprthash proc)
       aii.Proto0.bookAppUrlForSpirit)
      (js/Promise.resolve nil)))

(defn- simulate [network for spell user-info]
  (let [{:keys [request-api simulate-api request]} (build-process-request network for spell user-info)
        request (clj->js request)]
    (-> (request-api request)
        (.then (fn [proc]
                 (let [instruction (aii.Helpers.formatCliInstructionIntoString (.-cli_instructions proc))]
                   (-> (book-app-info proc)
                       (.then (fn [book-app]
                                (-> (simulate-api request proc)
                                    (.then (fn [[result-type result]]
                                             (if (= result-type "SimulationFailed")
                                               (js/Promise.reject (get (js->clj result) "error_message"))
                                               (assoc (js->clj result :keywordize-keys true)
                                                      :instruction instruction
                                                      :sprthash (.-sprthash proc)
                                                      :book-app (js->clj book-app :keywordize-keys true)))))))))))))))

(defcommand check-operation-injection [inject-token]
  (aii.Proto0.checkOperationInjection #js {:injtoken inject-token}))

(defn- same-fee? [{:keys [networkfees templatefees rawamount]} fees]
  (and (= networkfees (:networkfees fees))
       (= templatefees (:templatefees fees))
       (= rawamount (:rawamount fees))))

(defcommand inject-operation [txn signature js-network]
  (aii.Proto0.injectOperation #js {:network js-network
                                   :unsigned_transaction txn
                                   :signature signature}))

(defn- inject [txn public-key source-address signature js-network]
  (-> (inject-operation txn signature js-network)
      (.then (fn [[result-type result]]
               (prn "pass" result-type result)
               (if (= result-type "InjectionFailed")
                 (js/Promise.reject (:error_message result))
                 (js->clj result :keywordize-keys true))))))

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
         [{:type :spell-verifier
           :label label
           :tmplhash tmplhash}] (generate-spell-verifier label tmplhash)
         [{:type :verify-spell-params
           :salabel salabel
           :tmplversion tmplversion
           :fieldValues fieldValues}] (verify-spell-params salabel tmplversion fieldValues)
         [{:type :description :sahash sahash :for for :spell spell}] (generate-description for spell sahash)
         [{:type :inject-operation :signature signature
           :txn txn :public-key public-key :source-address source-address
           :network js-network}] (inject txn public-key source-address signature js-network)
         [{:type :confirm-injection :injection-token injection-token :interval interval}] (waiting-for-done injection-token interval)
         :else (js/Promise.reject (str "unknown command" command))))

(re-frame/reg-fx
 :aii
 (fn [{:keys [commands] :as callback-ids}]
   (let [promise (js/Promise.all (map process-single commands))]
     (task/callback callback-ids promise))))
