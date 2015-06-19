(ns gc-api-browser.headers
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]))

(defn header-component [[header-name header-value] owner {:keys [handle-delete] :as opts}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "headers__header u-direction-row"}
               (dom/div #js {:className "headers__header__name u-margin-Rxxs"}
                        (dom/input #js {:className "input"
                                        :disabled true
                                        :value header-name}))
               (dom/div #js {:className "headers__header__value u-margin-Rxxs"}
                        (dom/input #js {:className "input"
                                        :disabled true
                                        :value header-value}))
               (dom/button #js {:type "button"
                                :className "headers__header__delete"
                                :onClick handle-delete} "✖")))))

(defn store-headers! [headers]
  (.setItem js/localStorage "headers" (.stringify js/JSON (clj->js headers))))

(defn delete-header [headers k]
  (om/transact! headers #(dissoc % k))
  (store-headers! @headers))

(def ENTER_KEY 13)

(defn handle-new-header-keydown [e headers owner]
  (when (== (.-which e) ENTER_KEY)
    (let [name-node (om/get-node owner "newHeaderNameField")
          value-node (om/get-node owner "newHeaderValueField")]
      (when-not (or (string/blank? (.. name-node -value trim))
                    (string/blank? (.. value-node -value trim)))
        (om/transact! headers #(assoc % (.-value name-node) (.-value value-node)))
        (set! (.-value name-node) "")
        (set! (.-value value-node) "")
        (.focus name-node)))
    (.preventDefault e)
    (store-headers! @headers)))

(defn component [headers owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:className "request-form--field headers"}
             (dom/div #js {:className "headers__header u-direction-row"}
                      (dom/div #js {:className "headers__header__name u-margin-Rxxs"}
                               (dom/input #js {:className "input"
                                               :ref "newHeaderNameField"
                                               :placeholder "Header Name"
                                               :onKeyDown #(handle-new-header-keydown % headers owner)}))
                      (dom/div #js {:className "headers__header__value u-margin-Rxxs"}
                               (dom/input #js {:className "input"
                                               :ref "newHeaderValueField"
                                               :placeholder "Header Value"
                                               :onKeyDown #(handle-new-header-keydown % headers owner)})))
             (map (fn [[k v]]
                    (om/build header-component [k v] {:opts {:handle-delete #(delete-header headers k)}}))
                  headers)))))
