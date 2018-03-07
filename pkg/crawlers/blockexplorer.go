package crawlers

import (
	"time"

	"seismograph/pkg/helpers"

	"github.com/Sirupsen/logrus"
	"github.com/influxdata/influxdb/client/v2"
)

const (
	blockExplorerBlockHeight = "https://blockexplorer.com/api/status?q=getBlockCount" // real-time
	blockExplorerDifficulty = "https://blockexplorer.com/api/status?q=getDifficulty"
	blockExplorerLastHash = "https://blockexplorer.com/api/status?q=getLastBlockHash"
)

type BlockExplorer struct {
	Conf Config
}

func NewBlockExplorerCrawler(conf Config) *BlockExplorer {
	return &BlockExplorer{Conf: conf}
}

func (b *BlockExplorer) Run() {
	defer b.Conf.WaitGroup.Done()
	blockExplorerURL := "blockexplorer.com"

	bp, err := client.NewBatchPoints(BatchPointConf)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": blockExplorerURL,
			"conf":   BatchPointConf,
		}).Error("Unable to create client.NewBatchPoints")
		return
	}

	bh, err := b.GetBlockHeight()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": blockExplorerURL,
		}).Error("Failed to get block height")
		return
	}

	d, err := b.GetDifficulty()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": blockExplorerURL,
		}).Error("Failed to get difficulty")
		return
	}

	lh, err := b.GetLastHash()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": blockExplorerURL,
		}).Error("Failed to get last hash")
		return
	}

	p1, err := client.NewPoint("network",
		CrawlerTags(blockExplorerURL),
		map[string]interface{}{
			"height":      bh.Height,
			"difficulty":  d.Difficulty,
			"lastHash":	lh.LastBlockHash,
		},time.Now())
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": blockExplorerURL,
		}).Error("Unable to create client.NewPoint")
		return
	}
	bp.AddPoint(p1)

	if err := b.Conf.DBConn.Write(bp); err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": blockExplorerURL,
		}).Error("Unable to write to DB")
		return
	}
}

func (b *BlockExplorer) GetBlockHeight() (BlockExplorerHeight, error) {
	var n BlockExplorerHeight
	if err := helpers.GrabJSON(blockExplorerBlockHeight, &n); err != nil {
		return n, err
	}
	return n, nil
}

func (b *BlockExplorer) GetDifficulty() (BlockExplorerDifficulty, error) {
	var n BlockExplorerDifficulty
	if err := helpers.GrabJSON(blockExplorerDifficulty, &n); err != nil {
		return n, err
	}
	return n, nil
}

func (b *BlockExplorer) GetLastHash() (BlockExplorerLastHash, error) {
	var n BlockExplorerLastHash
	if err := helpers.GrabJSON(blockExplorerLastHash, &n); err != nil {
		return n, err
	}
	return n, nil
}


type BlockExplorerHeight struct {
	Height int `json:"blockcount"`
}

type BlockExplorerDifficulty struct {
	Difficulty float64 `json:"difficulty"`
}

type BlockExplorerLastHash struct {
	LastBlockHash string `json:"lastblockhash"`
}