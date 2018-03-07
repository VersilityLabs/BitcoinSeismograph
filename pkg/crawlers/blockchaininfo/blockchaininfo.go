package blockchaininfo

import (
	"seismograph/pkg/crawlers"

	"time"

	"errors"

	"github.com/Sirupsen/logrus"
	"github.com/influxdata/influxdb/client/v2"
)

type BlockChainInfo struct {
	Conf      crawlers.Config
	CrawlType crawlers.Frequency
}

const (
	src = "blockchain.info"
)

var (
	defaultTags = crawlers.CrawlerTags(src)
)

func New(crawlType crawlers.Frequency, conf crawlers.Config) *BlockChainInfo {
	return &BlockChainInfo{Conf: conf, CrawlType: crawlType}
}

func (b *BlockChainInfo) Run() {
	defer b.Conf.WaitGroup.Done()
	bp, err := client.NewBatchPoints(crawlers.BatchPointConf)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": src,
			"conf":   crawlers.BatchPointConf,
		}).Error("Unable to create client.NewBatchPoints")
		return
	}

	points, err := gatherData(b.CrawlType)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": src,
		}).Error("Failed to gather data")
		return
	}

	bp.AddPoints(points)
	if err := b.Conf.DBConn.Write(bp); err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": src,
		}).Error("Unable to write to DB")
		return
	}
}

func gatherData(mt crawlers.Frequency) ([]*client.Point, error) {
	switch mt {
	case crawlers.SoftRealTime:
		return SoftRealTime()
	case crawlers.QuarterHourly:
		return QuarterHour()
	case crawlers.Hourly:
		return Hourly()
	case crawlers.Daily:
		return Daily()
	}
	return nil, errors.New("invalid Frequency")
}

func SoftRealTime() ([]*client.Point, error) {
	points := make([]*client.Point, 0)
	t := time.Now()

	tick, err := ExchangeRates()
	utc, err := UnconfirmedTransactionCount()

	p1, err := client.NewPoint("network",
		defaultTags,
		map[string]interface{}{
			"unconfirmedCount": utc,
		}, t)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": src,
		}).Error("Unable to create client.NewPoint")
		return points, err
	}
	points = append(points, p1)

	p2, err := client.NewPoint("price",
		crawlers.CurrencyTags(crawlers.USD, src),
		map[string]interface{}{
			"ask":  tick.USDPrice.Buy,
			"bid":  tick.USDPrice.Sell,
			"last": tick.USDPrice.Last,
		}, t)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": src,
		}).Error("Unable to create client.NewPoint")
		return points, err
	}
	points = append(points, p2)

	p3, err := client.NewPoint("price",
		crawlers.CurrencyTags(crawlers.EUR, src),
		map[string]interface{}{
			"ask":  tick.EURPrice.Buy,
			"bid":  tick.EURPrice.Sell,
			"last": tick.EURPrice.Last,
		}, t)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": src,
		}).Error("Unable to create client.NewPoint")
		return points, err
	}
	points = append(points, p3)

	p4, err := client.NewPoint("price",
		crawlers.CurrencyTags(crawlers.CNY, src),
		map[string]interface{}{
			"ask":  tick.CNYPrice.Buy,
			"bid":  tick.CNYPrice.Sell,
			"last": tick.CNYPrice.Last,
		}, t)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": src,
		}).Error("Unable to create client.NewPoint")
		return points, err
	}
	points = append(points, p4)
	return points, nil
}

func QuarterHour() ([]*client.Point, error) {
	points := make([]*client.Point, 0)

	tc, err := TransactionCount()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": src,
		}).Error("TransactionCount failed")
		return points, err
	}

	i, err := BlockInterval()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": src,
		}).Error("BlockInterval broke!")
		return points, err
	}

	d, err := DifficultyTarget()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": src,
		}).Error("DifficultyTarget broke!")
		return points, err
	}

	point1, err := client.NewPoint(
		"network",
		defaultTags,
		map[string]interface{}{
			"transactionCount": tc,
			"blockInterval":    i,
			"difficulty":       d,
		},
		time.Now(),
	)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": src,
		}).Error("Unable to create client.NewPoint")
		return points, err
	}

	points = append(points, point1)

	p, err := MiningPoolSummary()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": src,
		}).Error("Hashrate Distribution failed")
		return points, err
	}

	for _, pool := range p.Miners {
		point, err := client.NewPoint(
			"pools",
			crawlers.PoolTags(pool.Name, src),
			map[string]interface{}{
				"share":            pool.KnownBlocks,
				"percentage_share": pool.BlockShare,
			},
			time.Now(),
		)

		if err != nil {
			logrus.WithFields(logrus.Fields{
				"error":  err.Error(),
				"source": src,
			}).Error("Unable to create client.NewPoint")
			return points, err
		}

		points = append(points, point)
	}

	return points, nil
}

func Hourly() ([]*client.Point, error) {
	points := make([]*client.Point, 0)

	mc, err := MarketCap()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": src,
		}).Error("MarketCap broke!")
		return points, err
	}

	point, err := client.NewPoint(
		"markets",
		crawlers.CurrencyTags(crawlers.USD, src),
		map[string]interface{}{
			"marketCap": mc,
		},
		time.Now(),
	)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": src,
		}).Error("Unable to create client.NewPoint")
		return points, err
	}
	points = append(points, point)
	return points, nil
}

func Daily() ([]*client.Point, error) {
	points := make([]*client.Point, 0)

	dt, err := DifficultyTarget()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": src,
		}).Error("DifficultyTarget broke!")
		return points, err
	}

	mct, err := MedianConfirmationTime()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": src,
		}).Error("MedianConfirmationTime broke!")
		return points, err
	}

	p1, err := client.NewPoint(
		"network",
		defaultTags,
		map[string]interface{}{
			"difficulty":             dt,
			"medianConfirmationTime": mct,
		},
		time.Now(),
	)
	points = append(points, p1)
	return points, nil
}
