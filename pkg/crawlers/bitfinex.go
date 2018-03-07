package crawlers

import (
	"time"

	"seismograph/pkg/helpers"

	"github.com/Sirupsen/logrus"
	"github.com/influxdata/influxdb/client/v2"
)

const (
	bitfinexPriceRate = "https://api.bitfinex.com/v2/ticker/tBTCUSD" // 10min
)

type Bitfinex struct {
	Conf Config
}

func NewBitfinexCrawler(conf Config) *Bitfinex {
	return &Bitfinex{Conf: conf}
}

func (b *Bitfinex) Run() {
	defer b.Conf.WaitGroup.Done()
	bfinURL := "bitfinex.com"

	bp, err := client.NewBatchPoints(BatchPointConf)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": bfinURL,
			"conf":   BatchPointConf,
		}).Error("Unable to create client.NewBatchPoints")
		return
	}

	pr, err := b.GetPriceRate()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": bfinURL,
		}).Error("Failed to get price rates")
		return
	}

	p1, err := client.NewPoint("price",
		CurrencyTags(USD, bfinURL),
		map[string]interface{}{
			"last":    pr.Last,
			"bid":     pr.Bid,
			"ask":     pr.Ask,
			"high":    pr.High,
			"low":     pr.Low,
		}, time.Now())
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": bfinURL,
		}).Error("Unable to create client.NewPoint")
		return
	}
	bp.AddPoint(p1)

	if err := b.Conf.DBConn.Write(bp); err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": bfinURL,
		}).Error("Unable to write to DB")
		return
	}
}

func (b *Bitfinex) GetPriceRate() (CurrencyPriceRate, error) {
	var p CurrencyPair
	if err := helpers.GrabJSON(bitfinexPriceRate, &p); err != nil {
		return CurrencyPriceRate{}, err
	}

	return CurrencyPriceRate{
		p[0],
		p[1],
		p[2],
		p[3],
		p[4],
		p[5],
		p[6],
		p[7],
		p[8],
		p[9],
	}, nil
}

type CurrencyPriceRate struct {
	Bid			float64
	Bid_Size	        float64
	Ask		        float64
	Ask_Size		float64
	Daily_Change            float64
	Daily_Change_Prec       float64
	Last			float64
	Volumne		        float64
	High			float64
	Low	                float64

}

type CurrencyPair []float64
