(ns tsca-webapp.common.subs-parts)

(defn button-class [disabled?]
  (if disabled?
    "btn btn-primary disabled"
    "btn btn-primary"))

(defn make-array-same-element [m path v]
  {path (mapv (fn [_] v) (path m))})
