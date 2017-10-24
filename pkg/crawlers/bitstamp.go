package crawlers

import (
	"seismograph/pkg/helpers"

	"github.com/Sirupsen/logrus"
	"github.com/influxdata/influxdb/client/v2"
)

const (
	bitstampUSDPrice = "https://www.bitstamp.net/api/v2/ticker/btcusd/" // real-time
	bitstampEURPrice = "https://www.bitstamp.net/api/v2/ticker/btceur/" // real-time
)

type BitStamp struct {
	Conf Config
}

func NewBitStampCrawler(conf Config) *BitStamp {
	return &BitStamp{Conf: conf}
}

func (b *BitStamp) Run() {
	defer b.Conf.WaitGroup.Done()
	bsURL := "bitstamp.net"

	bp, err := client.NewBatchPoints(BatchPointConf)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": bsURL,
			"conf":   BatchPointConf,
		}).Error("Unable to create client.NewBatchPoints")
		return
	}

	usd, err := b.GetUSDPrice()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": bsURL,
		}).Error("Failed to get USD prices")
		return
	}
	eur, err := b.GetEURPrice()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": bsURL,
		}).Error("Failed to get EUR prices")
		return
	}

	p1, err := client.NewPoint("price",
		CurrencyTags(USD, bsURL),
		map[string]interface{}{

			"last": 	usd.Last,
			"bid":    	usd.Bid,
			"ask":    	usd.Ask,
			"high":   	usd.High,
			"low":    	usd.Low,
		}, usd.TimeStamp.Time)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": bsURL,
		}).Error("Unable to create client.NewPoint")
		return
	}
	bp.AddPoint(p1)

	p2, err := client.NewPoint("price",
		CurrencyTags(EUR, bsURL),
		map[string]interface{}{

			"last": 	eur.Last,
			"bid":    	eur.Bid,
			"ask":    	eur.Ask,
			"high":   	eur.High,
			"low":    	eur.Low,
		}, eur.TimeStamp.Time)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": bsURL,
		}).Error("Unable to create client.NewPoint")
		return
	}
	bp.AddPoint(p2)

	if err := b.Conf.DBConn.Write(bp); err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": bsURL,
		}).Error("Unable to write to DB")
		return
	}
}

func (b *BitStamp) GetUSDPrice() (PriceTicker, error) {
	var ptUSD PriceTicker
	if err := helpers.GrabJSON(bitstampUSDPrice, &ptUSD); err != nil {
		return ptUSD, err
	}
	return ptUSD, nil
}

func (b *BitStamp) GetEURPrice() (PriceTicker, error) {
	var ptEUR PriceTicker
	if err := helpers.GrabJSON(bitstampEURPrice, &ptEUR); err != nil {
		return ptEUR, err
	}
	return ptEUR, nil
}

type PriceTicker struct {
	// High is the highest price in the last 24hrs
	High float64 `json:"high,string"`
	// Low is the lowest price in the last 24hrs
	Low float64 `json:"low,string"`
	//// Last is the last BTC price
	Last float64 `json:"last,string"`
	//// Bid is the highest buy order
	Bid float64 `json:"bid,string"`
	//// Ask is the lowest sell order
	Ask float64 `json:"ask,string"`
	//// Open is the first price of the day
	Open float64 `json:"open,string"`
	//// Volume is the transaction volume in the last 24hrs
	Volume float64 `json:"volume,string"`
	//// VolumeWeightedAveragePrice is the volume of weighted average price over the last 24hrs
	VolumeWeightedAveragePrice float64 `json:"vwap,string"`
	// TimeStamp is the time at which this data was issued
	TimeStamp Timestamp `json:"timestamp"`
}
