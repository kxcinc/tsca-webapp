(ns tsca-webapp.book.subs
  (:require [re-frame.core :as re-frame]
            [tsca-webapp.common.subs-parts :as common]))

(def books [{:bookhash "bk1ddb9NMYHZi5UzPdzTZMYQQZoMub195zgv"
             :tmplhash  "Tp1gjaF81ZRRvdzjobyfVNsAeSC6PScjfQwN"
             :title "Book of Funny"
             :synopsis "Book of Funny is a demo-purpose contract template that creates a contract that sends back to the invoker half of its current balance every time it is invoked. It could be invoked by anyone so basically the originator is likely to be losing tokens. Do not use this template on networks with real tokens."
             :basic-facts {:provider
                           {:value "The TSCA Team"
                            :description (str "hoge fuga hoge fuga hoge fuga hoge fuga"
                                              "hoge fuga hoge fuga hoge fuga hoge fuga"
                                              "hoge fuga hoge fuga hoge fuga hoge fuga")}
                           :contract-complexity {:value "Very low" :description ":joy:"}
                           :certification-status {:value "Carefully reviewed" :description ":thinking_face"}
                           :template-fees {:template-provider 0.5
                                           :agency 0.5}}
             :template-details
             {:contract-parameters [{:param-name "initial_balance"
                                     :description "the initial balance of the contract to be originated"}]
              :contract-terms ["Any implicit account could submit a transaction to the blockchain which results in an invocation to the originated contract. When such an invocation being successful, the originated contract will transfer half of its current balance back to the implicit account who submitted the transaction."]
              :caveats ["DO NOT USE ON THE MAINNET: contracts originated from this template gives away its initial balance to anyone who requests, and is created solely for the purpose of demonstration. Therefore it is strongly advised against using this template on chains that doesn’t not have free tokens otherwise, especially the Tezos Mainnet."]
              :formal-specifications
              [{:title "Behavioral reference implementation in SCaml"
                :url   "https://tezos.foundation/"}
               {:title "Effective contract source code in Michelson"
                :url   "https://tezos.foundation/"}]}}

            {:bookhash "bk1faswCTDciRzE4oJ9jn2Vm2dvjeyA9fUzU"
             :tmplhash  "Tp1b7tUupMgCNw2cCLpKTkSD1NZzB5TkP2sv"
             :title "Book of Frozen"
             :synopsis "Book of Frozen creates a contract that freeze its initial balance until a configurable “unfrozen timestamp”"
             :basic-facts {:provider {:value "The TSCA Team"
                                      :description (str "hoge fuga foo bar "
                                                        "hoge fuga foo bar "
                                                        "hoge fuga foo bar "
                                                        "hoge fuga foo bar "
                                                        "hoge fuga foo bar "
                                                        "hoge fuga foo bar ")}
                           :contract-complexity {:value "Low" :description ":joy:"}
                           :certification-status {:value "Formally certified and mechanically verified" :description ":thinking_face:"}
                           :template-fees {:template-provider 1.5
                                           :agency 0.5}}
             :template-details
             {:contract-parameters
              [{:param-name "fund_amount"
                :description "the initial balance of the contract to be frozen"}
               {:param-name "unfrozen_timestamp"
                :description "the timestamp from which the frozen fund is released"}
               {:param-name "fund_owners"
                :description "the owners of the fund, each of which could request a withdraw of the fund after `unfrozen_timestamp`. Each entry must be an implicit account address."}]
              :contract-terms
              ["The `fund_amount` will be held frozen in the originated contract until the `unfrozen_timestamp`, before which it is impossible to withdraw fund from the contract and after which any account listed in `fund_owners` could request a withdraw."
               "No parameter of the contract could be amended after the origination of the contract."]
              :caveats
              ["There is absolutely no way to withdraw the frozen fund before `unfrozen_timestamp`. Think carefully before the origination."
               "DO NOT list smart contracts in the `fund_owners`: only implicit account could successfully request a withdraw."
               "Only accounts listed in the `fund_owners` could request withdraws from the fund after unfrozen. Be careful that you maintain control over at least one of those addresses after the unfrozen date."]
              :formal-specifications
              [{:title "Behavioral reference implementation in SCaml"
                :url "https://tezos.foundation/"}
               {:title "Technical Notes"
                :url "https://tezos.foundation/"}
               {:title "Source code and mechanized verification script in Coq"
                :url "https://tezos.foundation/"}]}}])

(re-frame/reg-sub
 ::books-summary
 (fn []
   (->> books
        (map (fn [{:keys [bookhash title synopsis]}]
               {:bookhash bookhash :title title :synopsis synopsis})))))

(re-frame/reg-sub
 ::book-info
 (fn [db]
   (let [{:keys [bookhash]} (:routing-params db)]
     (some->> books
              (filter #(= (:bookhash %) bookhash))
              first))))

(re-frame/reg-sub
 ::initial-agreements
 :<- [::book-info]
 (fn [{:keys [template-details]} _]
   (merge (common/make-array-same-element template-details :contract-terms false)
          (common/make-array-same-element template-details :caveats false))))

(re-frame/reg-sub
 ::expected-agreements
 :<- [::book-info]
 (fn [{:keys [template-details]} _]
   (merge (common/make-array-same-element template-details :contract-terms true)
          (common/make-array-same-element template-details :caveats true))))
