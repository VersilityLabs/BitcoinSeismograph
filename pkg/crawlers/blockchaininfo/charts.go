package blockchaininfo

import (
	"seismograph/pkg/helpers"

)

const (
	mctURL = "https://api.blockchain.info/charts/Median-Confirmation-Time?timespan=1week" // be nice
)

type Chart struct {
	Values []Coordinate `json:"values"`
	Name   string       `json:"name"`
	Period string       `json:"period"`
	Unit   string       `json:"unit"`
}

type Coordinate struct {
	// timestamp is an x-coordinate in the graph, which represents a UNIX timestamp (in seconds)
	Timestamp int `json:"x"`
	// val is the y-dimension of the graph.
	// It can have different meanings/value-types depending on the graph.
	Val float32 `json:"y"`
}

func MedianConfirmationTime() (float32, error) {
	var result Chart
	err := helpers.GrabJSON(mctURL, &result)
	if err != nil {
		return 0, err
	}

	l := len(result.Values)

	// we only really care about the last value (yesterday)
	mct := result.Values[l-1]

	return mct.Val, nil

}
