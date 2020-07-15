(ns tsca-webapp.mock)

(defn- clj->str [o]
  (-> o clj->js js/JSON.stringify))

(def testnet (clj->str {:netident "testnet" :chainid "NetXjD3HPJJjmcd"}))

(def sahash-frozen "MOCK_sahash_proto0_frozen_genesis")

(def target-spec-frozen (clj->str {:spellkind "spellofgenesis"
                                   :tmplversion "MOCK_tmplversion_proto0_frozen"}))
(def spell-frozen "{\"fund_owners\":[\"abc\"],\"fund_amount\":10,\"unfrozen\":\"2020-07-04T00:00:00Z\"}")
