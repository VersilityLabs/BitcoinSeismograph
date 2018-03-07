package blockchaininfo

import (
	"io/ioutil"
	"net/http"
	"strconv"
)

const (
	transactionCountURL            = "https://blockchain.info/q/24hrtransactioncount"
	unconfirmedTransactionCountURL = "https://blockchain.info/q/unconfirmedcount"
	blockIntervalURL               = "https://blockchain.info/q/interval"
	difficultyTargetURL            = "https://blockchain.info/q/getdifficulty"
	USDMarketCapURL                = "https://blockchain.info/q/marketcap"
)

func TransactionCount() (float64, error) {
	return getSingleTextValue(transactionCountURL)
}

func UnconfirmedTransactionCount() (float64, error) {
	return getSingleTextValue(unconfirmedTransactionCountURL)
}

func BlockInterval() (float64, error) {
	return getSingleTextValue(blockIntervalURL)
}

func DifficultyTarget() (float64, error) {
	return getSingleTextValue(difficultyTargetURL)
}

func MarketCap() (float64, error) {
	return getSingleTextValue(USDMarketCapURL)
}

func getSingleTextValue(url string) (float64, error) {
	resp, err := http.Get(url)
	if err != nil {
		return 0, err
	}
	defer resp.Body.Close()

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return 0, err
	}

	bodyStr := string(body)

	result, err := strconv.ParseFloat(bodyStr, 64)
	if err != nil {
		return 0, err
	}
	return result, nil
}
