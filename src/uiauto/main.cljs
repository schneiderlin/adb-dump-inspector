(ns uiauto.main
  (:require 
   [replicant.dom :as r]
   [clojure.walk :as walk] 
   [uiauto.uiauto-dev :as ui]))

(defn interpolate [event data]
  (walk/postwalk
   (fn [x]
     (case x
       :event/target.value (.. event -target -value)
       :event/target (.. event -target)
       :event/event event
       ;; Add more cases as needed
       x))
   data))

(defn execute-actions [store e actions]
  (doseq [[action & args] actions]
    (case action
      :store/assoc-in (apply swap! store assoc-in args)
      :store/update-in (apply swap! store update-in args)
      :event/prevent-default (.preventDefault e)
      :event/stop-propagation  (.stopPropagation e)
      :debug/print (js/console.log (clj->js args))
      :clipboard/copy (js/navigator.clipboard.writeText (first args))
      :toast/show (let [[target msg] args
                        bound (.getBoundingClientRect target)]
                    (swap! store (fn [state]
                                   (-> state
                                       (assoc :toast-msg msg)
                                       (assoc :toast-rect {:top (.-top bound)
                                                           :left (.-left bound)}))))
                    (js/setTimeout #(swap! store dissoc :toast-msg :toast-rect)
                                   500)) 
      (println "Unknown action" action "with arguments" args))))

(defn render-loop [store el]
  (add-watch
   store ::render
   (fn [_ _ _ state]
     (r/render el (ui/main state))))

  (r/set-dispatch!
   (fn [{:keys [replicant/dom-event]} actions]
     (->> actions
          (interpolate dom-event)
          (execute-actions store dom-event))))

  (reset! store
          (-> ui/example-data)))
 
(defonce store
  (atom nil))

(defonce el (js/document.getElementById "app"))

(defn ^:dev/after-load main []
  (render-loop store el))

