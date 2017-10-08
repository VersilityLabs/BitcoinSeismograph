(ns gui.faq
  (:require [reagent.core :as r]

            [gui.vars :as vars]))

(defrecord QA [question answer])

(def faq-contents
  [(QA. "What is the intention of the Bitcoin Seismograph?"
        "This website is about supporting Bitcoin user and entrepreneurs to monitor Bitcoin and to understand unexpected movements in the Bitcoin economy more quickly. We therefore present on this website quantitative Bitcoin indicators and link them to current discussions in the Bitcoin community posted on Reddit or Bitcoin related forums. We believe that our website can be a helpful addition to many great tools already available observing Bitcoin. Nevertheless, the Bitcoin Seismograph is currently in a beta status, so we need your feedback to make it more helpful to you.")
   (QA. "Where did the idea come from?"
        [:p "Since 2014, we research the Bitcoin ecosystem to understand how open entrepreneurs can be resilient in a fluid and stigmatized open source community. For example, we have interviewed Bitcoin entrepreneurs how they have handled the Mt.Gox crisis and have published a few papers about it (please see "
         [:a {:href   "http://www.jistem.fea.usp.br/index.php/jistem/article/view/10.4301%252FS1807-17752016000100001"
              :target "_blank"}
          "here"]
         "). One problem, entrepreneurs are often confronted with, is the necessity to observe the Bitcoin community to foresee unwanted events or find explanations for exceptional incidents without spending too much resources needed for the development of their businesses. Therefore, we started a new design science research project to develop a solution to this problem. With the help from bitcoin.de we could detail the needs of entrepreneurs to monitor the Bitcoin ecosystem and after some prototypes and concept studies, we started to implement the website and API beginning of May 2017."])
   (QA. "Is the website free to use?"
        [:p
         "Yes, the website is free to use, but we kindly ask you to dedicate some time to fill out the "
         [:a {:href  vars/feedback-url
              :target "_blank"}
          "feedback form"]
         " to support us to improve the website further and help Marcel to complete his research project."])
   (QA. "Who has developed the Bitcoin Seismograph?"
        "The Bitcoin Seismograph is developed in cooperation between Marcel Morisse, PhD student at the Department of Informatics, Universität Hamburg and Versility Labs. The developed prototype is part of Marcel’s PhD programme. As this whole project is conceptualized as a open source project, we will publish the program code on Github shortly (we have to clean up the code a bit for a first public open source project commit) and hope, that other developers can include their ideas as well.")
   (QA. "Where do you get your data from?"
        [:div
         [:p "At the moment, we gather data from available APIs from different sources listed below"]
         [:table.table.is-striped
          [:thead
           [:tr
            [:th "API Provider"]
            [:th "Bitcoin Data"]
            [:th "API URL"]
            [:th "Frequency"]]]
          [:tbody
           [:tr
            [:td "BitcoinAverage"]
            [:td "Price EUR (Last, Ask, Bid, High, Low)"]
            [:td [:a {:href   "https://apiv2.bitcoinaverage.com/indices/global/ticker/BTCEUR"
                      :target "_blank"}
                  "https://apiv2.bitcoinaverage.com/indices/global/ticker/BTCEUR"]]
            [:td "Every 5 Minutes"]]
           [:tr
            [:td ""]
            [:td "Price USD (Last, Ask, Bid, High, Low)"]
            [:td [:a {:href   "https://apiv2.bitcoinaverage.com/indices/global/ticker/BTCUSD"
                      :target "_blank"}
                  "https://apiv2.bitcoinaverage.com/indices/global/ticker/BTCUSD"]]
            [:td "Every 5 Minutes"]]
           [:tr
            [:td "Bitcoinchain.com"]
            [:td "Block Height, Block Reward, Difficulty, Hash of Last Block"]
            [:td [:a {:href   "https://api-r.bitcoinchain.com/v1/status"
                      :target "_blank"}
                  "https://api-r.bitcoinchain.com/v1/status"]]
            [:td "Daily"]]
           [:tr
            [:td "BitFinex"]
            [:td "Price USD (Last, Ask, Bid, High, Low)"]
            [:td [:a {:href   "https://api.bitfinex.com/v2/ticker/tBTCUSD"
                      :target "_blank"}
                  "https://api.bitfinex.com/v2/ticker/tBTCUSD"]]
            [:td "Every 5 Minutes"]]
           [:tr
            [:td "Bitstamp"]
            [:td "Price USD (Last, Ask, Bid, High, Low)"]
            [:td [:a {:href   "https://www.bitstamp.net/api/v2/ticker/btcusd/"
                      :target "_blank"}
                  "https://www.bitstamp.net/api/v2/ticker/btcusd/"]]
            [:td "Every 5 Minutes"]]
           [:tr
            [:td ""]
            [:td "Price EUR (Last, Ask, Bid, High, Low)"]
            [:td [:a {:href   "https://www.bitstamp.net/api/v2/ticker/btceur/"
                      :target "_blank"}
                  "https://www.bitstamp.net/api/v2/ticker/btceur/"]]
            [:td "Every 5 Minutes"]]
           [:tr
            [:td "Blockchain.info"]
            [:td "Price CNY/EUR/USD (Last, Ask, Bid)"]
            [:td [:a {:href   "https://blockchain.info/ticker"
                      :target "_blank"}
                  "https://blockchain.info/ticker"]]
            [:td "Every 5 Minutes"]]
           [:tr
            [:td ""]
            [:td "Number of unconfirmed Transactions"]
            [:td [:a {:href   "https://blockchain.info/q/unconfirmedcount"
                      :target "_blank"}
                  "https://blockchain.info/q/unconfirmedcount"]]
            [:td "Every 5 Minutes"]]
           [:tr
            [:td ""]
            [:td "Number of confirmed Transactions (24h)"]
            [:td [:a {:href   "https://blockchain.info/q/24hrtransactioncount"
                      :target "_blank"}
                  "https://blockchain.info/q/24hrtransactioncount"]]
            [:td "Every 15 Minutes"]]
           [:tr
            [:td ""]
            [:td "Interval between blocks"]
            [:td [:a {:href   "https://blockchain.info/q/interval"
                      :target "_blank"}
                  "https://blockchain.info/q/interval"]]
            [:td "Every 15 Minutes"]]
           [:tr
            [:td ""]
            [:td "Difficulty"]
            [:td [:a {:href   "https://blockchain.info/q/getdifficulty"
                      :target "_blank"}
                  "https://blockchain.info/q/getdifficulty"]]
            [:td "Every 15 Minutes"]]
           [:tr
            [:td ""]
            [:td "Mining Pool Shares"]
            [:td [:a {:href   "https://api.blockchain.info/pools"
                      :target "_blank"}
                  "https://api.blockchain.info/pools"]]
            [:td "Every 15 Minutes"]]
           [:tr
            [:td ""]
            [:td "Market Capitalization"]
            [:td [:a {:href   "https://blockchain.info/q/marketcap"
                      :target "_blank"}
                  "https://blockchain.info/q/marketcap"]]
            [:td "Hourly"]]
           [:tr
            [:td ""]
            [:td "Median Confirmation Time"]
            [:td [:a {:href   "https://api.blockchain.info/charts/Median-Confirmation-Time?timespan=1week"
                      :target "_blank"}
                  "https://api.blockchain.info/charts/Median-Confirmation-Time?timespan=1week"]]
            [:td "Daily"]]
           [:tr
            [:td "BlockExplorer"]
            [:td "BlockHeight"]
            [:td [:a {:href   "https://blockexplorer.com/api/status?q=getBlockCount"
                      :target "_blank"}
                  "https://blockexplorer.com/api/status?q=getBlockCount"]]
            [:td "Every 5 Minutes"]]
           [:tr
            [:td ""]
            [:td "Difficulty"]
            [:td [:a {:href   "https://blockexplorer.com/api/status?q=getDifficulty"
                      :target "_blank"}
                  "https://blockexplorer.com/api/status?q=getDifficulty"]]
            [:td "Every 5 Minutes"]]
           [:tr
            [:td ""]
            [:td "Hash of last Block"]
            [:td [:a {:href   "https://blockexplorer.com/api/status?q=getLastBlockHash"
                      :target "_blank"}
                  "https://blockexplorer.com/api/status?q=getLastBlockHash"]]
            [:td "Every 5 Minutes"]]
           [:tr
            [:td "CoinDesk"]
            [:td "Price in USD/EUR"]
            [:td [:a {:href   "http://api.coindesk.com/v1/bpi/currentprice.json"
                      :target "_blank"}
                  "http://api.coindesk.com/v1/bpi/currentprice.json"]]
            [:td "Every 5 Minutes"]]
           [:tr
            [:td "CoinMarketCap"]
            [:td "Price in USD (last), Market Capitalization in USD "]
            [:td [:a {:href   "https://api.coinmarketcap.com/v1/ticker/bitcoin/"
                      :target "_blank"}
                  "https://api.coinmarketcap.com/v1/ticker/bitcoin/"]]
            [:td "Every 5 Minutes"]]
           [:tr
            [:td "Kraken"]
            [:td "Price USD/EUR (Last, Ask, Bid, High, Low) "]
            [:td [:a {:href   "https://api.kraken.com/0/public/Ticker?pair=BTCEUR,XBTUSD"
                      :target "_blank"}
                  "https://api.kraken.com/0/public/Ticker?pair=BTCEUR,XBTUSD"]]
            [:td "Every 5 Minutes"]]]]])
   (QA. "How do you calculate Trend and Change?"
        [:div
         [:p "We use a linear regression to estimate future values based on previous performance. Please be aware, that this method is a very simple one and only provides limited results. Under no circumstances you should rely on the trend indicator for making decisions. In the future, we plan to implement more sophisticated methods (like Polynomial regression or Gauss–Newton) for trend analysis."]
         [:p "Change represents the percentage change from the most current value to the value 24 hours before."]])
   (QA. "How is Volatility calculated?"
        "We calculate the standard deviation for Bitcoin indicators provided by the InfluxDB. After getting the standard deviation, the result is divided by the last available indicator value to get a percental value and a relation of the volatility to current market situations.")
   (QA. "Why turn some number yellow or red?"
        [:div
         [:p "We have implement some warning (yellow) and alert levels (red) indicating that the coloured indicators do not move according to expectations. If some indicators are yellow or even red, please re-check that indicators on other sources and examine community behavior to confirm if something is not going right in the Bitcoin ecosystem. Please be aware, that we had to make a first guess to determine warning and alert levels due to missing data. We hope that we can fine-tune our warning and alert levels based on your "
          [:a {:href   vars/feedback-url
               :target "_blank"}
           "feedback"]
          " or implement a possibility to adjust these levels based on your inputs."]
         [:p "At the moment, the warning and alert levels are:"]
         [:table.table.is-striped
          [:thead
           [:tr
            [:th "Indicator"]
            [:th "Warning Level"]
            [:th "Alert Level"]]]
          [:tbody
           [:tr
            [:td "Bid-Ask Spread"]
            [:td "more than 5% of mean price"]
            [:td "more than 10% of mean price"]]
           [:tr
            [:td "Price Volatility 24h"]
            [:td "more than 5% "]
            [:td "more than 10%"]]
           [:tr
            [:td "Price Volatility 30days / 60 days"]
            [:td "more than 10% "]
            [:td "more than 25%"]]
           [:tr
            [:td "Price Change"]
            [:td "-5% change to price 24 hours earlier"]
            [:td "-10% change to price 24 hours earlier"]]
           [:tr
            [:td "Price Trend"]
            [:td "negative outlook"]
            [:td "highly negative Trend (expected value is 10% lower than current value)"]]
           [:tr
            [:td "Market Capitalization Volatility 24h"]
            [:td "more than 5% "]
            [:td "more than 10%"]]
           [:tr
            [:td "Market Capitalization Volatility 30 days / 60 days"]
            [:td "more than 10% "]
            [:td "more than 25%"]]
           [:tr
            [:td "Market Capitalization Change"]
            [:td "-5% change to market capitalization 24 hours earlier"]
            [:td "-10% change to market capitalization 24 hours earlier"]]
           [:tr
            [:td "Market Capitalization Trend"]
            [:td "negative outlook"]
            [:td "highly negative outlook (expected value is 10% lower than current value)"]]
           [:tr
            [:td "Transaction Volatility"]
            [:td "more than 10% "]
            [:td "more than 20%"]]
           [:tr
            [:td "Transaction Change"]
            [:td "-10% change to number of transactions 24 hours earlier"]
            [:td "-20% change to number of transactions 24 hours earlier"]]
           [:tr
            [:td "Transaction Trend"]
            [:td "negative outlook"]
            [:td "highly negative outlook (expected value is 10% lower than current value)"]]
           [:tr
            [:td "Unconfirmed Transaction Change"]
            [:td "+5% change to number of transactions 24 hours earlier"]
            [:td "+10% change to number of transactions 24 hours earlier"]]
           [:tr
            [:td "Unconfirmed Transaction Trend"]
            [:td "positive outlook"]
            [:td "highly positive outlook (expected value is 10% higher than current value)"]]
           [:tr
            [:td "BlockInterval: Difference to 10min target"]
            [:td "greater than 5 Minutes"]
            [:td "greater than 10 Minutes"]]
           [:tr
            [:td "Share of biggest Mining Pool"]
            [:td "more than 31%"]
            [:td "more than 50%"]]]]])
   (QA. "Where do you get your qualitative Data from?"
        [:div
         [:p "We gather community data from the following sources:"]
         [:ul
          [:li "Reddit/Bitcoin"]
          [:li "Reddit/BitcoinMarkets"]
          [:li "Reddit/btc"]
          [:li "Bitcointalk/Discussion"]
          [:li "Bitcointalk/Legal"]
          [:li "Bitcointalk/Press"]
          [:li "Bitcointalk/Announcements"]
          [:li "Bitcointalk/Development & Technical Discussion"]
          [:li "Bitcointalk/Mining"]
          [:li "Bitcointalk/Mining Speculation"]
          [:li "Bitcointalk/Economics"]
          [:li "Bitcointalk/Speculation"]
          [:li "Bitcointalk/TradingDiscussion"]
          [:li "Forum.Bitcoin.com/Bitcoin Discussion"]
          [:li "Forum.Bitcoin.com/Press"]
          [:li "Forum.Bitcoin.com/Legal"]
          [:li "Forum.Bitcoin.com/Important Announcements"]
          [:li "Forum.Bitcoin.com/Economics"]
          [:li "Forum.Bitcoin.com/Speculation"]
          [:li "Forum.Bitcoin.com/Trading Discussion"]
          [:li "News.Bitcoin.com"]]])
   (QA. "Why are some text boxes green or red?"
        "In order to give you a quick overview about ongoing discussions in the Bitcoin community, we do a sentiment analysis of the forum and reddit posts to signal the positive or negative attitude the post have. Our idea behind that is to support your search for information in case something happened you might not have expected. Due to missing Bitcoin specific dictionaries, our sentiment analysis provides only limited results and might miscalculate the sentiment.")
   (QA. "What is the architecture behind the website?"
        "We are currently finalizing our architecture, as soon we have completed that, we will show it here")
   (QA. "What kind of technology do you use?"
        [:div
         [:p "This website uses several open source technologies to crawl data, analyse data and present it via the Bitcoin Seismograph"]
         [:table.table.is-striped
          [:thead
           [:tr
            [:th "Technology"]
            [:th "Function"]]]
          [:tbody
           [:tr
            [:td "Go 1.8"]
            [:td "Quantitative Data Crawling"]]
           [:tr
            [:td "Python 3.6"]
            [:td "Qualitative Data Scrapping"]]
           [:tr
            [:td "Clojure 1.8"]
            [:td "REST API, using yada"]]
           [:tr
            [:td "ClojureScript 1.9"]
            [:td "frontend, leveraging reagent and re-frame."]]
           [:tr
            [:td "InfluxDB v1.3.1"]
            [:td "Storing quantitative Data and calculating data statistics"]]
           [:tr
            [:td "ElasticSearch 5.4.0"]
            [:td "Storing qualitative Data and Text Analysis "]]]]
         [:p "The Bitcoin Seismograph is running on an AWS infrastructure"]])
   (QA. "Can I contribute?"
        [:p "Sure you can. First, it would be great if you can fill out the "
         [:a {:href   vars/feedback-url
              :target "_blank"}
          "feedback form"]
         " to help us to improve the website further. As soon as we are ready to publicize the open source code of this website (probably beginning of September), we would be happy if you join our project and discuss as well as implement future versions of the Bitcoin Seismograph"])
   (QA. "What is planned for the future?"
        [:div
         [:p "We have lots of ideas on how to improve the Bitcoin Seismograph, here are a few:"]
         [:ul.simple-list
          [:li "Finetuning our algorithms based on your feedback"]
          [:li "Crawling more API data from different API providers more frequently"]
          [:li "Crawling network data directly from the Blockchain (already work in progress in a side project)"]
          [:li "Implementing a fork detection (already work in progress in a side project)"]
          [:li "Introducing a Bitcoin specific dictionary to improve sentiment analysis"]
          [:li "Making the Bitcoin community data searchable"]
          [:li "Implementing the possibility to personalize the Bitcoin Seismograph (for example adjust the warning and alert levels to your needs or show filtered community data)"]]
         [:p "If you have features you are missing and/or feedback on things we should improve, we would love to hear from you."]])])

(defn faq []
  (let [open-answer (r/atom nil)
        q-count     (count faq-contents)]
    (fn []
      (let [open-id @open-answer]
        [:div.faqs
         (map-indexed
          (fn [idx i]
            ^{:key idx}
            [:div.qa
             [:div.faq-question.title.is-5
              {:on-click #(reset! open-answer idx)}
              (:question i)]
             [:div.faq-answer
              {:class (when-not (= open-id idx)
                        "is-hidden")}
              (:answer i)]
             (when (< (inc idx) q-count)
               [:hr])])
          faq-contents)]))))
