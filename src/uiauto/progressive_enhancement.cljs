(ns uiauto.progressive-enhancement
  (:require 
   [cljs.reader :as reader]))

(defn revive-map [el]
  (let [data (->> (.querySelector el "script")
                  .-innerText
                  reader/read-string)]
    (set! (.-innerHTML el) "")
    #_(map/mount-map el data)))

(defn main []
  (doseq [el (js/document.querySelectorAll "[data-client-feature]")]
    (case (.getAttribute el "data-client-feature")
      "marker-map"
      (revive-map el))))