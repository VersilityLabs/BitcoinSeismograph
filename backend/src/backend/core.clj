(ns backend.core
  (:gen-class)
  (:require [schema.core :as s]
            [yada.yada :as yada]
            [yada.resources.classpath-resource :refer [new-classpath-resource]]
            [aleph.netty :as netty]
            [capacitor.core :as c]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.query :as q]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [clojure.tools.logging :refer [info]]
            [clojure.core.cache :as cache]
            [clojure.core.memoize :as memo]
            [environ.core :refer [env]])
  (:import (org.apache.commons.math3.stat.regression SimpleRegression)))

(def influxdb
  (c/make-client {:host     (or (env :influx-host) "localhost")
                  :scheme   (or (env :influx-scheme) "http")
                  :port     (Integer/parseInt
                              (or (env :influx-port) "8086"))
                  :username (or (env :influx-username) "root")
                  :password (or (env :influx-password) "root")
                  :db       (or (env :influx-db) "seismograph")
                  :get-opts {:socket-timeout 5000           ; in ms
                             :conn-timeout   5000}}))       ; in ms

(def esconn
  (esr/connect (or (env :es-url) "http://localhost:9200")))

(defn trend [values]
  (let [regression (SimpleRegression.)
        data       (into-array (->> values
                                    (map-indexed (fn [idx value]
                                                   (when (second value)
                                                     (double-array [idx (second value)]))))
                                    (filter identity)))]
    (doto regression
      (.addData data))
    ;; (100 * (PredictionOf(n + 1)/PredictionOf(n) - 1))
    (* 100 (dec (/ (.predict regression (count values))
                   (.predict regression (dec (count values))))))))

(defn sliding-trend [values]
  (let [regression (SimpleRegression.)
        {prep-values true
         obs-values  false} (group-by #(boolean (second %)) values)]
    (doall
      (map-indexed
        (fn [idx [_ value]]
          (when value
            (doto regression
              (.addData ^double idx ^double value))))
        prep-values))
    (map-indexed
      (fn [idx [time _ value]]
        (when value
          (doto regression
            (.addData ^double (+ (count prep-values) idx) ^double value)))
        (let [step-result [time (* 100 (dec (/ (.predict regression (inc (+ (count prep-values) idx)))
                                               (.predict regression (+ (count prep-values) idx)))))]
              old-value   (if (< idx (count prep-values))
                            (second (nth prep-values idx))
                            (nth (nth obs-values (- idx (count prep-values))) 2))]
          (when old-value
            (doto regression
              (.removeData ^double idx ^double old-value)))
          step-result))
      obs-values)))

;; ############################
;; #### PRICE CALCULATIONS ####
;; ############################

(def safe-price-range "last > 0 AND last < 20000000")

;; ### MEAN ###
(defn current-price-mean [observation-window]
  {:type       :mean
   :category   :price
   :parameters {(name 'observation-window) observation-window}
   :units      {"CNY" "¥" "EUR" "€" "USD" "$"}
   :query      (str "SELECT MEAN(last) FROM price WHERE " safe-price-range " AND time > now() - " observation-window " GROUP BY currency")})
(defn graph-price-mean [observation-window smoothing-interval]
  {:type       :mean
   :category   :price
   :parameters {(name 'observation-window) observation-window
                (name 'smoothing-interval) smoothing-interval}
   :units      {"CNY" "¥" "EUR" "€" "USD" "$"}
   :query      (str "SELECT MEAN(last) FROM price WHERE " safe-price-range " AND time > now() - " observation-window " GROUP BY time(" smoothing-interval "),currency")})

;; ### HIGH ###
(defn current-price-high [observation-window]
  {:type       :high
   :category   :price
   :parameters {(name 'observation-window) observation-window}
   :units      {"CNY" "¥" "EUR" "€" "USD" "$"}
   :query      (str "SELECT MAX(last),source FROM price WHERE " safe-price-range " AND time > now() - " observation-window " GROUP BY currency")})
(defn graph-price-high [observation-window smoothing-interval]
  {:type       :high
   :category   :price
   :parameters {(name 'observation-window) observation-window
                (name 'smoothing-interval) smoothing-interval}
   :units      {"CNY" "¥" "EUR" "€" "USD" "$"}
   :query      (str "SELECT MAX(last),source FROM price WHERE " safe-price-range " AND time > now() - " observation-window " GROUP BY time(" smoothing-interval "),currency")})

;; ### LOW ###
(defn current-price-low [observation-window]
  {:type       :low
   :category   :price
   :parameters {(name 'observation-window) observation-window}
   :units      {"CNY" "¥" "EUR" "€" "USD" "$"}
   :query      (str "SELECT MIN(last),source FROM price WHERE " safe-price-range " AND time > now() - " observation-window " GROUP BY currency")})
(defn graph-price-low [observation-window smoothing-interval]
  {:type       :low
   :category   :price
   :parameters {(name 'observation-window) observation-window
                (name 'smoothing-interval) smoothing-interval}
   :units      {"CNY" "¥" "EUR" "€" "USD" "$"}
   :query      (str "SELECT MIN(last),source FROM price WHERE " safe-price-range " AND time > now() - " observation-window " GROUP BY time(" smoothing-interval "),currency")})

;; ### SPREAD MEAN ###
(defn current-price-spread-mean [observation-window]
  {:type       :spread-mean
   :category   :price
   :parameters {(name 'observation-window) observation-window}
   :units      {"CNY" "¥" "EUR" "€" "USD" "$"}
   :query      (str "SELECT SPREAD(mean) FROM (SELECT MEAN(last) FROM price WHERE " safe-price-range " AND time > now() - 20m GROUP BY currency,source) GROUP BY currency")})
(defn graph-price-spread-mean [observation-window, smoothing-interval]
  ;; 1. (inner select) get meaningful value per smoothing-interval, currency and source (!)
  ;; 2. then compare spread on specific smoothing-interval and currency (aggregating the source)
  {:type       :spread-mean
   :category   :price
   :parameters {(name 'observation-window) observation-window
                (name 'smoothing-interval) smoothing-interval}
   :units      {"CNY" "¥" "EUR" "€" "USD" "$"}
   :query      (str "SELECT SPREAD(mean) FROM "
                    "(SELECT MEAN(last) FROM price WHERE " safe-price-range " AND time > now() - " observation-window " GROUP BY time(" smoothing-interval "),currency,source) "
                    "WHERE time > now() - " observation-window " GROUP BY time(" smoothing-interval "),currency")})

;; ### BID ASK SPREAD ###
(defn current-price-bid-ask-spread [observation-window]
  ; currently: dirty fix! (crawler was and is mistakenly interchanging blockchain.info bid and ask)
  {:type       :bid-ask
   :category   :price
   :parameters {(name 'observation-window) observation-window}
   :units      {"CNY" "¥" "EUR" "€" "USD" "$"}
   :query      (str "SELECT MEAN(mean) FROM "
                    "(SELECT MEAN(ask_bid) FROM (SELECT ask - bid FROM price WHERE " safe-price-range " AND source != 'blockchain.info' AND time > now() - " observation-window ") GROUP BY currency,source),"
                    "(SELECT MEAN(bid_ask) FROM (SELECT bid - ask FROM price WHERE " safe-price-range " AND source = 'blockchain.info' AND time > now() - " observation-window ") GROUP BY currency,source) "
                    "GROUP BY currency")})
(defn graph-price-bid-ask-spread [observation-window, smoothing-interval]
  ; currently: dirty fix! (crawler was and is mistakenly interchanging blockchain.info bid and ask)
  {:type       :bid-ask
   :category   :price
   :parameters {(name 'observation-window) observation-window
                (name 'smoothing-interval) smoothing-interval}
   :units      {"CNY" "¥" "EUR" "€" "USD" "$"}
   :query      (str "SELECT MEAN(ask_bid) FROM "
                    "(SELECT ask - bid AS ask_bid FROM price WHERE " safe-price-range " AND source != 'blockchain.info' AND time > now() - " observation-window "),"
                    "(SELECT bid - ask AS ask_bid FROM price WHERE " safe-price-range " AND source = 'blockchain.info' AND time > now() - " observation-window ") "
                    "WHERE time > now() - " observation-window " GROUP BY time(" smoothing-interval "),currency")})

;; ### CHANGE ###
(defn current-price-change [observation-window]
  {:type       :change
   :category   :price
   :parameters {(name 'observation-window) observation-window}
   :units      {"CNY" "%" "EUR" "%" "USD" "%"}
   :query      (str "SELECT 100 * (LAST(mean) / FIRST(mean) - 1) FROM "
                    "(SELECT MEAN(last) FROM price WHERE " safe-price-range " AND time > now() - " observation-window " - " observation-window " AND time < now() - " observation-window " GROUP BY currency),"
                    "(SELECT MEAN(last) FROM price WHERE " safe-price-range " AND time > now() - " observation-window " GROUP BY currency) "
                    "GROUP BY currency")})

;; ### TREND ###
(defn current-price-trend [observation-window trend-interval]
  ;; it helps to have a fixed window here, since the `trend` calculation assumes a fixed time interval (for price not the case)
  {:type       :trend
   :category   :price
   :parameters {(name 'observation-window) observation-window
                (name 'trend-interval)     trend-interval}
   :units      {"CNY" "%" "EUR" "%" "USD" "%"}
   :query      (str "SELECT MEAN(last) FROM price WHERE " safe-price-range " AND time > now() - " observation-window " GROUP BY time(" trend-interval "),currency")
   :transform  trend})
(defn graph-price-trend [observation-window reference-window smoothing-interval]
  ;; reference window means regression length for every observed point in time
  ;; trend interval is equals to the graph smoothing interval
  ;; query selects preparation and observation window separately and sliding-trend calc does the rest
  {:type       :trend
   :category   :price
   :parameters {(name 'observation-window) observation-window
                (name 'reference-window)   reference-window
                (name 'smoothing-interval) smoothing-interval}
   :units      {"CNY" "%" "EUR" "%" "USD" "%"}
   :query      (str "SELECT pre_observation_relevant,observation_relevant FROM "
                    "(SELECT MEAN(last) AS observation_relevant FROM price WHERE " safe-price-range " AND "
                    "time > now() - " observation-window " GROUP BY time(" smoothing-interval "),currency),"
                    "(SELECT MEAN(last) AS pre_observation_relevant FROM price WHERE " safe-price-range " AND "
                    "time > now() - " observation-window " - " reference-window " AND time < now() - " observation-window " GROUP BY time(" smoothing-interval "),currency) "
                    "GROUP BY currency")
   :transform  sliding-trend})

;; ### VOLATILITY ###
(defn current-price-volatility [observation-window key]
  {:type       key
   :category   :price
   :parameters {(name 'observation-window) observation-window}
   :units      {"CNY" "%" "EUR" "%" "USD" "%"}
   :query      (str "SELECT 100 * STDDEV(last) / MEAN(last) FROM price WHERE " safe-price-range " AND time > now() - " observation-window " GROUP BY currency")})
(defn graph-price-volatility [observation-window smoothing-interval]
  {:type       :volatility
   :category   :price
   :parameters {(name 'observation-window) observation-window
                (name 'smoothing-interval) smoothing-interval}
   :units      {"CNY" "%" "EUR" "%" "USD" "%"}
   :query      (str "SELECT 100 * STDDEV(last) / MEAN(last) FROM price WHERE " safe-price-range " AND time > now() - " observation-window " GROUP BY time(" smoothing-interval "),currency")})

;; #############################
;; #### MARKET CALCULATIONS ####
;; #############################

;; ### MEAN ###
(defn current-market-mean [observation-window]
  {:type       :mean
   :category   :market
   :parameters {(name 'observation-window) observation-window}
   :units      {"CNY" "¥" "EUR" "€" "USD" "$"}
   :query      (str "SELECT MEAN(marketCap) FROM markets WHERE marketCap > 0 AND time > now() - " observation-window " GROUP BY currency")})
(defn graph-market-mean [observation-window smoothing-interval]
  {:type       :mean
   :category   :market
   :parameters {(name 'observation-window) observation-window
                (name 'smoothing-interval) smoothing-interval}
   :units      {"CNY" "¥" "EUR" "€" "USD" "$"}
   :query      (str "SELECT MEAN(marketCap) FROM markets WHERE marketCap > 0 AND time > now() - " observation-window " GROUP BY time(" smoothing-interval "),currency")})

;; ### HIGH ###
(defn current-market-high [observation-window]
  {:type       :high
   :category   :market
   :parameters {(name 'observation-window) observation-window}
   :units      {"CNY" "¥" "EUR" "€" "USD" "$"}
   :query      (str "SELECT MAX(marketCap),source FROM markets WHERE marketCap > 0 AND time > now() - " observation-window " GROUP BY currency")})

;; ### LOW ###
(defn current-market-low [observation-window]
  {:type       :low
   :category   :market
   :parameters {(name 'observation-window) observation-window}
   :units      {"CNY" "¥" "EUR" "€" "USD" "$"}
   :query      (str "SELECT MIN(marketCap),source FROM markets WHERE marketCap > 0 AND time > now() - " observation-window " GROUP BY currency")})

;; ### CHANGE ###
(defn current-market-change [observation-window]
  {:type       :change
   :category   :market
   :parameters {(name 'observation-window) observation-window}
   :units      {"CNY" "%" "EUR" "%" "USD" "%"}
   :query      (str "SELECT 100 * (LAST(mean) / FIRST(mean) - 1) FROM "
                    "(SELECT MEAN(marketCap) FROM markets WHERE marketCap > 0 AND time > now() - " observation-window " - " observation-window " AND time < now() - " observation-window " GROUP BY currency),"
                    "(SELECT MEAN(marketCap) FROM markets WHERE marketCap > 0 AND time > now() - " observation-window " GROUP BY currency) "
                    "GROUP BY currency")})

;; ### TREND ###
(defn current-market-trend [observation-window trend-interval]
  {:type       :trend
   :category   :market
   :parameters {(name 'observation-window) observation-window
                (name 'trend-interval)     trend-interval}
   :units      {"CNY" "%" "EUR" "%" "USD" "%"}
   :query      (str "SELECT MEAN(marketCap) FROM markets WHERE marketCap > 0 AND time > now() - " observation-window " GROUP BY time(" trend-interval "),currency")
   :transform  trend})

;; ### VOLATILITY ###
(defn current-market-volatility [observation-window key]
  {:type       key
   :category   :market
   :parameters {(name 'observation-window) observation-window}
   :units      {"CNY" "%" "EUR" "%" "USD" "%"}
   :query      (str "SELECT 100 * STDDEV(marketCap) / MEAN(marketCap) FROM markets WHERE marketCap > 0 AND time > now() - " observation-window " GROUP BY currency")})
(defn graph-market-volatility [observation-window smoothing-interval]
  {:type       :volatility
   :category   :market
   :parameters {(name 'observation-window) observation-window
                (name 'smoothing-interval) smoothing-interval}
   :units      {"CNY" "%" "EUR" "%" "USD" "%"}
   :query      (str "SELECT 100 * STDDEV(marketCap) / MEAN(marketCap) FROM markets WHERE marketCap > 0 AND time > now() - " observation-window " GROUP BY time(" smoothing-interval "),currency")})


;; #####################################
;; #### BLOCK INTERVAL CALCULATIONS ####
;; #####################################

;; ### MEAN ###
(defn current-interval-mean [observation-window]
  {:type       :mean
   :category   :block-interval
   :parameters {(name 'observation-window) observation-window}
   :units      {"" "s"}
   :query      (str "SELECT MEAN(blockInterval) FROM network WHERE blockInterval > 0 AND time > now() - " observation-window)})
(defn graph-interval-mean [observation-window smoothing-interval]
  {:type       :mean
   :category   :block-interval
   :parameters {(name 'observation-window) observation-window
                (name 'smoothing-interval) smoothing-interval}
   :units      {"" "s"}
   :query      (str "SELECT MEAN(blockInterval) FROM network WHERE blockInterval > 0 "
                    "AND time > now() - " observation-window " GROUP BY time(" smoothing-interval ")")})

;; ### DIFFERENCE ###
(defn current-interval-difference [observation-window]
  {:type       :difference
   :category   :block-interval
   :parameters {(name 'observation-window) observation-window}
   :units      {"" "s"}
   :query      (str "SELECT MEAN(diff) FROM (SELECT blockInterval - 600 AS diff FROM network WHERE blockInterval > 0 AND time > now() - " observation-window ")")})

;; #########################################
;; #### NETWORK DIFFICULTY CALCULATIONS ####
;; #########################################

(defn current-network-difficulty [observation-window]
  {:type       :last
   :category   :network-difficulty
   :parameters {(name 'observation-window) observation-window}
   :units      {"" ""}
   :query      (str "SELECT LAST(difficulty) FROM network WHERE difficulty > 0 AND time > now() - " observation-window)})

(defn graph-network-difficulty [observation-window smoothing-interval]
  {:type       :max
   :category   :network-difficulty
   :parameters {(name 'observation-window) observation-window
                (name 'smoothing-interval) smoothing-interval}
   :units      {"" ""}
   :query      (str "SELECT MAX(difficulty) FROM network WHERE difficulty > 0 AND time > now() - " observation-window " GROUP BY time(" smoothing-interval ")")})


;; ###############################################
;; #### UNCONFIRMED TRANSACTIONS CALCULATIONS ####
;; ###############################################

;; ### MEAN ###
(defn current-unconfirmed-mean [observation-window]
  {:type       :mean
   :category   :unconfirmed-count
   :parameters {(name 'observation-window) observation-window}
   :units      {"" ""}
   :query      (str "SELECT MEAN(unconfirmedCount) FROM network WHERE unconfirmedCount > 0 AND time > now() - " observation-window)})
(defn graph-unconfirmed-mean [observation-window smoothing-interval]
  {:type       :mean
   :category   :unconfirmed-count
   :parameters {(name 'observation-window) observation-window
                (name 'smoothing-interval) smoothing-interval}
   :units      {"" ""}
   :query      (str "SELECT MEAN(unconfirmedCount) FROM network WHERE unconfirmedCount > 0 AND time > now() - " observation-window " GROUP BY time(" smoothing-interval ")")})


;; ### CHANGE ###
(defn current-unconfirmed-change [observation-window]
  {:type       :change
   :category   :unconfirmed-count
   :parameters {(name 'observation-window) observation-window}
   :units      {"" "%"}
   :query      (str "SELECT 100 * (LAST(mean) / FIRST(mean) - 1) FROM "
                    "(SELECT MEAN(unconfirmedCount) FROM network WHERE unconfirmedCount > 0 AND time > now() - " observation-window " - " observation-window " AND time < now() - " observation-window "),"
                    "(SELECT MEAN(unconfirmedCount) FROM network WHERE unconfirmedCount > 0 AND time > now() - " observation-window ")")})

;; ### TREND ###
(defn current-unconfirmed-trend [observation-window trend-interval]
  {:type       :trend
   :category   :unconfirmed-count
   :parameters {(name 'observation-window) observation-window
                (name 'trend-interval)     trend-interval}
   :units      {"" "%"}
   :query      (str "SELECT MEAN(unconfirmedCount) FROM network WHERE unconfirmedCount > 0 AND time > now() - " observation-window " GROUP BY time(" trend-interval ")")
   :transform  trend})

;; ###################################
;; #### TRANSACTIONS CALCULATIONS ####
;; ###################################

;; ### MEAN ###
(defn current-transaction-mean [observation-window]
  {:type       :mean
   :category   :transaction-count
   :parameters {(name 'observation-window) observation-window}
   :units      {"" ""}
   :query      (str "SELECT MEAN(transactionCount) FROM network WHERE transactionCount > 0 AND time > now() - " observation-window)})
(defn graph-transaction-mean [observation-window smoothing-interval]
  {:type       :mean
   :category   :transaction-count
   :parameters {(name 'observation-window) observation-window
                (name 'smoothing-interval) smoothing-interval}
   :units      {"" ""}
   :query      (str "SELECT MEAN(transactionCount) FROM network WHERE transactionCount > 0 AND time > now() - " observation-window " GROUP BY time(" smoothing-interval ")")})

;; ### CHANGE ###
(defn current-transaction-change [observation-window]
  {:type       :change
   :category   :transaction-count
   :parameters {(name 'observation-window) observation-window}
   :units      {"" "%"}
   :query      (str "SELECT 100 * (LAST(mean) / FIRST(mean) - 1) FROM "
                    "(SELECT MEAN(transactionCount) FROM network WHERE transactionCount > 0 AND time > now() - " observation-window " - " observation-window " AND time < now() - " observation-window "),"
                    "(SELECT MEAN(transactionCount) FROM network WHERE transactionCount > 0 AND time > now() - " observation-window ")")})

;; ### TREND ###
(defn current-transaction-trend [observation-window trend-interval]
  {:type       :trend
   :category   :transaction-count
   :parameters {(name 'observation-window) observation-window
                (name 'trend-interval)     trend-interval}
   :units      {"" "%"}
   :query      (str "SELECT MEAN(transactionCount) FROM network WHERE transactionCount > 0 AND time > now() - " observation-window " GROUP BY time(" trend-interval ")")
   :transform  trend})

;; ### VOLATILITY ###
(defn current-transaction-volatility [observation-window]
  {:type       :volatility
   :category   :transaction-count
   :parameters {(name 'observation-window) observation-window}
   :units      {"" "%"}
   :query      (str "SELECT 100 * STDDEV(transactionCount) / MEAN(transactionCount) FROM network WHERE transactionCount > 0 AND time > now() - " observation-window)})

;; ###############################################
;; #### MEDIAN CONFIRMATION TIME CALCULATIONS ####
;; ###############################################

(defn graph-median-confirmation-time-mean [observation-window smoothing-interval]
  {:type       :mean
   :category   :median-confirmation-time
   :parameters {(name 'observation-window) observation-window
                (name 'smoothing-interval) smoothing-interval}
   :units      {"" "s"}
   :query      (str "SELECT 60 * MEAN(medianConfirmationTime) FROM network WHERE medianConfirmationTime > 0 AND time > now() - " observation-window " GROUP BY time(" smoothing-interval ")")})

;; ###############################################
;; #### MEDIAN CONFIRMATION TIME CALCULATIONS ####
;; ###############################################

(defn current-block-number [observation-window]
  {:type       :last
   :category   :block-height
   :parameters {(name 'observation-window) observation-window}
   :units      {"" ""}
   :query      (str "SELECT LAST(height) FROM network WHERE height > 0 AND time > now() - " observation-window)})


;; ############################################
;; #### HASHRATE DISTRIBUTION CALCULATIONS ####
;; ############################################

;; ### MAX ###
(defn current-hashrate-distribution-max [observation-window]
  {:type       :max
   :category   :hashrate-distribution
   :parameters {(name 'observation-window) observation-window}
   :units      {"" "%"}
   :query      (str "SELECT 100 * MAX(percentage_share),mining_pool FROM pools WHERE percentage_share > 0 AND time > now() - " observation-window)})

;; #######################
;; #### QUERY HELPERS ####
;; #######################

(defn extract [values unwrapping?]
  (cond
    (not unwrapping?) values
    (not= (count values) 1) values
    (= (count (first values)) 2) (second (first values))
    :else (vec (rest (first values)))))

(defn process-queries [qs unwrapping?]
  (let [bulk-query (str/join ";" (map #(:query %) qs))
        result     (c/db-query influxdb bulk-query)
        meta       (map #(dissoc % :query) qs)
        parsed     (for [unwrapped (:results result)]
                     (into {} (for [grouping (:series unwrapped)]
                                [(str/join "+" (vals (:tags grouping))) (extract (:values grouping) unwrapping?)])))]
    (->> (map (fn [m1 m2]
                (if-let [tx (:transform m1)]
                  (assoc (dissoc m1 :transform) :values (into {} (map (fn [[k v]] [k (tx v)]) m2)))
                  (assoc m1 :values m2))) meta parsed)
         (group-by #(:category %))
         (map (fn [[k v]] [k (->> (group-by #(:type %) v)
                                  (map (fn [[k2 v2]] [k2 (first v2)]))
                                  (into {}))]))
         (cons [:meta {:timestamp (System/currentTimeMillis)}])
         (into {}))))

;; ##############################
;; #### WEB RESOURCE HELPERS ####
;; ##############################

(def interval-pattern #"([1-9][0-9]*)([wdhm])")

(def StringInterval
  (s/constrained String #(re-matches interval-pattern %)))  ;; limited to >1 weeks, days, hours or minutes

(defn max-interval [i1 i2]
  (let [[_ n1 u1] (re-find (re-matcher interval-pattern i1))
        [_ n2 u2] (re-find (re-matcher interval-pattern i2))
        unit->minutes (fn [u] (case u "w" 10080 "d" 1440 "h" 60 "m" 1))
        v1            (* (Integer/parseInt n1) (unit->minutes u1))
        v2            (* (Integer/parseInt n2) (unit->minutes u2))]
    (if (> v1 v2) i1 i2)))

;; #################################
;; #### WEB RESOURCE: DASHBOARD ####
;; #################################

(defn dashboard-statistics [observation-window trend-interval]
  (info "serving dashboard data with [" observation-window "," trend-interval "]")
  ; trend-interval defines how far in the future (n+1) is predicted
  (process-queries
    [(current-price-mean observation-window)
     (current-price-low observation-window)
     (current-price-high observation-window)
     (current-price-spread-mean observation-window)
     (current-price-bid-ask-spread observation-window)
     (current-price-change observation-window)
     (current-price-trend observation-window trend-interval)
     (current-price-volatility observation-window :volatility)
     (current-price-volatility "30d" :volatility-30d)
     (current-price-volatility "60d" :volatility-60d)
     (current-market-mean observation-window)
     (current-market-low observation-window)
     (current-market-high observation-window)
     (current-market-change observation-window)
     (current-market-volatility observation-window :volatility)
     (current-market-volatility "30d" :volatility-30d)
     (current-market-volatility "60d" :volatility-60d)
     (current-market-trend observation-window trend-interval)
     (current-interval-mean observation-window)
     (current-interval-difference observation-window)
     (current-network-difficulty observation-window)
     (current-unconfirmed-mean observation-window)
     (current-unconfirmed-change observation-window)
     (current-unconfirmed-trend observation-window trend-interval)
     (current-transaction-mean observation-window)
     (current-transaction-change observation-window)
     (current-transaction-trend observation-window trend-interval)
     (current-transaction-volatility observation-window)
     (current-block-number observation-window)
     (current-hashrate-distribution-max observation-window)]
    true))

(def cached-dashboard-statistics
  (memo/ttl dashboard-statistics
            (cache/lru-cache-factory {} :threshold 32)
            :ttl/threshold (* 5 60 1000)))                  ; 5 min TTL

(def dashboard-resource
  (yada/resource
    {:id             :resource/dashboard
     :produces       "application/json"
     :swagger/tags   ["frontend"]
     :parameters     {:query {(s/optional-key :observation-window) StringInterval
                              (s/optional-key :trend-interval)     StringInterval}}
     :access-control {:allow-origin      "*"
                      :allow-credentials false
                      :allow-methods     #{:get :options}}
     :methods        {:get {:description (str "Returns grouped and categorized potentially relevant data of current Bitcoin key figures. "
                                              "Parameter examples: '4w' for 'four weeks', '1d' for 'one day', '2h' for 'two hours' or '10m' for 'ten minutes' "
                                              "(default: 24h observation window, 1h trend interval)")
                            :summary     "quantitative dashboard data"
                            :response    (fn [ctx]
                                           (let [observation-window (-> ctx :parameters :query :observation-window)
                                                 trend-interval     (-> ctx :parameters :query :trend-interval)]
                                             (cached-dashboard-statistics "24h" "1h")
                                             #_(cached-dashboard-statistics (or observation-window "24h")
                                                                            (or trend-interval "1h"))))}}}))

;; #############################
;; #### WEB RESOURCE: GRAPH ####
;; #############################

(defn graph-data [observation-window smoothing-interval trend-window]
  (info "serving graph data with [" observation-window "," smoothing-interval "," trend-window "]")
  (process-queries
    [(graph-price-mean observation-window smoothing-interval)
     (graph-price-high observation-window smoothing-interval)
     (graph-price-low observation-window smoothing-interval)
     (graph-price-spread-mean observation-window smoothing-interval)
     (graph-price-bid-ask-spread observation-window smoothing-interval)
     (graph-price-trend observation-window trend-window smoothing-interval)
     (graph-price-volatility observation-window smoothing-interval)
     (graph-market-mean observation-window smoothing-interval)
     (graph-market-volatility observation-window smoothing-interval)
     (graph-median-confirmation-time-mean observation-window (max-interval smoothing-interval "1d")) ;; else many nulls
     (graph-interval-mean observation-window (max-interval smoothing-interval "30m")) ;; no less than 30m smoothing, else nil handling
     (graph-network-difficulty observation-window (max-interval smoothing-interval "30m")) ;; no less than 30m smoothing, else nil handling
     (graph-transaction-mean observation-window (max-interval smoothing-interval "30m")) ;; no less than 30m smoothing, else nil handling
     (graph-unconfirmed-mean observation-window smoothing-interval)]
    false))

(def cached-graph-data
  (memo/ttl graph-data
            (cache/lru-cache-factory {} :threshold 64)
            :ttl/threshold (* 5 60 1000)))                  ; 5 min TTL

(def graph-resource
  (yada/resource
    {:id             :resource/graph
     :produces       "application/json"
     :swagger/tags   ["frontend"]
     :parameters     {:query {(s/optional-key :observation-window) StringInterval
                              (s/optional-key :smoothing-interval) StringInterval
                              (s/optional-key :trend-window)       StringInterval}}
     :access-control {:allow-origin      "*"
                      :allow-credentials false
                      :allow-methods     #{:get :options}}
     :methods        {:get {:description (str "Returns grouped and categorized lists of potentially relevant time series data. "
                                              "Parameter examples: '4w' for 'four weeks', '1d' for 'one day', '2h' for 'two hours' or '10m' for 'ten minutes' "
                                              "(default: 30d observation window, 12h smoothing interval, 24h trend window) "
                                              "Disclaimer: Multiple smoothing intervals need to fit into one trend window.")
                            :summary     "quantitative time series data"
                            :response    (fn [ctx]
                                           (let [observation-window (-> ctx :parameters :query :observation-window)
                                                 smoothing-interval (-> ctx :parameters :query :smoothing-interval)
                                                 trend-window       (-> ctx :parameters :query :trend-window)]
                                             (cached-graph-data "30d" "12h" "1d")
                                             #_(cached-graph-data (or observation-window "30d")
                                                                  (or smoothing-interval "12h")
                                                                  (or trend-window "1d"))))}}}))

;; #############################
;; #### WEB RESOURCE: GRAPH ####
;; #############################

(defn hits [data]
  (map (fn [m]
         (if-let [last-scrape (last (get-in m [:_source :scrapes]))]
           (assoc-in m [:_source :scrapes] [last-scrape])
           m))
       (get-in data [:hits :hits])))

(defn text-data [epoch-millis]
  (info "serving text data with [" epoch-millis "]")
  {:meta        {:timestamp epoch-millis}
   :news        (hits (esd/search esconn "news" "item"
                                  :size 10
                                  :query {:range
                                          {:timestamp
                                           {:lte epoch-millis}}}
                                  :sort {:timestamp {:order :desc}}))
   :submissions (hits (esd/search esconn "submissions" "submission"
                                  :size 10
                                  :sort {"scrapes.rank"
                                         {:order         "desc"
                                          :nested_path   "scrapes"
                                          :nested_filter {:range
                                                          {"scrapes.scraped_at"
                                                           {:gte (- epoch-millis (* 20 60 1000)) ;; -20m
                                                            :lte epoch-millis}}}}}))
   :threads     (hits (esd/search esconn "threads" "thread"
                                  :size 10
                                  :query {"bool"
                                          {"must"
                                           [{"nested"
                                             {"path"  "scrapes"
                                              "query" {"range"
                                                       {"scrapes.scraped_at"
                                                        {"gte" (- epoch-millis (* 6 60 60 1000)) ;; -6h
                                                         "lte" epoch-millis}}}}}
                                            {"range"
                                             {"created_at"
                                              {"gte" (- epoch-millis (* 3 24 60 60 1000)) ;; -3d
                                               "lte" epoch-millis}}}]}}
                                  :sort {"scrapes.views"
                                         {"nested_path" "scrapes"
                                          "order"       "desc"}}))})

(def cached-text-data
  (memo/ttl text-data
            (cache/lru-cache-factory {} :threshold 256)
            :ttl/threshold (* 120 60 1000)))                ; 120 min TTL (since already partitioned by time)

(def text-resource
  (yada/resource
    {:id             :resource/graph
     :produces       "application/json"
     :swagger/tags   ["frontend"]
     :parameters     {:query {(s/optional-key :timestamp) Long}}
     :access-control {:allow-origin      "*"
                      :allow-credentials false
                      :allow-methods     #{:get :options}}
     :methods        {:get {:description "Given an (optional) timestamp in epoch milliseconds, returns a chronological fitting (or else current) grouped list of news and community contributions."
                            :summary     "qualitative community data"
                            :response    (fn [ctx]
                                           (let [t           (-> ctx :parameters :query :timestamp)
                                                 now         (System/currentTimeMillis)
                                                 param-t     (long (or t now))
                                                 valid-t     (max (min param-t now) 1497477600000) ;; 2017-06-15
                                                 granularity (* 30 60 1000)
                                                 floored-t   (* granularity (long (Math/floor (/ valid-t granularity))))]
                                             (cached-text-data floored-t)))}}}))


;; #####################
;; #### WEB ROUTING ####
;; #####################

(defn api-routes []
  [""
   [
    ["/dashboard" (yada/handler dashboard-resource)]
    ["/graph" (yada/handler graph-resource)]
    ["/text" (yada/handler text-resource)]]])

(defn routes []
  [""
   [
    ["/api" (yada/swaggered
              (api-routes)
              {:info     {:title          "The BitcoinSeismograph API"
                          :version        "0.1"
                          :description    "Methods for observing the Bitcoin ecosystem."
                          :termsOfService "https://bitcoinseismograph.info/termsofuse.html"
                          :contact        {:name  "Versility Labs GmbH & Marcel Morisse"
                                           :email "feedback@bitcoinseismograph.info"
                                           :url   "https://bitcoinseismograph.info"}
                          :license        {:name "MIT License"}}
               :tags     [{:name        "frontend"
                           :description "aggregated frontend methods"}]
               :basePath "/api"})]

    #_["" (yada/handler (new-classpath-resource "public" {:index-files ["index.html"]}))]

    [true (yada/handler nil)]]])

;; ##############
;; #### MAIN ####
;; ##############

(defn launch []
  ;; for repl use
  (yada/listener (routes) {:port (Integer/parseInt (or (env :port) "3020"))}))

(defn -main [& args]
  (info "Starting...")
  (launch)
  ;; all threads are daemon, so block forever:
  (info "... Startup Done!")
  @(promise))


