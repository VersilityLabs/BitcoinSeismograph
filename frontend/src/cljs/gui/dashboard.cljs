(ns gui.dashboard
  (:require [re-frame.core :as re-frame]

            [gui.components :as components]
            [gui.format :refer [->duration
                                ->label
                                raw-with-unit
                                warning->class
                                warning->hiccup-attrs]]
            [gui.listings :as listings]
            [gui.tiles :as tiles]
            [gui.warnings :as warnings]))

(defmulti tabbed-kpi-contents (fn [type _] type))
(defmethod tabbed-kpi-contents :price
  [_ data]
  (let [price (:price data)]
    [:div.tile.is-vertical
     ^{:key "price-summary"} [tiles/tile :summary
                              :price price]
     ^{:key "price-bracket"} [tiles/tile :price-bracket
                              price]
     ^{:key "price-extended-volatility"} [tiles/tile :volatility
                                          price]]))
(defmethod tabbed-kpi-contents :market
  [_ data]
  (let [markets (:market data)]
    [:div.tile.is-vertical
     ^{:key "market-cap"} [tiles/tile :summary
                           :market markets]
     ^{:key "market-bracket"} [tiles/tile :low-high
                               markets]
     ^{:key "market-volatility"} [tiles/tile :volatility
                                  markets]]))
(defmethod tabbed-kpi-contents :blockchain
  [_ data]
  (let [bi           (:block-interval data)
        tx-count     (:transaction-count data)
        unconf-count (:unconfirmed-count data)
        pools        (:hashrate-distribution data)]
    [:div.tile.is-vertical
     [tiles/tile :block-interval
      bi]
     [tiles/tile :summary
      :unconfirmed-count unconf-count]
     [tiles/tile :summary
      :transaction-count tx-count]
     [tiles/tile :volatility
      tx-count]
     [tiles/tile :pools
      pools]]))

(defn- tabbed-kpis [data]
  (let [active-tab (re-frame/subscribe [:config/active-metric-tab])]
    (fn [data]
      (let [price-warning   (get data (warnings/->warning-level-key :price))
            markets-warning (get data (warnings/->warning-level-key :market))
            network-warning (warnings/->highest-warning-level data [:block-interval
                                                                    :transaction-count
                                                                    :unconfirmed-count
                                                                    :hashrate-distribution])]
        [:div
         [:div.tabs.is-centered
          [:ul
           [components/tab "Price"
            :price :config/active-metric-tab (warning->class price-warning)]
           [components/tab "Markets"
            :market :config/active-metric-tab (warning->class markets-warning)]
           [components/tab "Network"
            :blockchain :config/active-metric-tab (warning->class network-warning)]]]
         (tabbed-kpi-contents @active-tab data)]))))

(defn- community-focus [data]
  (fn [data]
    [:div.columns
     [:div.column.is-one-quarter
      [:div.title.is-5
       "Latest Bitcoin News"
       [:small.has-small-margin-left
        [components/info-with-tooltip :news :bottom]]]
      [listings/listing :community/news :dashboard]]
     [:div.column.is-one-quarter
      [:div.title.is-5
       "Top Reddit Submissions"
       [:small.has-small-margin-left
        [components/info-with-tooltip :reddit :bottom]]]
      [listings/listing :community/reddit :dashboard]]
     [:div.column.is-one-quarter
      [:div.title.is-5
       "Most Active Forum Threads"
       [:small.has-small-margin-left
        [components/info-with-tooltip :forums :bottom]]]
      [listings/listing :community/forums :dashboard]]
     [:div.column.is-one-quarter
      [tabbed-kpis data]]]))

(defn- data-focus [data]
  (fn [data]
    [:div.columns
     [:div.column.is-one-quarter
      [:div.title.is-5
       "Top Reddit Submissions"
       [:small.has-small-margin-left
        [components/info-with-tooltip :reddit :bottom]]]
      [listings/listing :community/reddit :dashboard]]

     [:div.column.is-three-quarter
      (let [price (:price data)]
        [:div.content
         [:div.title.is-5
          {:class (warning->class (get data (warnings/->warning-level-key :price)))}
          "Price"]
         [:div.card
          [:div.card-content.is-paddingless
           [:div.tile.is-parent
            [tiles/vertical (:mean price) {:width 3}]
            [tiles/vertical (:high price) {:width 3}]
            [tiles/vertical (:low price) {:width 3}]
            [tiles/vertical (:bid-ask price) {:width 3}]]
           [:div.tile.is-parent
            [tiles/vertical (:change price) {:width 3 :indicator true}]
            [tiles/vertical (:trend price) {:width 3 :indicator true}]
            [tiles/vertical-volatility-ext price 3]]]]])

      ;; Markets
      (let [markets (:market data)]
        [:div.content
         [:p.title.is-5
          {:class (warning->class (get data (warnings/->warning-level-key :market)))}
          "Markets"]
         [:div.card
          [:div.card-content.is-paddingless
           [:div.tile.is-parent
            [tiles/vertical (:mean markets) {:width 3}]
            [tiles/vertical (:high markets) {:width 3}]
            [tiles/vertical (:low markets) {:width 3}]]
           [:div.tile.is-parent
            [tiles/vertical (:change markets) {:width 3 :indicator true}]
            [tiles/vertical (:trend markets) {:width 3 :indicator true}]
            [tiles/vertical-volatility-ext markets 6]]]]])

      ;; Network
      (let [block-interval (:block-interval data)
            tx-count       (:transaction-count data)
            unconf-count   (:unconfirmed-count data)
            pools          (:hashrate-distribution data)
            difficulty     (:network-difficulty data)
            height         (:block-height data)]
        [:div.content
         [:p.title.is-5
          {:class (warning->class
                   (warnings/->highest-warning-level data [:block-interval
                                                           :transaction-count
                                                           :unconfirmed-count
                                                           :hashrate-distribution]))}
          "Network"]
         [:div.card
          [:div.card-content.is-paddingless
           [:div.tile.is-parent
            [tiles/vertical (:last height) {:width 3 :fractionless? true}]
            [tiles/vertical (:last difficulty) {:width 3 :fractionless? true}]
            [tiles/vertical (:mean block-interval) {:width 3 :val-fmt ->duration}]
            [tiles/vertical (:max pools) {:width 3}]]
           [:div.tile.is-parent
            [tiles/vertical (:mean tx-count) {:width 3 :fractionless? true}]
            [tiles/vertical (:change tx-count) {:width 3 :indicator true}]
            [tiles/vertical (:trend tx-count) {:width 3 :indicator true}]
            [tiles/vertical (:volatility tx-count) {:width 3}]]
           [:div.tile.is-parent
            [tiles/vertical (:mean unconf-count) {:width 3 :fractionless? true}]
            [tiles/vertical (:change unconf-count) {:width               3
                                                    :indicator-inverted? true
                                                    :indicator           true}]
            [tiles/vertical (:trend unconf-count) {:width               3
                                                   :indicator-inverted? true
                                                   :indicator           true}]]]]])]]))

(defn dashboard []
  (let [style (re-frame/subscribe [:dashboard/style])
        data  (re-frame/subscribe [:dashboard/metrics])]
    (fn []
      [:div#dashboard
       (if (= @style :community-focus)
         [community-focus @data]
         [data-focus @data])])))
