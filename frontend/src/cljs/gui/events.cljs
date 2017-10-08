(ns gui.events
  (:require [re-frame.core :as re-frame]

            [gui.db :as db]))

(defn toggle-style [db _]
  (update db :dashboard/style #(case %
                                 :community-focus :data-focus
                                 :community-focus)))

(re-frame/reg-event-db
 :dashboard/set-style
 (fn [db [_ style]]
   (assoc db :dashboard/style style)))

(re-frame/reg-event-db
 :dashboard/toggle-style
 toggle-style)

(re-frame/reg-event-db
 :prev-page
 (fn [db [_ page-type]]
   (update-in db [:community/current-pages
                  page-type]
              dec)))

(re-frame/reg-event-db
 :next-page
 (fn [db [_ page-type]]
   (update-in db [:community/current-pages
                  page-type]
              inc)))

(re-frame/reg-event-db
 :config/close-modal
 (fn [db _]
   (dissoc db :config/modal)))

(re-frame/reg-event-db
 :config/open-modal
 (fn [db [_ data]]
   (assoc db :config/modal data)))

(re-frame/reg-event-db
 :config/reset-modal-layer
 (fn [db _]
   (update-in db [:config/modal] dissoc :layer)))

(re-frame/reg-event-db
 :config/set
 (fn [db [_ key value]]
   (assoc db key value)))

(re-frame/reg-event-db
 :config/hide-welcome
 (fn [db _]
   (dissoc db :welcome)))

(re-frame/reg-event-db
 :load/data-available
 (fn [db [_ type data1 data2]]
   (if data2
     (assoc-in db [type data1] data2)
     (assoc db type data1))))

(re-frame/reg-event-fx
 :load/graph-data
 (fn [_ _]
   {:load-data {:url        "https://api.bitcoinseismograph.info/graph"
                :on-success #(re-frame/dispatch
                              [:load/data-available :dashboard/graph %])}}))

(re-frame/reg-event-fx
 :load/community-data
 (fn [_ _]
   {:load-data {:url        "https://api.bitcoinseismograph.info/text"
                :on-success #(re-frame/dispatch
                              [:load/data-available :dashboard/community %])}}))

(re-frame/reg-event-fx
 :load/dashboard-data
 (fn [_ _]
   {:load-data {:url        "https://api.bitcoinseismograph.info/dashboard"
                :on-success #(re-frame/dispatch
                              [:load/data-available :dashboard/metrics %])}}))

(re-frame/reg-event-fx
 :load/community-data-for-timestamp
 (fn [_ [_ timestamp]]
   {:dispatch  [:load/data-available
                :chart/community-data
                timestamp
                {:status    :progress/loading
                 :timestamp timestamp}]
    :load-data {:url          "https://api.bitcoinseismograph.info/text"
                :query-params {:timestamp timestamp}
                :on-success   #(re-frame/dispatch
                                ;; TODO: add caching
                                [:load/data-available
                                 :chart/community-data
                                 timestamp
                                 %])}}))

(re-frame/reg-event-db
 :display/hide-welcome
 (fn [db _]
   (dissoc db :welcome)))

(re-frame/reg-event-fx
 :load/community-data-from-graph
 (fn [_ [_ timestamp]]
   ;; c3 on-click is buggy: multiple events are thrown if there are overlapping elements
   ;; at the click location
   ;; we use debouncing with a 15ms timeout to ensure we only handle a single event per
   ;; actual user click
   {:scroll-into-view  "graphs" ;; ensure the community data section is visible
    :dispatch-debounce [{:id       :load/delayed
                         :timeout  15
                         :dispatch [:load/community-data-for-timestamp
                                    timestamp]}
                        {:id       :set/delayed
                         :timeout  15
                         :dispatch [:config/set
                                    :config/chart-community-data-timestamp
                                    timestamp]}]}))

(re-frame/reg-event-fx
 :load/refresh
 (fn [_ _]
   {:dispatch [:load/initial-data]}))

(re-frame/reg-event-fx
 :load/initial-data
 (fn [_ _]
   {:dispatch-n [[:load/graph-data]
                 [:load/community-data]
                 [:load/dashboard-data]]}))

(re-frame/reg-event-db
 :initialize-db
 (fn [db _]
   (merge db/initial-db db)))
