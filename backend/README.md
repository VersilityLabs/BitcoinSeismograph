# Backend Instructions

Backend of the Bitcoin Seismograph. Serves and aggregates data from multiple data sources:

- An Elasticsearch instance containing all news, forum and reddit scrapes from the Bitcoin Seismograph python 
scrapers/crawlers (see separate project folder/branch).

- An Influx DB instance containing all bitcoin price, market, blockchain and mining statistics 
(crawlers see separate project folder/branch).

## Setup

- Install Influx DB. e.g. by using [https://portal.influxdata.com/downloads]().

- Install Elasticsearch.

- Fill instances with appropriate data by using the crawlers and scrapers.

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
- Instead of using environment variables you can create a `profiles.clj` in the project folder with the following 
content to specify the environment parameters.
```clojure
{:dev {:env {:influx-host "..."    ; Replace ... with your InfluxDB host.
             :es-url      "..."}}} ; Replace ... with your Elasticsearch url. 
```

## Test

- Check functionality by verifying the api endpoints `/api/dashboard`, `/api/text` and `/api/graph`.

## Build

- Use `lein uberjar` to generate a jar files in `target/uberjar/`.

## Run

- Setup environment variables, Influx DB and Elasticsearch.
- Use `lein run` or `java -jar backend-standalone.jar`.