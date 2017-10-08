(ns gui.subs
  (:require [re-frame.core :as re-frame]

            [gui.chart :as chart]
            [gui.warnings :as warnings]))

;; ============== Config
(re-frame/reg-sub
 :config/currency
 (fn [db _]
   (:config/currency db)))

(re-frame/reg-sub
 :config/active-metric-tab
 (fn [db _]
   (:config/active-metric-tab db)))

(re-frame/reg-sub
 :config/active-chart-tab
 (fn [db _]
   (:config/active-chart-tab db)))

(re-frame/reg-sub
 :config/items-per-page
 (fn [db _]
   (:config/items-per-page db)))

(re-frame/reg-sub
 :config/chart-community-data-timestamp
 (fn [db _]
   (:config/chart-community-data-timestamp db)))

(re-frame/reg-sub
 :config/welcome-visible?
 (fn [db]
   (:welcome db)))

(defn is-number? [str]
  (re-find #"\d+" str))
(def url-regex
  "Taken from https://stackoverflow.com/a/5717133"
  (re-pattern
   #"(\?[;&a-z\d%_.~+=-]*)?((([a-z\d]([a-z\d-]*[a-z\d])*)\.)+[a-z]{2,}|((\d{1,3}\.){3}\d{1,3}))(\:\d+)?(/[-a-z\d%_.~+]*)*(\?[;&a-z\d%_.~+=-]*)?(\#[-a-z\d_]*)?$"))

(defn is-url? [str]
  (re-find url-regex str))

(re-frame/reg-sub
 :config/modal
 (fn [db _]
   (if-let [modal (:config/modal db)]
     (let [contents          (:data modal)
           filtered-keywords (filterv #(and (not (is-url? %))
                                            (not (is-number? %)))
                                      (:keywords contents))]
       (assoc-in modal [:data :keywords] filtered-keywords))
     nil)))

;; ============== Charts
(re-frame/reg-sub
 :charts/view-related-community-data
 (fn [db _]
   (let [data (get db :charts/related)])))

;; ============== Dashboard
(re-frame/reg-sub
 :dashboard/style
 (fn [db _]
   (:dashboard/style db)))

(defn- extract-first-val [data]
  (let [[val src] (:val data)]
    (assoc data :val val :src src)))

(defn- currency->val-and-currency [currency values units]
  (if-let [match (get values currency)]
    [match currency]
    (let [available-unit (first (keys values))
          val            (get values available-unit)]
      [val available-unit])))

(defn ->dashboard-val [data currency]
  (let [type          (:type data)
        cat           (:category data)
        values        (:values data)
        units         (:units data)
        [value unit]  (currency->val-and-currency currency values units)
        unit-str      (get units unit)
        obs-window    (:observation-window (:parameters data))
        ;; ignore change and bid-ask types, these need additional data
        warning-level (when-not (#{"change" "bid-ask"} type)
                        (warnings/get-warning-level cat type value))]
    (cond-> {:unit   unit-str
             :type   type
             :cat    cat
             :period obs-window
             :warning-level warning-level
             :val    value}
      ;; max = hashrate distribution
      (#{"low" "high" "max"} type) extract-first-val
      (#{"trend"} type)            (assoc :interval (:trend-interval data)))))

(re-frame/reg-sub
 :dashboard/metric
 (fn [db [_ category type]]
   (let [currency       @(re-frame/subscribe [:config/currency])
         dashboard-data (:dashboard/metrics db)
         cat            (get dashboard-data category)
         data           (get cat type)]
     (->dashboard-val data currency))))

(re-frame/reg-sub
 :dashboard/data
 (fn [db _]
   (:dashboard/metrics db)))

(re-frame/reg-sub
 :dashboard/metrics
 (fn [_ _]
   [(re-frame/subscribe [:config/currency])
    (re-frame/subscribe [:dashboard/data])])
 (fn [[currency dashboard-data] _]
   (reduce (fn [accumulator [category category-data]]
             (let [first-pass-category-data
                   (reduce (fn [acc [type type-data]]
                             (assoc acc
                                    type (->dashboard-val type-data currency)))
                           {}
                           category-data)
                   mean  (:mean first-pass-category-data)
                   second-pass-category-data
                   (if (= :price category)
                     (update-in first-pass-category-data
                                [:bid-ask] (fn [old]
                                             (assoc old
                                                    :warning-level
                                                    (warnings/bid-ask-spread (:val old)
                                                                             (:val mean)))))
                     first-pass-category-data)
                   trend (:trend first-pass-category-data)
                   finalized-category-data
                   (update-in second-pass-category-data
                              [:change] (fn [old]
                                          ;; TODO: move this logic into `warnings.cljs`
                                          (if (= category :unconfirmed-count)
                                            (assoc old
                                                   :warning-level
                                                   (cond
                                                     (and (pos? (:val trend))
                                                          (> (:val old) 20)) :alert
                                                     (> (:val old) 10)       :warning
                                                     :else                   nil))
                                            (assoc old
                                                   :warning-level
                                                   (cond
                                                     (and (neg? (:val trend))
                                                          (< (:val old) -20)) :alert
                                                     (< (:val old) -10)       :warning
                                                     :else                    nil)))))]
               (assoc accumulator
                      category finalized-category-data
                      (warnings/->warning-level-key category)
                      (warnings/for-category finalized-category-data))))
           {}
           (dissoc dashboard-data :meta))))

(re-frame/reg-sub
 :dashboard/last-update
 (fn [_ _]
   (re-frame/subscribe [:dashboard/data]))
 (fn [dashboard-data _]
   (get-in dashboard-data [:meta :timestamp])))

(re-frame/reg-sub
 :dashboard/warning-level
 (fn [_ _]
   (re-frame/subscribe [:dashboard/metrics]))
 (fn [dashboard-metrics _]
   (warnings/->highest-warning-level dashboard-metrics #{:price
                                                         :market
                                                         :block-interval
                                                         :transaction-count
                                                         :unconfirmed-count
                                                         :hashrate-distribution})))

(re-frame/reg-sub
 :community/data
 (fn [db [_ community-name]]
   (let [data           (:dashboard/community db)
         community-data (get data (case community-name
                                    :community/news   :news
                                    :community/reddit :submissions
                                    :community/forums :threads))]
     community-data)))

(re-frame/reg-sub
 :community/current-page
 (fn [db [_ community-name]]
   (get (:community/current-pages db) community-name 0)))

(re-frame/reg-sub
 :community/paginated-data
 (fn [[_ community-name] _]
   [(re-frame/subscribe [:community/data community-name])
    (re-frame/subscribe [:community/current-page community-name])
    (re-frame/subscribe [:config/items-per-page])])
 (fn [[community-data page offset] _]
   (let [start    (* page offset)
         next-end (+ start offset)
         lim      (count community-data)
         end      (js/Math.min lim
                               next-end)]
     {:items        (subvec community-data start end)
      :current-page page
      :has-next?    (< next-end lim)
      :has-prev?    (pos? start)})))

(defn ->chart-vals [data currency]
  (let [values             (:values data)
        [unit data-points] (if-let [v (get values currency)]
                             [currency v]
                             (first values))]
    (assoc (dissoc data :values)
           :data-points (clj->js data-points)
           :unit        unit)))

(re-frame/reg-sub
 :chart/raw-data
 (fn [db _]
   (:dashboard/graph db)))

(re-frame/reg-sub
 :chart/data
 (fn [_ _]
   [(re-frame/subscribe [:config/active-chart-tab])
    (re-frame/subscribe [:chart/raw-data])
    (re-frame/subscribe [:config/currency])])
 (fn [[chart-type data currency] _]
   (let [chart-data (assoc (case chart-type
                             :market-cap (select-keys (:market data)
                                                      [:mean
                                                       :volatility])
                             :transactions
                             (assoc {}
                                    :tx-count-mean (:mean (:transaction-count data))
                                    :unconfirmed-count-mean (:mean (:unconfirmed-count data)))

                             :interval                 (select-keys (:block-interval data)
                                                                    [:mean])
                             :median-confirmation-time (select-keys (:median-confirmation-time data)
                                                                    [:mean])
                             :difficulty               (select-keys (:network-difficulty data)
                                                                    [:max])
                             :price                    (select-keys (:price data)
                                                                    [:low
                                                                     :high
                                                                     :spread-mean])
                             :price-analysis           (select-keys (:price data)
                                                                    [:volatility
                                                                     :trend])
                             :price-bid-ask            (select-keys (:price data)
                                                                    [:bid-ask]))
                           ;; mean price is available to all charts
                           :mean-price (:mean (:price data)))]
     ;; TODO: attach actual unit to each data type
     (chart/config chart-type
                   (reduce (fn [acc [k v]]
                             (assoc acc k (->chart-vals v currency)))
                           {}
                           chart-data)))))

(re-frame/reg-sub
 :chart/related-community-data
 (fn [db _]
   (let [ts @(re-frame/subscribe [:config/chart-community-data-timestamp])]
     (assoc (get (:chart/community-data db) ts)
            :timestamp ts))))

;; ============== Internal
(re-frame/reg-sub
 :initialized?
 (fn [db _]
   (and (not (empty? db))
        (:dashboard/graph db)
        (:dashboard/community db)
        (:dashboard/metrics db))))
