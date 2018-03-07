package crawlers

import (
	"time"

	"seismograph/pkg/helpers"

	"github.com/Sirupsen/logrus"
	"github.com/influxdata/influxdb/client/v2"
)

const (
	blockrioTicker = "http://btc.blockr.io/api/v1/coin/info" // real-time
)

type Blockrio struct {
	Conf Config
}

func NewBlockrioCrawler(conf Config) *Blockrio {
	return &Blockrio{Conf: conf}
}

func (b *Blockrio) Run() {
	defer b.Conf.WaitGroup.Done()
	blkrURL := "blockr.io"

	bp, err := client.NewBatchPoints(BatchPointConf)
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": blkrURL,
			"conf":   BatchPointConf,
		}).Error("Unable to create client.NewBatchPoints")
		return
	}

	d, err := b.GetDifficultyTicker()
	if err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": blkrURL,
		}).Error("Failed to get difficulty data")
		return
	}

	p1, err := client.NewPoint("network",
		CrawlerTags(blkrURL),
		map[string]interface{}{
			"height":         d.LastBlock.Height,
			"difficulty":     d.LastBlock.Difficulty,
			"retarget_in":    d.NextDifficulty.Retarget,
			"retarget_block": d.NextDifficulty.Retarget_Block,
		}, time.Now())
	bp.AddPoint(p1)

	if err := b.Conf.DBConn.Write(bp); err != nil {
		logrus.WithFields(logrus.Fields{
			"error":  err.Error(),
			"source": blkrURL,
		}).Error("Unable to write to DB")
		return
	}
}

func (b *Blockrio) GetDifficultyTicker() (BlockrioDifficultyTicker, error) {
	t := BlockrioData{}
	if err := helpers.GrabJSON(blockrioTicker, &t); err != nil {
		return t.Data, err
	}
	return t.Data, nil
}

type BlockrioData struct {
	Data BlockrioDifficultyTicker `json:"data"`
}

type BlockrioDifficultyTicker struct {
	LastBlock      BlockrioLastBlock `json:"last_block"`
	NextDifficulty BlockrioNextDiff  `json:"next_difficulty"`
}

type BlockrioLastBlock struct {
	Height     int     `json:"nb"`
	Fee        float32 `json:"fee,string"`
	Difficulty float32 `json:"difficulty,string"`
}

type BlockrioNextDiff struct {
	Retarget       int `json:"retarget_in"`
	Retarget_Block int `json:"retarget_block"`
}
