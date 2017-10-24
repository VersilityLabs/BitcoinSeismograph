package crawlers

import (
	"github.com/influxdata/influxdb/client/v2"
	"sync"
)

type Config struct {
	WaitGroup *sync.WaitGroup
	DBConn    client.Client
}

type Crawler interface {
	GetData(Frequency)
}
type AsyncCrawler interface {
	Run()
}

type Frequency uint8

const (
	SoftRealTime Frequency = iota
	QuarterHourly
	Hourly
	Daily
)

var (
	BatchPointConf = client.BatchPointsConfig{
		Precision: "s",
		Database:  "seismograph",
	}
)

type Currency string

const (
	USD Currency = "USD"
	EUR Currency = "EUR"
	CNY Currency = "CNY"
)

func CurrencyTags(currency Currency, src string) map[string]string {
	return map[string]string{
		"source":   src,
		"currency": string(currency),
	}
}

func CrawlerTags(src string) map[string]string {
	return map[string]string{
		"source": src,
	}

}

func PoolTags (mining_pool string, src string) map[string]string {
	return map[string]string{
		"source":   src,
		"mining_pool": mining_pool,
	}
}
