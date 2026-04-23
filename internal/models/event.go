package models

import (
	"crypto/md5"
	"fmt"
	"strings"
	"time"
)

type Event struct {
	Artist   string
	Location string
	DateTime time.Time
	URL      string
}

func (e Event) Checksum() string {
	input := strings.Join([]string{
		normalise(e.Location),
		normalise(e.DateTime.Format("2006-01-02T15:04:05")),
	}, "|")
	return fmt.Sprintf("%x", md5.Sum([]byte(input)))
}

func (e Event) Key() string {
	return "banquet:event:" + e.Checksum()
}

func normalise(value string) string {
	return strings.ToLower(strings.TrimSpace(value))
}
