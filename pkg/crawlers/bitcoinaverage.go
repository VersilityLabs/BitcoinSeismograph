package crawlers

import (
	"seismograph/pkg/helpers"

	"github.com/Sirupsen/logrus"
	"github.com/influxdata/influxdb/client/v2"
)

const (
	bitcoinAverageEURPriceRate = "https://apiv2.bitcoinaverage.com/indices/global/ticker/BTCEUR" // real-time
	bitcoinAverageUSDPriceRate = "https://apiv2.bitcoinaverage.com/indices/global/ticker/BTCUSD" // real-time
)

type BitcoinAverage struct {
	Conf Config
}

func NewBitcoinAverageCrawler(conf Config) *BitcoinAverage {
	return &BitcoinAverage{conf}
}

func (b *BitcoinAverage) Run() {
	defer b.Conf.WaitGroup.Done()
	btcAvgURL := "bitcoinaverage.com"

	bp, err := client.NewBatchPoints(BatchPointConf)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": btcAvgURL,
			"conf":   BatchPointConf,
		}).Error("Unable to create client.NewBatchPoints")
		return
	}

	usd := b.GetUSDPrice()
	eur := b.GetEURPrice()

	p1, err := client.NewPoint("price",
		CurrencyTags(USD, btcAvgURL),
		map[string]interface{}{
			"last": 	usd.Last,
			"bid":    	usd.Bid,
			"ask":    	usd.Ask,
			"high":		usd.High,
			"low":		usd.Low,
		}, usd.TimeStamp.Time)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": btcAvgURL,
		}).Error("Unable to create client.NewPoint")
		return
	}
	bp.AddPoint(p1)

	p2, err := client.NewPoint("price",
		CurrencyTags(EUR, btcAvgURL),
		map[string]interface{}{
			"last": 	eur.Last,
			"bid":     	eur.Bid,
			"ask":    	eur.Ask,
			"high":		eur.High,
			"low":		eur.Low,
		}, eur.TimeStamp.Time)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": btcAvgURL,
		}).Error("Unable to create client.NewPoint")
		return
	}
	bp.AddPoint(p2)

	if err := b.Conf.DBConn.Write(bp); err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": btcAvgURL,
		}).Error("Unable to write to DB")
		return
	}
}

func (b *BitcoinAverage) GetEURPrice() AvgPriceTicker {
	var ptEUR AvgPriceTicker
	helpers.GrabJSON(bitcoinAverageEURPriceRate, &ptEUR)
	return ptEUR
}

func (b *BitcoinAverage) GetUSDPrice() AvgPriceTicker {
	var ptUSD AvgPriceTicker
	helpers.GrabJSON(bitcoinAverageUSDPriceRate, &ptUSD)
	return ptUSD
}

type AvgPriceTicker struct {
	// Current lowest ask price
	Ask float64 `json:"ask"`
	// Current highest bid price
	Bid float64 `json:"bid"`
	// Current Price
	Last float64 `json:"last"`
	// Price high
	High float64 `json:"high"`
	// Price Low
	Low float64 `json:"low"`
	// TimeStamp is the time at which this data was issued
	TimeStamp Timestamp `json:"timestamp"`

}
