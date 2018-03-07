package blockchaininfo

import (
	"seismograph/pkg/helpers"

)

const (
	poolsURL = "https://api.blockchain.info/pools"
)

type HashrateDistribution struct {
	Miners           []Miner
	TotalKnownBlocks float64
}

type Miner struct {
	// Name of the mining entity
	Name string
	// BlockShare is the percentage share the mining entity holds of the total known blocks
	BlockShare float64
	// KnownBlocks is the number of known blocks the mining entity discovered
	KnownBlocks float64
}

func MiningPoolSummary() (*HashrateDistribution, error) {
	var rawPools interface{}
	err := helpers.GrabJSON(poolsURL, &rawPools)
	if err != nil {
		return &HashrateDistribution{}, err
	}

	hd := HashrateDistribution{}
	pools := rawPools.(map[string]interface{})

	for _, blocks := range pools {
		b := blocks.(float64)
		hd.TotalKnownBlocks += b
	}

	for name, blocks := range pools {
		b := blocks.(float64)

		hd.Miners = append(hd.Miners, Miner{Name: name, KnownBlocks: b, BlockShare: float64(b) / float64(hd.TotalKnownBlocks)})
	}

	return &hd, nil
}
