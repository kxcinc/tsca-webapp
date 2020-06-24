(ns tsca-webapp.settings
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn load-edn [source key]
  (try
    (with-open [r (io/reader source)]
      (or (get (edn/read (java.io.PushbackReader. r)) key)
          (throw (RuntimeException. (str "fail to load " key " from " source)))))

    (catch java.lang.Exception e
      (throw (RuntimeException. (str "You must prepare '" source "' correctly\n"
                                     e))))))

(defmacro recaptcha-site-key []
  (load-edn "settings/recaptcha.edn" :site-key))

(defmacro recaptcha-secret-key []
  (load-edn "settings/recaptcha.edn" :secret-key))

