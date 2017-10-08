(ns gui.fx
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async]
            [goog.dom :as gdom]

            [cljs-http.client :as http]
            [re-frame.core :as re-frame]
            [re-frame.loggers]))

(re-frame/reg-fx
 :load-data
 (fn [{:keys [url query-params on-success on-err]}]
   ;; FIXME: how do I detect an error here?
   (go (let [resp (async/<! (http/get url {:with-credentials? false
                                           :query-params      query-params}))
             bod  (:body resp)]
         (on-success bod)))))

;; taken straight up from re-frame PR#249
(def debounced-events (atom {}))

(defn cancel-timeout [id]
  (js/clearTimeout (:timeout (@debounced-events id)))
  (swap! debounced-events dissoc id))

(re-frame/reg-fx
 :dispatch-debounce
 (fn [dispatches]
   (let [dispatches (if (sequential? dispatches) dispatches [dispatches])]
     (doseq [{:keys [id action dispatch timeout]
              :or   {action :dispatch}}
             dispatches]
                 (case action
                   :dispatch (do
                               (cancel-timeout id)
                               (swap! debounced-events assoc id
                                      {:timeout  (js/setTimeout
                                                  (fn []
                                                    (swap! debounced-events dissoc id)
                                                    (re-frame/dispatch dispatch))
                                                  timeout)
                                       :dispatch dispatch}))
                   :cancel   (cancel-timeout id)
                   :flush    (let [ev (get-in @debounced-events [id :dispatch])]
                               (cancel-timeout id)
                               (re-frame/dispatch ev))
                   (re-frame.loggers/console
                    :warn "re-frame: ignoring bad :dispatch-debounce action:" action "id:" id))))))

(re-frame/reg-fx
 :scroll-into-view
 (fn [id]
   (.scrollIntoView (gdom/getElement id))))
