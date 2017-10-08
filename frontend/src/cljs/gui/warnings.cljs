(ns gui.warnings
  "Contains the logic for warning levels."
  (:require [re-frame.core :as re-frame]))

;; Price
(defn bid-ask-spread [val mean-price]
  (let [spread-percentage (/ val mean-price)]
    (cond
      (> spread-percentage 0.1)  :alert
      (> spread-percentage 0.05) :warning
      :else                      nil)))

(defn price-volatility [val]
  (cond
    (> val 10) :alert
    (> val 5)  :warning
    :else      nil))

(defn price-volatility-ext [val]
  (cond
    (> val 25) :alert
    (> val 10)  :warning
    :else      nil))

(defn price-trend [val]
  (cond
    (< val -10) :alert
    (neg? val)  :warning
    :else       nil))

;; Market Capitalization
(defn market-cap-volatility [val]
  (cond
    (> val 10) :alert
    (> val 5)  :warning
    :else      nil))

(defn market-cap-volatility-ext [val]
  (cond
    (> val 25) :alert
    (> val 10) :warning
    :else      nil))

(defn market-cap-trend [val]
  (cond
    (< val -10) :alert
    (neg? val)  :warning
    :else       nil))

;; Block Interval
(def ^:const five-min (* 5 60))
(def ^:const ten-min (* 10 60))

(defn diff-to-10m [block-interval]
  (let [diff (js/Math.abs (- (* 10 60 1000) block-interval))]
    (cond
      (> val ten-min)  :alert
      (> val five-min) :warning
      :else            nil)))

;; Unconfirmed Count
(defn unconfirmed-count-trend [val]
  (cond
    (> val 10) :alert
    (pos? val) :warning
    :else      nil))

;; Transaction Count
(defn tx-count-volatility [val]
  (cond
    (> val 20) :alert
    (> val 10) :warning
    :else      nil))

(defn tx-count-trend [val]
  (cond
    (< val -10) :alert
    (< val -5)  :warning
    :else       nil))

;; Hashrate distribution
(defn share-of-highest-pool [share]
  (cond
    (> share 51) :alert
    (> share 31) :warning
    :else        nil))

(defn get-warning-level [category type value]
  (case category
    "price"                 (case type
                              "volatility"       (price-volatility value)
                              ("volatility-30d"
                               "volatility-60d") (price-volatility-ext value)
                              "trend"            (price-trend value)
                              nil)
    "market"                (case type
                              "volatility"       (market-cap-volatility value)
                              ("volatility-30d"
                               "volatility-60d") (market-cap-volatility-ext value)
                              "trend"            (market-cap-trend value)
                              nil)
    "block-interval"        (case type
                              "mean" (diff-to-10m value)
                              nil)
    "transaction-count"     (case type
                              ("volatility"
                               "volatility-30d"
                               "volatility-60d") (tx-count-volatility value)
                              "trend"            (tx-count-trend value)
                              nil)
    "unconfirmed-count"     (case type
                              "trend" (unconfirmed-count-trend value)
                              nil)
    "hashrate-distribution" (case type
                              "max" (share-of-highest-pool value)
                              nil)
    nil))

(defn for-category [category-data]
  (reduce (fn [acc [cat {:keys [warning-level] :as d}]]
            (if warning-level
              (update-in acc [warning-level]
                         inc)
              acc))
          {:alert   0
           :warning 0}
          category-data))

(defn ->warning-level-key [category]
  (keyword (if (keyword? category)
             (name category)
             category) "warning-levels"))

(defn ->highest-warning-level [data categories]
  (let [keys     (map ->warning-level-key categories)
        warnings (apply merge-with + (map data keys))]
    (cond
      (pos? (:alert warnings))   :alert
      (pos? (:warning warnings)) :warning
      :else                      nil)))

(defn values->highest-warning-level [values]
  (reduce (fn [highest val]
            (case highest
              :alert   :alert
              :warning (if (= :alert (:warning-level val))
                         :alert
                         :warning)
              (:warning-level val)))
   nil
   values))
