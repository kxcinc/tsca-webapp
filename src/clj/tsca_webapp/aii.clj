(ns tsca-webapp.aii)

(defmacro defcommand [name args & body]
  `(defn- ~name ~args (-> ~@body
                          (cljs.core/js->clj :keywordize-keys true)
                          (js/Promise.resolve))))


