# Backend Instructions

https://portal.influxdata.com/downloads

## Configuration

General parameters:
- `PORT` (default: `3020`)

Connection to **InfluxDB** (time series based blockchain data): 
- `INFLUX_HOST` (default: `localhost`)
- `INFLUX_PORT` (default: `8086`)
- `INFLUX_SCHEME` (default: `http`)
- `INFLUX_USERNAME` (default: `root`, works when no access control is configured)
- `INFLUX_PASSWORD` (default: `root`, works when no access control is configured)
- `INFLUX_DB` - Database identifier where the crawled bitcoin data is stored. (default: `seismograph`)

Connection to **Elastic Search** (text based community data):
- `ES_URL` (default: `http://localhost:9200`)

# Usage

## Develop

Commands:
- Use `lein repl` for experimenting with internal code.
- Use `lein run dev` for starting the api.

Configuration: 
- Alternatively create a `profiles.clj` in the project folder with the following content to specify the environment parameters.
```clojure
{:dev {:env {:influx-host "..."    ; Replace ... with your InfluxDB host.
             :es-url      "..."}}} ; Replace ... with your Elasticsearch url. 
```

## Test

## Build

## Run