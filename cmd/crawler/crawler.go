package main

import (
	"os"

	"github.com/Sirupsen/logrus"
	"github.com/influxdata/influxdb/client/v2"
	"github.com/urfave/cli"
	"seismograph/pkg/crawlers"
	"seismograph/pkg/crawlers/blockchaininfo"
	"sync"
)

func main() {
	var providersArgs cli.StringSlice
	log := logrus.New()

	c, err := client.NewHTTPClient(client.HTTPConfig{
		Addr: "http://localhost:8086", // TODO: make this configurable
	})
	if err != nil {
		log.WithField("err", err.Error()).Fatal("Error creating InfluxDB Client")
	}

	q := client.NewQuery("CREATE DATABASE seismograph", "", "")
	if response, err := c.Query(q); err != nil || response.Error() != nil {
		if err != nil {
			log.WithField("err", err.Error()).Fatal("Error running InfluxDB Query")
		}
		log.WithField("influxDBResponse", response.Error()).Fatal("InfluxDB Error")
	}
	defer c.Close()

	app := cli.NewApp()
	app.Name = "BitcoinSeismograph Crawler"
	app.Usage = "retrieve quantitative data from bitcoin API-providers"
	app.Version = "0.1.0"
	app.Copyright = "(c) 2017 Versility Labs GmbH & Marcel Morisse"

	app.Flags = []cli.Flag{
		cli.StringSliceFlag{

			Name:  "provider, p",
			Value: &providersArgs,

			Usage: "To run the crawler to grab data, you can set multiple 'PROVIDER' or 'P' values, valid provider names are: {blockchaininfo_1m|blockchaininfo_15m|blockchaininfo_1h|blockchaininfo_1d|bitcoinAverage|bitcoinchain|bitcoinde|bitcoinexplorer|bitfinex|bitstamp|blockrio|coindesk|coinmarketcap|kraken}" +
				"The provider bitcoinde requires a valid API key from bitcoin.de",
		},
	}

	var wg sync.WaitGroup

	conf := crawlers.Config{
		WaitGroup: &wg,
		DBConn:    c,
	}

	providers := map[string]crawlers.AsyncCrawler{
		"blockchaininfo_1m":  blockchaininfo.New(crawlers.SoftRealTime, conf),
		"blockchaininfo_15m": blockchaininfo.New(crawlers.QuarterHourly, conf),
		"blockchaininfo_1h":  blockchaininfo.New(crawlers.Hourly, conf),
		"blockchaininfo_1d":  blockchaininfo.New(crawlers.Daily, conf),
		"bitcoinAverage":     crawlers.NewBitcoinAverageCrawler(conf),
		"bitcoinchain":       crawlers.NewBitcoinchainCrawler(conf),
		"bitcoinde":          crawlers.NewBitcoinDECrawler(conf),
		"bitcoinexplorer":    crawlers.NewBlockExplorerCrawler(conf),
		"bitfinex":           crawlers.NewBitfinexCrawler(conf),
		"bitstamp":           crawlers.NewBitStampCrawler(conf),
		"blockrio":           crawlers.NewBlockrioCrawler(conf),
		"coindesk":           crawlers.NewCoinDeskCrawler(conf),
		"coinmarketcap":      crawlers.NewCoinMarketCapCrawler(conf),
		"kraken":             crawlers.NewKrakenCrawler(conf),
	}

	app.Action = func(c *cli.Context) error {
		p := c.StringSlice("provider")
		// validate provided flags
		if len(p) == 0 {
			return cli.NewExitError("No API-provider specified", 101)
		}

		// run providers for the data kind
		for _, x := range p {
			provider, found := providers[x]
			if found {
				wg.Add(1)
				go provider.Run()
			}
		}
		wg.Wait() // FIXME: should this go inside Action or outside?

		return nil
	}

	app.Run(os.Args)
}
