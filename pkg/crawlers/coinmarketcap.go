package crawlers

import (
	"seismograph/pkg/helpers"

	"github.com/Sirupsen/logrus"
	"github.com/influxdata/influxdb/client/v2"
)

const coinMarketCapMarketCaps = "https://api.coinmarketcap.com/v1/ticker/bitcoin/" // 1h

type CoinMarketCap struct {
	Conf Config
}

func NewCoinMarketCapCrawler(conf Config) *CoinMarketCap {
	return &CoinMarketCap{Conf: conf}
}

func (c *CoinMarketCap) Run() {
	defer c.Conf.WaitGroup.Done()
	cmpURL := "coinmarketcap.com"

	bp, err := client.NewBatchPoints(BatchPointConf)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": cmpURL,
			"conf":   BatchPointConf,
		}).Error("Unable to create client.NewBatchPoints")
		return
	}

	mc, err := c.GetMarketCap()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": cmpURL,
		}).Error("Failed to get market capitalization data")
		return
	}

	btc := mc[0]
	p1, err := client.NewPoint("price",
		CurrencyTags(USD, cmpURL),
		map[string]interface{}{
			"last": btc.PriceUSD,
		}, btc.LastEdited.Time)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": cmpURL,
		}).Error("Unable to create client.NewPoint")
		return
	}
	bp.AddPoint(p1)

	p2, err := client.NewPoint("markets",
		CurrencyTags(USD, cmpURL),
		map[string]interface{}{
			"marketCap":            btc.USDMarketCap,
			"volume" :		btc.USDVolume24h,
			"availableCoins": 	btc.AvailableCoins,
		}, btc.LastEdited.Time)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": cmpURL,
		}).Error("Unable to create client.NewPoint")
		return
	}
	bp.AddPoint(p2)

	if err := c.Conf.DBConn.Write(bp); err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": cmpURL,
		}).Error("Unable to write to DB")
		return
	}
}

func (b *CoinMarketCap) GetMarketCap() (MarketCaps, error) {
	var mc MarketCaps
	if err := helpers.GrabJSON(coinMarketCapMarketCaps, &mc); err != nil {
		return mc, err
	}
	return mc, nil
}

type MarketCaps []MarketCap

type MarketCap struct {
	// CurrencyID is an internal identifier of a currency
	CurrencyID string `json:"id"`
	// CurrencyName is the english name of a currency
	CurrencyName string `json:"name"`
	// CurrencySymbol is a symbolic representation of a currency
	CurrencySymbol string `json:"symbol"`
	// CurrencyRank is a ranking of the currency's market capitalization
	CurrencyRank int `json:"rank,string"`
	// PriceUSD is the price in USD for one unit of the currency
	PriceUSD float64 `json:"price_usd,string"`
	// PriceBTC is the price in BTC for one unit of the currency
	PriceBTC float64 `json:"price_btc,string"`
	// USDVolume_24h is the traded volume in the last 24hrs
	USDVolume24h float64 `json:"24h_volume_usd,string"`
	// USDMarketCap is the market capitalization in USD
	USDMarketCap float64 `json:"market_cap_usd,string"`
	// AvailableCoins represents the available amount of coins
	AvailableCoins float64 `json:"available_supply,string"`
	// TotalCoins is the total number of coins for the currency
	TotalCoins float64 `json:"total_supply,string"`
	// MarketCapChange_1h is the percentage change in MarketCap in the last hour
	MarketCapChange1h float64 `json:"percent_change_1h,string"`
	// MarketCapChange_24h is the percentage change in MarketCap in the last 24hrs
	MarketCapChange24h float64 `json:"percent_change_24h,string"`
	// MarketCapChange_7d is the percentage change in MarketCap in the last week
	MarketCapChange7d float64 `json:"percent_change_7d,string"`
	// LastEdited is a timestamp indicating when the MarketCap was calculated
	LastEdited Timestamp `json:"last_updated"`
}
