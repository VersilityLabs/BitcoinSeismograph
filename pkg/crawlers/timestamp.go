package crawlers

import (
	"fmt"
	"strconv"
	"strings"
	"time"
)

type Timestamp struct {
	time.Time
}

func (t *Timestamp) UnmarshalJSON(b []byte) error {
	// FIXME: this seems like a really strange workaround, I would have thought that ",string" in the struct tag already takes care of this
	s := strings.Trim(string(b), `"`)
	ts, err := strconv.Atoi(s)
	if err != nil {
		return err
	}

	t.Time = time.Unix(int64(ts), 0)

	return nil
}

func (t *Timestamp) MarshalJSON() ([]byte, error) {
	ts := t.Time.Unix()
	stamp := fmt.Sprint(ts)

	return []byte(stamp), nil
}
