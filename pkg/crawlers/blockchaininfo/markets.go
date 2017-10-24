package blockchaininfo

import (
	"seismograph/pkg/helpers"
	"time"
)

const (
	tickerURL = "https://blockchain.info/ticker"
)

type Ticker struct {
	// USDPrice is the current price in USD for BTC
	USDPrice Price `json:"USD"`
	// EURPrice is the current price in EUR for BTC
	EURPrice Price `json:"EUR"`
	// CNYPrice is the current price in CNY for BTC
	CNYPrice Price `json:"CNY"`
	// TimeStamp is the time at which the data was queried
	TimeStamp time.Time
}

type Price struct {
	Last   float32 `json:"last"`
	DelayedPrice float32 `json:"15m"`
	Symbol       string  `json:"symbol"`
	Buy          float32 `json:"buy"`
	Sell         float32 `json:"sell"`
}

func ExchangeRates() (*Ticker, error) {
	t := Ticker{TimeStamp: time.Now()}
	err := helpers.GrabJSON(tickerURL, &t)
	if err != nil {
		return &t, err
	}
	return &t, err
}
