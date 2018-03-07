package crawlers

import (
	"encoding/json"
	"time"

	"seismograph/pkg/helpers"

	"github.com/Sirupsen/logrus"
	"github.com/influxdata/influxdb/client/v2"
)

const krakenTicker = "https://api.kraken.com/0/public/Ticker?pair=BTCEUR,XBTUSD" // real-time

type Kraken struct {
	Conf Config
}

func NewKrakenCrawler(conf Config) *Kraken {
	return &Kraken{Conf: conf}
}

func (k *Kraken) Run() {
	defer k.Conf.WaitGroup.Done()
	krakenURL := "kraken.com"

	bp, err := client.NewBatchPoints(BatchPointConf)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": krakenURL,
			"conf":   BatchPointConf,
		}).Error("Unable to create client.NewBatchPoints")
		return
	}

	t, err := k.GetTicker()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": krakenURL,
		}).Error("Failed to get ticker data")
		return
	}

	p1, err := client.NewPoint("price",
		CurrencyTags(USD, krakenURL),
		map[string]interface{}{
			"last": helpers.ParseJSONFloat64(t.Result.BTCtoUSD.LastTrade[0]),
			"bid":     helpers.ParseJSONFloat64(t.Result.BTCtoUSD.BidPrices[0]),
			"ask":    helpers.ParseJSONFloat64(t.Result.BTCtoUSD.AskPrices[0]),
			"high":   helpers.ParseJSONFloat64(t.Result.BTCtoUSD.Highs[1]),
			"low":   helpers.ParseJSONFloat64(t.Result.BTCtoUSD.Lows[1]),
		}, t.TimeStamp.Time)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": krakenURL,
		}).Error("Unable to create client.NewPoint")
		return
	}
	bp.AddPoint(p1)

	p2, err := client.NewPoint("price",
		CurrencyTags(EUR, krakenURL),
		map[string]interface{}{
			"last": helpers.ParseJSONFloat64(t.Result.BTCtoEUR.LastTrade[0]),
			"bid":     helpers.ParseJSONFloat64(t.Result.BTCtoEUR.BidPrices[0]),
			"ask":    helpers.ParseJSONFloat64(t.Result.BTCtoEUR.AskPrices[0]),
			"high":   helpers.ParseJSONFloat64(t.Result.BTCtoEUR.Highs[1]),
			"low":   helpers.ParseJSONFloat64(t.Result.BTCtoEUR.Lows[1]),
		}, t.TimeStamp.Time)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": krakenURL,
		}).Error("Unable to create client.NewPoint")
		return
	}
	bp.AddPoint(p2)

	if err := k.Conf.DBConn.Write(bp); err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": krakenURL,
		}).Error("Unable to write to DB")
		return
	}
}

func (b *Kraken) GetTicker() (PriceRates, error) {
	pr := PriceRates{TimeStamp: Timestamp{time.Now()}}
	if err := helpers.GrabJSON(krakenTicker, &pr); err != nil {
		return pr, err
	}
	return pr, nil
}

type PriceRates struct {
	Result struct {
		BTCtoUSD PriceRate `json:"XXBTZUSD"`
		BTCtoEUR PriceRate `json:"XXBTZEUR"`
	} `json:"result"`
	// TimeStamp is the time at which the data was queried
	TimeStamp Timestamp
}

type PriceRate struct {
	//a : ask array(<price>, <whole lot volume>, <lot volume>),
	AskPrices []json.Number `json:"a"`
	//b : bid array(<price>, <whole lot volume>, <lot volume>),
	BidPrices []json.Number `json:"b"`
	//c : last trade closed array(<price>, <lot volume>),
	LastTrade []json.Number `json:"c"`
	//v : volume array(<today>, <last 24 hours>),
	Volume []json.Number `json:"v"`
	//p : volume weighted average price array(<today>, <last 24 hours>),
	WeightedVolume []json.Number `json:"p"`
	//t : number of trades array(<today>, <last 24 hours>),
	TradeCount []float64 `json:"t"`
	//l : low array(<today>, <last 24 hours>),
	Lows []json.Number `json:"l"`
	//h : high array(<today>, <last 24 hours>),
	Highs []json.Number `json:"h"`
	//o : today's opening price
	OpeningPrice float64 `json:"o,string"`
}
