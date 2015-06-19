(ns gc-api-browser.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [gc-api-browser.request :as request]
            [gc-api-browser.response :as response]
            [gc-api-browser.schema-select :as schema-select]))

(def default-headers {"Authorization"      "FILL ME IN"
                      "GoCardless-Version" "2015-04-29"
                      "Accept"             "application/json"
                      "Content-Type"       "application/json"})

(def init-app-state
  {:history []
   :request {:text              "Select a JSON schema..."
             :selected-resource nil
             :selected-action   nil
             :url               nil
             :method            "GET"
             :body              nil
             :headers           {}}})

(defonce app-state
  (atom init-app-state))


(enable-console-print!)

;; handy repl functions
(comment
  (keys @app-state)
  (swap! app-state (fn [] init-app-state))
  (do
    (.removeItem js/localStorage "schema")
    (.removeItem js/localStorage "headers"))
  (swap! app-state (fn [x] (update-in x [:request] (fn [y] (dissoc y :schema))))))

(defn handle-new-response [app resp]
  (om/transact! app (fn [m]
                      (-> m
                          (assoc :response resp)
                          (update :history #(conj % (select-keys m [:request :response])))))))

(defn render-request-and-response [app]
  (dom/div nil
           (om/build request/component (:request app)
                     {:opts {:handle-new-response-fn (partial handle-new-response app)}})
           (when-let [resp (:response app)]
             (om/build response/component resp))))

(defn main []
  (om/root
    (fn [app _]
      (reify
        om/IWillMount
        (will-mount [_]
          ;; Hack! See https://github.com/omcljs/om/issues/336
          (js/setTimeout
            (fn []
              (when-let [json (.getItem js/localStorage "schema")]
                (schema-select/set-schema! (:request app) (.parse js/JSON json))))))
        om/IDidMount
        (did-mount [_]
          ;; Hack! See https://github.com/omcljs/om/issues/336
          (js/setTimeout
            (fn []
              (om/update! app [:request :headers]
                          (or (js->clj (.parse js/JSON (.getItem js/localStorage "headers")))
                              default-headers)))))
        om/IRender
        (render [_]
          (dom/div #js {:style #js {:minWidth "760px" :margin "0 auto"}}
                   (dom/header nil
                               (dom/h2 #js {:className "u-text-light u-margin-Am u-text-center"}
                                       (get-in app [:request :text])))
                   (if (get-in app [:request :schema])
                     (render-request-and-response app)
                     (schema-select/schema-file (:request app)))))))
    app-state
    {:target (.getElementById js/document "app")}))
