// Helpers contains globally useful functions.
package helpers

import (
	"encoding/json"
	"net/http"
)

// Max returns the larger of two integers
func Max(a, b int) int {
	if a > b {
		return a
	}
	return b
}

// Min returns the smaller of two integers
func Min(a, b int) int {
	if a < b {
		return a
	}
	return b
}

// Contains returns whether a []string contains a string x
func Contains(s []string, x string) bool {
	for _, e := range s {
		if e == x {
			return true
		}
	}
	return false
}

// GrabJSON fetches a URL, and parses the response body as
// JSON into the target interface{}
func GrabJSON(url string, target interface{}) error {
	resp, err := http.Get(url)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	return json.NewDecoder(resp.Body).Decode(target)
}

// FIXME: figure out a good way to do error handling here
func ParseJSONFloat64(n json.Number) float64 {
	x, err := n.Float64()
	if err != nil {
		return -1
	}
	return x
}
