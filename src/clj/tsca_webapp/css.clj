(ns tsca-webapp.css
  (:require [garden.def :refer [defstyles]]))

(defstyles screen
  [:.p            {:margin "0 0 1.2rem"}]
  [:.docs-content {:margin "36px"}]
  [:.gap          {:height "16px"}]
  [:.modal-large  {:width "90vw"
                   :height "95vh"
                   :max-width "none"
                   :max-height "none"
                   :overflow-y "none"}
   [:.modal-body  {:flex-grow 1
                   :margin "18px 0 0"
                   :padding 0}]
   [:iframe        {:width "100%"
                    :height "99%"}]]

  [:.text-small   {:font-size "80%"}]
  [:.monospace    {:font-family "monospace"}]
  [:.card         {:height "100%"}]
  [:.shape        {:width "18px" :height "18px"
                   :margin "2px 6px"
                   :line-height "1.5em"
                   :display "inline-block"}]
  [:.flexbox      {:display "flex"
                   :flex-wrap "wrap"}
   [:a   {:margin  "0 18px"}]]
  [:.popover-container.popover-large {:width "380px"}])