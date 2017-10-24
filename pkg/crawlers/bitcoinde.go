package crawlers

import (
	"fmt"
	"os"
	"time"

	"seismograph/pkg/helpers"

	"github.com/Sirupsen/logrus"
	"github.com/influxdata/influxdb/client/v2"
)

const bitcoinDERate = "https://bitcoinapi.de/v1/%s/rate.json" // 10min

type BitcoinDE struct {
	Conf Config
}

func NewBitcoinDECrawler(conf Config) *BitcoinDE {
	return &BitcoinDE{Conf: conf}
}

func (b *BitcoinDE) Run() {
	defer b.Conf.WaitGroup.Done()
	btcDEURL := "bitcoin.de"

	// TODO: need API key!
	apiKey, found := os.LookupEnv("BITCOIN_DE_API_KEY")
	if !found {
		logrus.WithFields(logrus.Fields{
			"source": btcDEURL,
		}).Error("Missing API Key")
		return
	}

	bp, err := client.NewBatchPoints(BatchPointConf)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": btcDEURL,
			"conf":   BatchPointConf,
		}).Error("Unable to create client.NewBatchPoints")
		return
	}

	wr, err := b.GetRate(apiKey)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": btcDEURL,
		}).Error("Failed to get average weighted rate")
		return
	}

	p1, err := client.NewPoint("price",

		CurrencyTags(EUR, btcDEURL),
		map[string]interface{}{
			"last": wr.WeightedRate,
		}, time.Now())

	bp.AddPoint(p1)

	if err := b.Conf.DBConn.Write(bp); err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": btcDEURL,
		}).Error("Unable to write to DB")
		return
	}
}

// TODO: which currency is this???
func (b *BitcoinDE) GetRate(apiKey string) (WeightedRate, error) {
	var wr WeightedRate
	targetURL := fmt.Sprintf(bitcoinDERate, apiKey)
	if err := helpers.GrabJSON(targetURL, &wr); err != nil {
		return wr, err
	}
	return wr, nil
}

type WeightedRate struct {
	WeightedRate    float64 `json:"rate_weighted"`
	WeightedRate3H  float64 `json:"rate_weighted_3h"`
	WeightedRate12H float64 `json:"rate_weighted_12h"`
}
