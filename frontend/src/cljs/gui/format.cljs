(ns gui.format
  (:require [clojure.string :as string]
            [goog.date.relative :as grelative])
  (:import goog.i18n.NumberFormat
           goog.i18n.DateTimeFormat
           [goog.i18n.NumberFormat Format]))

(def ->short-date (DateTimeFormat. "dd.MM.yy kk:mm"))
(def ->number-with-fraction (-> (NumberFormat. (-> Format .-DECIMAL))
                                (.setMaximumFractionDigits 2)
                                (.setMinimumFractionDigits 2)))
(def ->number (-> (NumberFormat. (-> Format .-DECIMAL))
                  (.setMaximumFractionDigits 0)))
(def ->short (NumberFormat. (-> Format .-COMPACT_SHORT)))

(defn ->label [metric]
  (let [type (:type metric)
        cat  (:cat metric)]
    (cond
      (and (= "last" type)
           (= "block-height" cat))          "Current Block Number"
      (and (= "last" type)
           (= "network-difficulty" cat))    "Block Difficulty"
      (and (= "max" type)
           (= "hashrate-distribution" cat)) "Largest Mining Pool"
      (and (= "mean" type)
           (= "block-interval" cat))        "Time between Blocks"
      (and (= "volatility" type)
           (or (= "transaction-count" cat)
               (= "market" cat)))           (str "Volatility (" (:period metric) ")")
      (and (= "mean" type)
           (= "transaction-count" cat))     (str "Transactions (" (:period metric) ")")
      (and (= "mean" type)
           (= "unconfirmed-count" cat))     (str "Unconfirmed Transactions (" (:period metric) ")")
      (and (= "mean" type)
           (= "price" cat))                 "Current"
      (and (= "mean" type)
           (= "market" cat))                "Current Market Capitalization"
      (#{"high" "low"
         "change" "trend"} type)            (str (string/capitalize type) " (" (:period metric) ")")
      (= "bid-ask" type)                    "Current Ask-Bid Spread")))

(defn ->fixed [metric]
  (str (.toFixed (get metric :val 0) 2)
       (:unit metric)))

(defn ->duration [metric]
  (let [val (:val metric)
        min (quot val 60)
        sec (js/Math.round (rem val 60))]
    (str min "m " sec "s")))

(defn raw-with-unit
  ([metric]
   (raw-with-unit metric {}))
  ([metric opts]
   (let [{:keys [val unit]}     metric
         {:keys [shorten?
                 fractionless?]} opts]
    (cond
      shorten?      (str unit " " (.format ->short val))
      fractionless? (str unit " " (.format ->number val))
      :else         (str (.format ->number-with-fraction val) " " unit)))))

(defn with-unit
  ([unit]
   (with-unit unit nil))
  ([unit opts]
   (let [{:keys [shorten?
                 fractionless?]} opts]
     (fn [val]
       (cond
         shorten?      (str unit " " (.format ->short val))
         fractionless? (str unit " " (.format ->number val))
         :else         (str (.format ->number-with-fraction val) " " unit))))))

(defn ->min [seconds]
  (let [min (quot seconds 60)
        sec (js/Math.round (rem seconds 60))]
    (str min "m " sec "s")))

(defn ->date [date]
  (if (number? date)
    (.format ->short-date (js/Date. date))
    (.format ->short-date date)))

(defn ->relative-date [date]
  ;; TODO: could probably have more strenuous testing here: number, string, js/Date
  ;; FIXME: this is ugly... make it pretty
  (let [datum (if-not (number? date)
                (.getTime (js/Date. date))
                date)
        rel   (grelative/format datum)]
    (if (= "" rel)
      (->date (js/Date. datum))
      rel)))

(defn smoothed-val->chart-label [cat type data]
  (let [smoothing-interval (:smoothing-interval (:parameters data))]
    (cond
      (and (= "max" type)
           (= "network-difficulty" cat)) (str "Network Difficulty (" smoothing-interval ")")
      (and (= "mean" type)
           (= "block-interval" cat))     (str "Mean Time between Blocks ("
                                              smoothing-interval ")")
      (and (= "mean" type)
           (= "transaction-count" cat))  (str "Mean Transaction Count (" smoothing-interval ")")

      (and (= "mean" type)
           (= "median-confirmation-time" cat)) (str "Mean Median Confirmation Time ("
                                                    smoothing-interval ")")

      (and (= "volatility" type))       (str (case cat
                                               "market" "Market Capitalization "
                                               "price"  "Price "
                                               nil) "Volatiltiy (" smoothing-interval ")")
      (and (= "mean" type)
           (= "unconfirmed-count" cat)) (str "Mean Unconfirmed Transaction Count ("
                                             smoothing-interval ")")
      (and (= "mean" type)
           (= "price" cat))             (str "Mean Price (" smoothing-interval ")")
      (and (= "spread-mean" type)
           (= "price" cat))             (str "Mean Price Spread (" smoothing-interval ")")
      (and (= "mean" type)
           (= "market" cat))            (str "Mean Market Capitalization ("
                                             smoothing-interval")")
      (and (= "trend" type)
           (= "price" cat))             (str "Price Trend (+" smoothing-interval ")")
      (#{"high" "low"
         "change" "trend"} type)        (str "Mean " cat " " (string/capitalize type)
                                             " (" smoothing-interval ")")
      (= "bid-ask" type)                (str "Mean Bid-Ask Spread (" smoothing-interval ")"))))

(defn warning->class [warning]
  ;; Note: currently the `map?` branch is only designed to handle aggregate warning level data!
  (let [w (if (map? warning)
            (cond
              (pos? (:alert warning))   :alert
              (pos? (:warning warning)) :warning
              :else                     nil)
            warning)]
    (case w
      :alert   "is-alert"
      :warning "is-warning"
      nil      nil)))

(defn warning->hiccup-attrs [warning]
  {:class (warning->class warning)})
