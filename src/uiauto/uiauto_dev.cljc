(ns uiauto.uiauto-dev 
  (:require
   [clojure.data.xml :as xml]
   #?(:clj [clojure.java.io :as io])
   #?(:cljs [uiauto.utils :include-macros true :refer [slurp]])
   [replicant.alias :refer [defalias]]
   [clojure.zip :as zip]
   [clojure.walk :as walk]))

(defn walk-xml [node]
  (when (map? node)
    (let [children (filter map? (:content node))]
      (assoc node :content (map walk-xml children)))))

(defn parse-bound [bound-str]
  (let [[_ x1 y1 x2 y2]
        (re-matches #"\[(\d+),(\d+)\]\[(\d+),(\d+)\]" bound-str)
        [x1 y1 x2 y2] (map parse-long [x1 y1 x2 y2])]
    {:left x1
     :top y1
     :width (- x2 x1)
     :height (- y2 y1)}))

(defalias user-rect [{:keys [bounds
                             active?
                             id
                             path]}
                     content] 
  (if bounds
    (let [{:keys [left top width height]} (parse-bound bounds)]
      [:div {:id id
             :on {:click
                  [[:event/stop-propagation]
                   [:debug/print "click" id]
                   [:store/update-in [:expand-ids]
                    (fn [expand-ids]
                      (clojure.set/union expand-ids (into #{} (conj path id))))]
                   [:store/assoc-in [:active-id] id]]}
             :style (if active?
                      {:position "fixed"
                       :left (str left "px")
                       :top (str top "px")
                       :width (str width "px")
                       :height (str height "px")
                       :background "rgba(255,0,0,0.3)"
                       :border "solid #f00"
                       :box-sizing "border-box"}
                      {:position "fixed"
                       :left (str left "px")
                       :top (str top "px")
                       :width (str width "px")
                       :height (str height "px")})}
       content])
    [:div {}
     content]))

(defalias device-rect [{:keys [width height]}]
  [:div {:style {:width (str width "px")
                 :height (str height "px")
                 :position "relative"
                 :background "#eee"
                 :border "solid #444"
                 :box-sizing "border-box"}}
   (let [r (:rect {:left 0
                   :top 0
                   :width width
                   :height height})]
     [:div {:style {:position "absolute"
                    :left (str (:left r) "px")
                    :top (str (:top r) "px")
                    :width (str (:width r) "px")
                    :height (str (:height r) "px")
                    :background "rgba(0,128,255,0.3)"
                    :border "solid #08f"}}])])

;; TODO tree 全部展开如果太长了, 需要加滚动
(def example-xml
  #?(:clj (-> (io/file "example.xml")
              (io/reader)
              (xml/parse)
              (walk-xml))
     :cljs (-> "example.xml"
               slurp
               xml/parse-str
               (walk-xml))))

(defn copy-icon [props]
  [:svg (merge {:xmlns "http://www.w3.org/2000/svg"
                :shape-rendering "geometricPrecision"
                :text-rendering "geometricPrecision"
                :image-rendering "optimizeQuality"
                :fill-rule "evenodd"
                :clip-rule "evenodd"
                :viewBox "0 0 438 511.52"}
               props)
   [:path
    {:fill-rule "nonzero"
     :d
     "M141.44 0h172.68c4.71 0 8.91 2.27 11.54 5.77L434.11 123.1a14.37 14.37 0 0 1 3.81 9.75l.08 251.18c0 17.62-7.25 33.69-18.9 45.36l-.07.07c-11.67 11.64-27.73 18.87-45.33 18.87h-20.06c-.3 17.24-7.48 32.9-18.88 44.29-11.66 11.66-27.75 18.9-45.42 18.9H64.3c-17.67 0-33.76-7.24-45.41-18.9C7.24 480.98 0 464.9 0 447.22V135.87c0-17.68 7.23-33.78 18.88-45.42C30.52 78.8 46.62 71.57 64.3 71.57h12.84V64.3c0-17.68 7.23-33.78 18.88-45.42C107.66 7.23 123.76 0 141.44 0zm30.53 250.96c-7.97 0-14.43-6.47-14.43-14.44 0-7.96 6.46-14.43 14.43-14.43h171.2c7.97 0 14.44 6.47 14.44 14.43 0 7.97-6.47 14.44-14.44 14.44h-171.2zm0 76.86c-7.97 0-14.43-6.46-14.43-14.43 0-7.96 6.46-14.43 14.43-14.43h136.42c7.97 0 14.43 6.47 14.43 14.43 0 7.97-6.46 14.43-14.43 14.43H171.97zM322.31 44.44v49.03c.96 12.3 5.21 21.9 12.65 28.26 7.8 6.66 19.58 10.41 35.23 10.69l33.39-.04-81.27-87.94zm86.83 116.78-39.17-.06c-22.79-.35-40.77-6.5-53.72-17.57-13.48-11.54-21.1-27.86-22.66-48.03l-.14-2v-64.7H141.44c-9.73 0-18.61 4-25.03 10.41C110 45.69 106 54.57 106 64.3v319.73c0 9.74 4.01 18.61 10.42 25.02 6.42 6.42 15.29 10.42 25.02 10.42H373.7c9.75 0 18.62-3.98 25.01-10.38 6.45-6.44 10.43-15.3 10.43-25.06V161.22zm-84.38 287.11H141.44c-17.68 0-33.77-7.24-45.41-18.88-11.65-11.65-18.89-27.73-18.89-45.42v-283.6H64.3c-9.74 0-18.61 4-25.03 10.41-6.41 6.42-10.41 15.29-10.41 25.03v311.35c0 9.73 4.01 18.59 10.42 25.01 6.43 6.43 15.3 10.43 25.02 10.43h225.04c9.72 0 18.59-4 25.02-10.43 6.17-6.17 10.12-14.61 10.4-23.9z"}]])

(defn arrow-icon [props]
  [:svg (merge {:xmlns "http://www.w3.org/2000/svg"
                :shape-rendering "geometricPrecision"
                :text-rendering "geometricPrecision"
                :image-rendering "optimizeQuality"
                :fill-rule "evenodd"
                :clip-rule "evenodd"
                :viewBox "0 0 512 512"} props)
   [:path
    {:fill-rule "nonzero"
     :d
     "M156.92 231.12a9.776 9.776 0 0 1-.12-13.91c3.85-3.88 10.15-3.93 14.06-.12l86.31 83.71 83.89-83.63c3.87-3.85 10.16-3.87 14.06-.04a9.778 9.778 0 0 1 .04 13.92l-90.85 90.56c-3.85 3.83-10.12 3.87-14.02.07l-93.37-90.56zM256 512c-70.69 0-134.69-28.66-181.02-74.98C28.66 390.7 0 326.69 0 256c0-70.69 28.66-134.7 74.98-181.02C121.31 28.66 185.31 0 256 0c70.7 0 134.7 28.65 181.02 74.98C483.35 121.3 512 185.31 512 256c0 70.69-28.66 134.7-74.98 181.02C390.7 483.34 326.69 512 256 512zM89.08 422.92c42.72 42.71 101.73 69.14 166.92 69.14 65.19 0 124.21-26.42 166.92-69.14 42.71-42.71 69.14-101.73 69.14-166.92 0-65.19-26.42-124.21-69.14-166.92C380.21 46.36 321.19 19.94 256 19.94c-65.19 0-124.2 26.43-166.92 69.14C46.36 131.79 19.94 190.81 19.94 256c0 65.19 26.43 124.21 69.14 166.92z"}]])

(defn ui-attrs [attrs]
  [:div {:id "attrs"
         :class "w-1/3"}
   (for [[k v] attrs]
     [:div {:class ["flex" "justify-between"]}
      [:label k]
      [:div {:class ["flex"]}
       [:span v]
       (copy-icon {:class ["w-4" "h-4"]
                   :on {:click [[:clipboard/copy v]
                                [:toast/show :event/target "Copied"]]}})]])])

(defn subtree [{:keys [expand-ids active-id]
                :as state} path node]
  (let [{:keys [attrs content id]} node
        {:keys [class]} attrs
        expand? (get expand-ids id)]
    [:div
     ;; current node
     [:div {:class ["flex" "hover:bg-indigo-400"]}
      (arrow-icon {:class ["w-4" "h-4" (when (not expand?) "rotate-270")]
                   :on {:click [[:debug/print id] 
                                [:store/update-in [:expand-ids]
                                 (fn [expand-ids] 
                                   (if expand?
                                     (clojure.set/difference expand-ids #{id}) 
                                     (clojure.set/union expand-ids (into #{} (conj path id)))))]]}})
      [:p {:on {:click [[:store/assoc-in [:current-attrs] attrs]
                        [:debug/print "set-active" id]
                        [:store/assoc-in [:active-id] id]]}}
       class]]
     ;; children 
     (when expand?
       [:div {:style {:padding-left "20px"}}
        (map (fn [child-node]
               (subtree state (conj path id) child-node)) content)])]))

(comment
 (clojure.set/union #{1 } #{3}) 
  (clojure.set/difference #{1 2} #{1})
  :rcf)

(defn xml->data [xml]
  (let [xml (walk/postwalk
             (fn [node] (if (and (map? node) (:tag node))
                          (assoc node :id (str (random-uuid)))
                          node))
             xml)
        root-attrs (:attrs xml)] 
    {:root [::device-rect root-attrs] 
     :active-nodes []
     :xml xml
     :current-attrs {}}))

(comment 
  (xml->data example-xml) 
  :rcf)

(def example-data
  (xml->data example-xml))

(defn tree-of-rects [xml active-id path] 
  (let [{:keys [attrs content id]} xml] 
    [::user-rect (assoc attrs 
                        :active? (= (:id xml) active-id)
                        :id id
                        :active-id active-id
                        :path path)
     (map #(tree-of-rects % active-id (conj path id)) content)]))

(comment
  (tree-of-rects {:attr {:id :root}
                  :id :root
                  :content [{:attr {}
                             :id :1
                             :content []}
                            {:attr {:id :root}
                             :id :2
                             :content []}]}
                 :1
                 [])
  :rcf)

(defn main [state]
  (let [xml (:xml state)
        tree (subtree state [] xml)
        active-id (:active-id state)]
    [:div {:id "main"
           :class ["flex" "h-full" "w-full" "gap-4" "p-4"]}
     ;; 弹窗
     (when-let [msg (:toast-msg state)]
       (let [rect (:toast-rect state)
             {:keys [top left]} rect]
         [:div {:class ["fixed" "left-1/2"
                        "bg-gray-400" "p-2" "rounded-lg" "shadow-lg"
                        "z-50"]
                :style {:left left
                        :top top}}
          msg]))
     ;; 截图 
     [:div {:id "container"
            :style {:position "relative"
                    :width 720
                    :-webkit-transform "rotate(0deg)"; 
                    :-moz-transform "rotate(0deg)";
                    :-ms-transform "rotate(0deg)";
                    :-o-transform "rotate(0deg)";
                    :transform "rotate(0deg)"
                    :height 1280
                    :background-image "url('/example.png')"
                    :margin-left "auto"
                    :margin-right "auto"}}
      (tree-of-rects xml active-id [])]
     ;; 属性
     (ui-attrs (:current-attrs state))
     ;; 树
     tree]))

