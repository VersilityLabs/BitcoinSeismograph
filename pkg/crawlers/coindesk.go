package crawlers

import (
	"time"

	"seismograph/pkg/helpers"

	"github.com/Sirupsen/logrus"
	"github.com/influxdata/influxdb/client/v2"
)

const coindeskCurrentPrice = "http://api.coindesk.com/v1/bpi/currentprice.json"

type CoinDesk struct {
	Conf Config
}

func NewCoinDeskCrawler(conf Config) *CoinDesk {
	return &CoinDesk{Conf: conf}
}

func (c *CoinDesk) Run() {
	defer c.Conf.WaitGroup.Done()
	cdURL := "coindesk.com"

	bp, err := client.NewBatchPoints(BatchPointConf)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": cdURL,
			"conf":   BatchPointConf,
		}).Error("Unable to create client.NewBatchPoints")
		return
	}

	p, err := c.GetPrices()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": cdURL,
		}).Error("Failed to get price data")
		return
	}

	p1, err := client.NewPoint("price",
		CurrencyTags(USD, cdURL),
		map[string]interface{}{
			"last": p.BitcoinPriceIndex.USD.Rate,
		}, p.Time.ISOTime)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": cdURL,
		}).Error("Unable to create client.NewPoint")
		return
	}
	bp.AddPoint(p1)

	p2, err := client.NewPoint("price",
		CurrencyTags(EUR, cdURL),
		map[string]interface{}{
			"last": p.BitcoinPriceIndex.EUR.Rate,
		}, p.Time.ISOTime)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": cdURL,
		}).Error("Unable to create client.NewPoint")
		return
	}
	bp.AddPoint(p2)

	if err := c.Conf.DBConn.Write(bp); err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": cdURL,
		}).Error("Unable to write to DB")
		return
	}
}

func (b *CoinDesk) GetPrices() (PriceIndex, error) {
	var pi PriceIndex
	if err := helpers.GrabJSON(coindeskCurrentPrice, &pi); err != nil {
		return pi, err
	}
	return pi, nil
}

type PriceIndex struct {
	Time struct {
		ISOTime time.Time `json:"updatedISO"`
	} `json:"time"`
	BitcoinPriceIndex struct {
		USD PI `json:"USD"`
		GBP PI `json:"GBP"`
		EUR PI `json:"EUR"`
	} `json:"bpi"`
}

type PI struct {
	Code   string  `json:"code"`
	Symbol string  `json:"symbol"`
	Rate   float32 `json:"rate_float"`
	// rate string and currency description
}
