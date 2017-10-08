(ns gui.tiles
  (:require [gui.components :as components]
            [gui.format :refer [->fixed
                                ->label
                                ->duration
                                raw-with-unit
                                warning->class
                                warning->hiccup-attrs]]
            [gui.warnings :as warnings]))

(defn summary [cat data]
  (fn [cat data]
    (let [mean   (:mean data)
          trend  (:trend data)
          change (:change data)]
      [:div
       [components/level-item
        (warning->hiccup-attrs (:warning-level mean))
        [:div.title.is-6
         (->label mean)]
        (case cat
          :market            (raw-with-unit mean {:shorten? true})
          :transaction-count (raw-with-unit mean {:fractionless? true})
          :unconfirmed-count (raw-with-unit mean {:fractionless? true})
          (raw-with-unit mean))]
       [components/level-item
        (warning->hiccup-attrs (:warning-level trend))
        [:div.title.is-6
         (->label trend)]
        [:div
         [components/trend-indicator (:val trend) :invert? (= cat :unconfirmed-count)]
         (raw-with-unit trend)]]
       [components/level-item
        (warning->hiccup-attrs (:warning-level change))
        [:div.title.is-6
         (->label change)]
        [:div
         [components/trend-indicator (:val change) :invert? (= cat :unconfirmed-count)]
         (raw-with-unit change)]]])))

(defn low-high [data]
  (fn [data]
    (let [high (:high data)
          low  (:low data)]
      [:div
       [components/level-item
        (warning->hiccup-attrs (:warning-level high))
        [:div.title.is-6
         (->label high)]
        [:div.has-text-right
         (raw-with-unit high)
         [:br]
         [:small (:src high)]]]
       [components/level-item
        (warning->hiccup-attrs (:warning-level low))
        [:div.title.is-6
         (->label low)]
        [:div.has-text-right
         (raw-with-unit low)
         [:br]
         [:small (:src low)]]]])))

(defn price-bracket [data]
  (fn [data]
    (let [bid-ask (:bid-ask data)
          high    (:high data)
          low     (:low data)]
      [:div
       [components/level-item
        (warning->hiccup-attrs (:warning-level bid-ask))
        [:div.title.is-6
         (->label bid-ask)]
        (raw-with-unit bid-ask)]
       [components/level-item
        (warning->hiccup-attrs (:warning-level high))
        [:div.title.is-6
         (->label high)]
        [:div.has-text-right
         (raw-with-unit high)
         [:br]
         [:small (:src high)]]]
       [components/level-item
        (warning->hiccup-attrs (:warning-level low))
        [:div.title.is-6
         (->label low)]
        [:div.has-text-right
         (raw-with-unit low)
         [:br]
         [:small (:src low)]]]])))

(defn volatility [data]
  (fn [data]
    (let [volatility     (:volatility data)
          volatility-30d (:volatility-30d data)
          volatility-60d (:volatility-60d data)]
      [:div
       [:div.title.is-6.is-marginless
        (warning->hiccup-attrs (warnings/values->highest-warning-level [volatility
                                                                        volatility-30d
                                                                        volatility-60d]))
        (str (case (:cat volatility)
               "transaction-count" "Transaction Count "
               "price"             "Price "
               "market"            "Market Cap ") "Volatility")]
       [components/level-item
        (warning->hiccup-attrs (:warning-level volatility))
        [:div.title.is-6
         "24h"]
        (raw-with-unit volatility)]
       (when volatility-30d
         [components/level-item
          (warning->hiccup-attrs (:warning-level volatility-30d))
          [:div.title.is-6
           "30d"]
          (raw-with-unit volatility-30d)])
       (when volatility-60d
         [components/level-item
          (warning->hiccup-attrs (:warning-level volatility-60d))
          [:div.title.is-6
           "60d"]
          (raw-with-unit volatility-60d)])])))

(defn pools [data]
  (fn [data]
    (let [pool (:max data)]
      [:div
       (warning->hiccup-attrs (:warning-level pool))
       [components/level-item
        [:div.title.is-6
         (->label pool)]
        [:div.has-text-right
         (->fixed pool)
         [:br]
         [:small (:src pool)]]]])))

(defn block-interval [data]
  (fn [data]
    (let [bi (:mean data)]
      [:div
       [components/level-item
        (warning->hiccup-attrs (:warning-level bi))
        [:div.title.is-6
         (->label bi)]
        (->duration bi)]])))

(defn tile [type & args]
  (let []
    [:div.tile.is-parent
     [:div.tile.is-child
      [:div.card
       [:div.card-content.is-narrow
        (case type
          :summary        (into [summary] args)
          :price-bracket  (into [price-bracket] args)
          :low-high       (into [low-high] args)
          :volatility     (into [volatility] args)
          :block-interval (into [block-interval] args)
          :pools          (into [pools] args))]]]]))

(defn vertical [data {:keys [width
                             indicator
                             indicator-inverted?
                             fractionless?
                             val-fmt]
                      :or   {val-fmt raw-with-unit}}]
  [:div.tile.is-vertical
   {:class (str "is-" width " " (warning->class (:warning-level data)))}
   [:div.title.is-6.is-marginless
    (->label data)]
   [:div
    (when indicator
      [components/trend-indicator (:val data) :invert? indicator-inverted?])
    (val-fmt data {:fractionless? fractionless?})]
   (when-let [src (:src data)]
     [:small src])])

(defn vertical-volatility-ext [data width]
  (let [volatility     (:volatility data)
        volatility-30d (:volatility-30d data)
        volatility-60d (:volatility-60d data)]
    [:div.tile.is-vertical
     {:class (str "is-" width)}
     [:div.title.is-6.is-marginless
      (warning->hiccup-attrs (warnings/values->highest-warning-level [volatility
                                                                      volatility-30d
                                                                      volatility-60d]))
      "Volatility (24h / 30d / 60d)"]
     [:div
      [:span
       (warning->hiccup-attrs (:warning-level volatility))
       (raw-with-unit volatility)]
      " / "
      [:span
       (warning->hiccup-attrs (:warning-level volatility-30d))
       (raw-with-unit volatility-30d)]
      " / "
      [:span
       (warning->hiccup-attrs (:warning-level volatility-60d))
       (raw-with-unit volatility-60d)]]]))
