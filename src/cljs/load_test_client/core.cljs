(ns load-test-client.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.repl :as repl :include-macros true]
            [goog.events :as events]
            [goog.json :as gjson]
            [goog.net.XhrIo :as gxhr]
            [load-test-client.form :as form]
            [load-test-client.load-tests :as load-tests])
  (:import [goog.net XhrIo WebSocket]))

(defonce app-state
  (atom {:api {:http-url "http://localhost:3000/"
               :ws-url "ws://localhost:3000/"}
         :text "GoCardless Enterprise API Load Tester"
         :form {}
         :load-tests {}}))

(enable-console-print!)

(comment
  (get-in @app-state [:form])
  (:data-points (second (first (get-in @app-state [:load-tests]))))
  ;; how many data points in first load test
  (count (apply sorted-set-by :time (get-in @app-state [:load-tests :0 :data-points])))
  ;; look at a data-point
  (get-in @app-state [:load-tests :items 0 :data-points 0])
  ;; look at the first load-test's stats
  (get-in @app-state [:load-tests :items 0 :stats])
  ;; Correct order?
  (apply > (map
             (fn [load-test] (get-in load-test [:data-points 0 :time]))
             (get-in @app-state [:load-tests :items])))
  ;; which has the most data-points
  (->> (get-in @app-state [:load-tests :items])
       (sort-by (comp count :data-points) >)
       first
       :id)
  ;; show first load test stats
  (->> (get-in @app-state [:load-tests :items 0 :stats])))

(comment (identity @app-state))

(defn initial-selected-resource [resources]
  [(first (first resources))
   (first (second (first resources)))])

(defn handle-preset-response [app e]
  (let [json (js->clj (.. e -target getResponseJson))]
    (om/transact! app :form (fn [form]
                              (assoc form
                                     :resources (get json "resources")
                                     :url (get json "url")
                                     :duration (get json "duration")
                                     :rate (get json "rate")
                                     :selected-resource (initial-selected-resource (get json "resources")))))))

(defn handle-new-or-updated-load-test [app data]
  (om/transact! app :load-tests
                #(merge % (js->clj (gjson/parse data) :keywordize-keys true))))

(defn main []
  (om/root
    (fn [app owner]
      (reify
        om/IWillMount
        (will-mount [_]
          (let [ws (WebSocket.)
                ws-endpoint (str (-> app :api :ws-url) "load-tests")
                http-endpoint (str (-> app :api :http-url) "presets")]
            (gxhr/send http-endpoint (partial handle-preset-response app))
            (doto ws
              (events/listen WebSocket.EventType.MESSAGE #(handle-new-or-updated-load-test app (.-message %)))
              (.open ws-endpoint))
            (om/set-state! owner :load-tests-ws ws)))

        om/IWillUnmount
        (will-unmount [_]
          (.close (om/get-state owner :load-tests-ws)))

        om/IRender
        (render [_]
          (dom/div nil
                   (dom/header nil
                               (dom/div #js {:className "container"}
                                        (dom/h2 #js {:id "title"} (:text app))))
                   (dom/div #js {:className "container"}
                            (dom/div #js {:className "main"}
                                     (om/build form/component app)
                                     (dom/div #js {:className "hr"})
                                     (om/build load-tests/component (:load-tests app))))))))
    app-state
    {:target (. js/document (getElementById "app"))}))