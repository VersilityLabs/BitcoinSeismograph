package crawlers

import (
	"seismograph/pkg/helpers"

	"github.com/Sirupsen/logrus"
	"github.com/influxdata/influxdb/client/v2"
)

const bitcoinchainNetworkStatus = "https://api-r.bitcoinchain.com/v1/status" // 10min

type Bitcoinchain struct {
	Conf Config
}

func NewBitcoinchainCrawler(conf Config) *Bitcoinchain {
	return &Bitcoinchain{Conf: conf}
}

func (b *Bitcoinchain) Run() {
	defer b.Conf.WaitGroup.Done()
	btcChainURL := "bitcoinchain.com"

	bp, err := client.NewBatchPoints(BatchPointConf)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": btcChainURL,
			"conf":   BatchPointConf,
		}).Error("Unable to create client.NewBatchPoints")
		return
	}

	ns, err := b.GetNetworkStatus()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": btcChainURL,
		}).Error("Failed to get network status")
		return
	}

	p1, err := client.NewPoint("network",
		CrawlerTags(btcChainURL),
		map[string]interface{}{
			"height":      ns.Height,
			"blockReward": ns.CurrentBlockReward,
			"difficulty":  ns.Difficulty,
			"lastHash":    ns.Hash,
		}, ns.TimeStamp.Time)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": btcChainURL,
		}).Error("Unable to create client.NewPoint")
		return
	}
	bp.AddPoint(p1)

	if err := b.Conf.DBConn.Write(bp); err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": btcChainURL,
		}).Error("Unable to write to DB")
		return
	}
}

func (b *Bitcoinchain) GetNetworkStatus() (NetworkStatus, error) {
	var n NetworkStatus
	if err := helpers.GrabJSON(bitcoinchainNetworkStatus, &n); err != nil {
		return n, err
	}
	return n, nil
}

type NetworkStatus struct {
	// Height is the height of the blockchain
	Height int `json:"height"`
	// Hash is the hash value of the current block
	Hash string `json:"hash"`
	// PreviousHash is the hash value of the previous block
	PreviousHash string `json:"prev_hash"`
	// Current difficulty of Bitcoin Mining
	Difficulty float64 `json:"difficulty,string"`
	// CurrentBlockReward is the current reward (in BTC) for solving a block
	CurrentBlockReward float32 `json:"reward,string"`
	// NextBlockReward is the reward (in BTC) for solving the next block
	NextBlockReward float32 `json:"next_block_reward"`
	// TimeStamp is the time at which these network states were collected
	TimeStamp Timestamp `json:"time"`
}
