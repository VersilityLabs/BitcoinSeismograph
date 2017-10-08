(ns gui.chart
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]

            [gui.components :as components]
            [gui.format :refer [->date
                                ->min
                                ->number
                                ->number-with-fraction
                                ->short
                                smoothed-val->chart-label
                                with-unit]]
            [gui.listings :as listings]
            [gui.style :as style]))

(def c3 js/c3)

(defmulti config (fn [type _] type))
(defmethod config :price
  [_ data]
  (let [low         (:data-points (:low data))
        high        (:data-points (:high data))
        m           (:mean-price data)
        mean-unit   (get (:units m) (:unit m))
        mean        (:data-points m)
        spread-mean (:data-points (:spread-mean data))]
    (.unshift low #js["timestamp" "low" "low-source"])
    (.unshift high #js["timestamp" "high" "high-source"])
    (.unshift mean #js["timestamp" "mean"])
    (.unshift spread-mean #js["timestamp" "spread-mean"])
    {:data    [low high mean spread-mean]
     :options {:data    {:types  {"spread-mean" "bar"}
                         :colors {"mean"        style/turquoise
                                  "low"         style/green
                                  "high"        style/blue
                                  "spread-mean" style/grey}
                         :axes   {"mean"        "y"
                                  "spread-mean" "y2"}
                         :names  {"mean"        (smoothed-val->chart-label
                                                 "price"
                                                 "mean"
                                                 m)
                                  "low"         (smoothed-val->chart-label
                                                 "price"
                                                 "low"
                                                 (:low data))
                                  "high"        (smoothed-val->chart-label
                                                 "price"
                                                 "high"
                                                 (:high data))
                                  "spread-mean" (smoothed-val->chart-label
                                                 "price"
                                                 "spread-mean"
                                                 (:spread-mean data))}}
               :axis    {"y" {:tick  {:format (with-unit mean-unit {:fractionless? true})}
                              :label {:text     "Mean Price"
                                      :position "outer-middle"}}}
               :legend  {:hide ["low-source" "high-source"]}
               ;; TODO: add handling of low and high src!
               :tooltip {:format {:value (with-unit mean-unit)}}}}))
(defmethod config :price-analysis
  [_ data]
  (let [volatility (:data-points (:volatility data))
        trend      (:data-points (:trend data))]
    (.unshift volatility #js["timestamp" "volatility"])
    (.unshift trend #js["timestamp" "trend"])
    {:data    [volatility trend]
     :options {:data    {:colors {"volatility" style/turquoise
                                  "trend"      style/blue}
                         :axes   {"volatility" "y"
                                  "trend"      "y2"}
                         :names  {"volatility" (smoothed-val->chart-label
                                                "price"
                                                "volatility"
                                                (:volatility data))
                                  "trend"      (smoothed-val->chart-label
                                                "price"
                                                "trend"
                                                (:trend data))}}
               :axis    {"y"  {:tick  {:format (with-unit "%")}
                               :label {:text     "Price Volatility"
                                       :position "outer-middle"}}
                         "y2" {:tick  {:format (with-unit "%")}
                               :show  true
                               :label {:text     "Price Trend"
                                       :position "outer-middle"}}}
               :grid    {:y {:lines [{:value 0 :text ""}]}}
               :tooltip {:format {:value (with-unit "%")}}}}))
(defmethod config :price-bid-ask
  [_ data]
  (let [mean         (:data-points (:mean-price data))
        b-a          (:bid-ask data)
        bid-ask      (:data-points b-a)
        bid-ask-unit (get (:units b-a) (:unit b-a))]
    (.unshift mean #js["timestamp" "mean"])
    (.unshift bid-ask #js["timestamp" "bid-ask"])
    {:data    [mean bid-ask]
     :options {:legend  {:hide ["mean"]}
               :axis    {"y" {:tick  {:format (with-unit bid-ask-unit)}
                              :label {:text     "Bid-Ask Price"
                                      :position "outer-middle"}}}
               :data    {:colors {"bid-ask" style/turquoise
                                  "mean"    style/grey}
                         :axes   {"bid-ask" "y"
                                  "mean"    "y2"}
                         :names  {"bid-ask" (smoothed-val->chart-label
                                             "price"
                                             "bid-ask"
                                             (:bid-ask data))
                                  "mean"    (smoothed-val->chart-label
                                             "price"
                                             "mean"
                                             (:mean-price data))}}
               :tooltip {:format {:value (with-unit bid-ask-unit)}}}}))
(defmethod config :market-cap
  [_ data]
  (let [m-c                   (:mean data)
        market-cap-mean       (:data-points m-c)
        market-cap-mean-unit  (get (:units m-c) (:unit m-c))
        market-cap-volatility (:data-points (:volatility data))
        mean-formatter        (with-unit market-cap-mean-unit)
        volatility-formatter  (with-unit "%")]
    (.unshift market-cap-mean #js["timestamp" "mean"])
    (.unshift market-cap-volatility #js["timestamp" "volatility"])
    {:data    [market-cap-mean market-cap-volatility]
     :options {:data    {:colors {"mean"       style/turquoise
                                  "volatility" style/blue}
                         :axes   {"mean"       "y"
                                  "volatility" "y2"}
                         :names  {"mean"       (smoothed-val->chart-label
                                                "market"
                                                "mean"
                                                m-c)
                                  "volatility" (smoothed-val->chart-label
                                                "market"
                                                "volatility"
                                                (:volatility data))}}
               :axis    {"y"  {:tick  {:format (with-unit market-cap-mean-unit {:shorten? true})}
                               :label {:text     "Mean Market Cap"
                                       :position "outer-middle"}}
                         "y2" {:tick  {:format (with-unit "%")}
                               :label {:text     "Market Cap Volatility"
                                       :position "outer-middle"}
                               :show  true}}
               :tooltip {:format {:value
                                  (fn [val _ id idx]
                                    (if (= id "mean")
                                      (mean-formatter val)
                                      (volatility-formatter val)))}}}}))
(defmethod config :transactions
  [_ data]
  (let [tx-count          (:data-points (:tx-count-mean data))
        unconfirmed-count (:data-points (:unconfirmed-count-mean data))]
    (.unshift tx-count #js["timestamp" "transaction-count"])
    (.unshift unconfirmed-count #js["timestamp" "unconfirmed-count"])
    {:data    [tx-count unconfirmed-count]
     :options {:data    {:colors {"unconfirmed-count" style/turquoise
                                  "transaction-count" style/green}
                         :axes   {"transaction-count" "y"
                                  "unconfirmed-count" "y2"}
                         :names  {"transaction-count" (smoothed-val->chart-label
                                                       "transaction-count"
                                                       "mean"
                                                       (:tx-count-mean data))
                                  "unconfirmed-count" (smoothed-val->chart-label
                                                       "unconfirmed-count"
                                                       "mean"
                                                       (:unconfirmed-count-mean data))}}
               :axis    {"y"  {:tick  {:format (with-unit "" {:shorten? true})}
                               :label {:text     "Transaction Count"
                                       :position "outer-middle"}}
                         "y2" {:tick  {:format (with-unit "" {:shorten? true})}
                               :label {:text     "Unconfirmed Transaction Count"
                                       :position "outer-middle"}
                               :show  true}}
               :tooltip {:format {:value (with-unit "" {:fractionless? true})}}}}))
(defmethod config :interval
  [_ data]
  (let [interval             (:data-points (:mean data))
        m-p                  (:mean-price data)
        mean-price           (:data-points m-p)
        mean-price-unit      (get (:units m-p) (:unit m-p))
        mean-price-formatter (with-unit mean-price-unit)]
    (.unshift mean-price #js["timestamp" "mean-price"])
    (.unshift interval #js["timestamp" "interval"])
    {:data    [mean-price interval]
     :options {:legend  {:hide ["mean-price"]}
               :data    {:colors {"interval"   style/turquoise
                                  "mean-price" style/grey}
                         :axes   {"interval"   "y"
                                  "mean-price" "y2"}
                         :names  {"mean-price" (smoothed-val->chart-label
                                                "price"
                                                "mean"
                                                (:mean-price data))
                                  "interval"   (smoothed-val->chart-label
                                                "block-interval"
                                                "mean"
                                                (:mean data))}}
               :axis    {"y" {:tick  {:format ->min}
                              :label {:text     "Interval between Blocks"
                                      :position "outer-middle"}}}
               :grid    {"y" {:lines [{:value 600
                                       :text  "10min target"}]}}
               :tooltip {:format {:value
                                  (fn [val _ id idx]
                                    (if (= id "mean-price")
                                      (mean-price-formatter val)
                                      (->min val)))}}}}))
;; FIXME: find a way to fit the median confirmation time data to the mean price data
(defmethod config :median-confirmation-time
  [_ data]
  (let [median-confirmation-time (:data-points (:mean data))
        m-p                      (:mean-price data)
        ;; mean-price               (:data-points m-p)
        ;; mean-price-unit          (get (:units m-p) (:unit m-p))
        ;; mean-price-formatter     (with-unit mean-price-unit)
        ]
    #_(.unshift mean-price #js["timestamp" "mean-price"])
    (.unshift median-confirmation-time #js["timestamp" "median-confirmation-time"])
    {:data    [#_mean-price median-confirmation-time]
     :options {:legend  {:hide ["mean-price"]}
               :axis    {"y" {:tick  {:format ->min}
                              :label {:text     "Median Confirmation Time"
                                      :position "outer-middle"}}}
               :data    {:colors {"median-confirmation-time" style/turquoise
                                  ;; "mean-price"               style/grey
                                  }
                         :axes   {"median-confirmation-time" "y"
                                  ;; "mean-price"               "y2"
                                  }
                         :names  {"median-confirmation-time" (smoothed-val->chart-label
                                                              "median-confirmation-time"
                                                              "mean"
                                                              (:mean data))}}
               :tooltip {:format {:value ->min}}}}))
(defmethod config :difficulty
  [_ data]
  (let [difficulty           (:data-points (:max data))
        m-p                  (:mean-price data)
        mean-price           (:data-points m-p)
        mean-price-unit      (get (:units m-p) (:unit m-p))
        mean-price-formatter (with-unit mean-price-unit)]
    (.unshift mean-price #js["timestamp" "mean-price"])
    (.unshift difficulty #js["timestamp" "network-difficulty"])
    {:data    [mean-price difficulty]
     :options {:legend  {:hide ["mean-price"]}
               :axis    {"y" {:tick  {:format (with-unit "" {:shorten? true})}
                              :label {:text     "Network Difficulty"
                                      :position "outer-middle"}}}
               :data    {:colors {"network-difficulty" style/turquoise
                                  "mean-price"         style/grey}
                         :axes   {"network-difficulty" "y"
                                  "mean-price"         "y2"}
                         :names  {"mean-price"         (smoothed-val->chart-label
                                                        "price"
                                                        "mean"
                                                        (:mean-price data))
                                  "network-difficulty" (smoothed-val->chart-label
                                                        "network-difficulty"
                                                        "max"
                                                        (:max data))}}
               :tooltip {:format {:value
                                  (fn [val _ id idx]
                                    (if (= id "mean-price")
                                      (mean-price-formatter val)
                                      ((with-unit "") val)))}}}}))

(defn- chart [config]
  (let [it              (atom nil)
        default-options {:data    {:xFormat "%Y-%m-%dT%H:%M:%SZ"
                                   :x       "timestamp"
                                   :onclick
                                   (fn [d _]
                                     (re-frame/dispatch [:load/community-data-from-graph
                                                         (-> d .-x .getTime)]))}
                         :axis    {:x {:type      "timeseries"
                                       :localtime false
                                       :tick      {:format ->date}}}
                         :tooltip {:format {:title ->date}}}]
    (reagent/create-class
     {:component-did-mount
      (fn [this]
        (let [opts (merge-with into default-options (:options config))
              g    (c3.generate (clj->js (assoc-in opts
                                                   [:data :rows]
                                                   (first (:data config)))))]
          (doseq [p (rest (:data config))]
            (.load g #js{"rows" p}))
          (reset! it g)))
      :component-did-update
      (fn [this x]
        (let [g @it]
          (.destroy g)
          (let [{:keys [data
                        options]} (reagent/props this)
                opts              (merge-with into default-options options)
                g                 (c3.generate
                                   (clj->js (assoc-in opts
                                                      [:data :rows]
                                                      (first data))))]
            (doseq [p (rest data)]
              (.load g #js{"rows" p}))
            (reset! it g))))
      :reagent-render
      (fn [config]
        [:div#chart])})))

(defn charts []
  (let [conf (re-frame/subscribe [:chart/data])]
    (fn []
      [:div#graphs
       [:div.container
        [:div.title.is-3.has-text-centered
         "Quantitative Data of the last 30 days"
         [:small.has-small-margin-left
          [components/info-with-tooltip :graphs]]]
        [:div.tabs
         [:ul
          [components/tab "Price"
           :price :config/active-chart-tab]
          [components/tab "Price Analysis"
           :price-analysis :config/active-chart-tab]
          [components/tab "Bid-Ask Spread"
           :price-bid-ask :config/active-chart-tab]
          [components/tab "Market Cap"
           :market-cap :config/active-chart-tab]
          [components/tab "Transactions"
           :transactions :config/active-chart-tab]
          [components/tab "Interval"
           :interval :config/active-chart-tab]
          [components/tab "Median Confirmation Time"
           :median-confirmation-time :config/active-chart-tab]
          [components/tab "Difficulty"
           :difficulty :config/active-chart-tab]]]
        [chart @conf]]])))

(defn community-data []
  (let [ts (re-frame/subscribe [:chart/related-community-data])]
    (fn []
      [:div#historical-community-data
       (cond
         (= :progress/loading (:status @ts))
         [:div.container.placeholder
          [:div.content.has-text-centered
           [:span.icon.is-large
            [:i.fa.fa-circle-o-notch.fa-spin.fa-3x.fa-fw]]]]

         (nil? (:timestamp @ts))
         [:div.container
          [:div.message
           [:div.message-body
            "Click on a data-point to view related community data."]]]

         :else
         [:div.container
          [:div.title.is-3.has-text-centered
           "Most discussed / viewed community contributions on " (->date (:timestamp @ts))]
          [:div.columns
           [:div.column.is-one-third
            [:div.title.is-5
             "Latest Bitcoin News"
             [:small.has-small-margin-left
              [components/info-with-tooltip :news]]]
            [:div.listing
             [listings/items :communiy/historical-data (:news @ts)]]]
           [:div.column.is-one-third
            [:div.title.is-5
             "Top Reddit Submissions"
             [:small.has-small-margin-left
              [components/info-with-tooltip :reddit]]]
            [:div.listing
             [listings/items :communiy/historical-data (:submissions @ts)]]]
           [:div.column.is-one-third
            [:div.title.is-5
             "Most Active Forum Threads"
             [:small.has-small-margin-left
              [components/info-with-tooltip :forums]]]
            [:div.listing
             [listings/items :communiy/historical-data (:threads @ts)]]]]])])))
