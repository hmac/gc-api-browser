(ns load-test-om.response-time-chart
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn draw-points [el scales axes data-points]
  (let [svg (-> js/d3
                (.select el)
                (.select "svg"))
        line (-> js/d3
                 .-svg
                 .line
                 (.x #((:x scales) (.-x %)))
                 (.y #((:y scales) (.-y %))))
        data (->> data-points
                  (sort-by :time)
                  (map (fn [data-point]
                         #js {:x (js/Date. (:time data-point))
                              :y (:response-time data-point)})))]
    (-> svg
        (.select ".x.axis")
        (.call (:x axes)))

    (-> svg
        (.select ".y.axis")
        (.call (:y axes)))

    (-> svg
        (.select "path.line")
        (.datum (apply array data))
        (.attr "d" line))))

(defn get-scales [domain width height]
  {:x (-> js/d3
          .-time
          .scale
          (.domain (apply array (:x domain)))
          (.range #js [0 width]))
   :y (-> js/d3
          .-scale
          .linear
          (.domain (apply array (:y domain)))
          (.range #js [height 0]))})

(defn get-axes [scales]
  {:x (-> js/d3
          .-svg
          .axis
          (.scale (:x scales))
          (.orient "bottom"))
   :y (-> js/d3
          .-svg
          .axis
          (.scale (:y scales))
          (.orient "left"))})

(def min-max (juxt (partial apply min) (partial apply max)))

(defn domain [{:keys [data-points] :as load-test}]
  {:pre [(contains? load-test :stats)]
   :post [#(= 2 (count (:x %)))
          #(= 2 (count (:y %)))]}
  {:x (min-max (map :time data-points))
   :y (map (:stats load-test) [:min :max])})

(defn update-chart [el width height load-test]
  (let [scales (get-scales (domain load-test) width height)
        axes   (get-axes scales)]
    (draw-points el scales axes (:data-points load-test))))

(defn create-chart [el load-test]
  (let [width 446
        height 150
        top 20
        right 10
        bottom 30
        left 30
        svg (-> js/d3
                (.select el)
                (.append "svg")
                (.attr "class" "d3")
                (.attr "width" (+ width left right))
                (.attr "height" (+ height top bottom))
                (.append "g")
                (.attr "transform" (str "translate(" left "," top ")")))]
    (-> svg
        (.append "g")
        (.attr "class" "x axis")
        (.attr "transform" (str "translate(0," height ")")))

    (-> svg
        (.append "g")
        (.attr "class" "y axis")
        (.append "text")
        (.attr "transform" "rotate(-90)"))

    (-> svg
        (.append "path")
        (.attr "class" "line"))

    (update-chart el width height load-test)))

(defn response-time-chart [load-test owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (create-chart (om/get-node owner) load-test))

    om/IRender
    (render [_]
      (dom/div #js {:className "chart response-time-chart"}))))
