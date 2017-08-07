# Bitcoin Seismograph

In the future, this will be the place where you can find the source code of the [Bitcoin Seismograph](https://bitcoinseismograph.info). 
This will include the frontend and backend code aswell as the documentation and some configuration files for the infrastructure.

Please also have a look at the FAQ on the [main page](https://bitcoinseismograph.info) (question mark, top right corner).

If you have any questions/suggestions feel free to [open an issue](https://github.com/VersilityLabs/BitcoinSeismograph/issues/new)! 

## Status

We are currently in the process of getting the source code ready for publication (cleaning up, adding documentation, etc.).

We should be ready sometime during autumn.

## Architectural Overview

The application is composed of four separate parts:

1. the API crawlers, written in go
2. the community scrapers, most of which are based upon [scrapy](https://scrapy.org)
3. the REST API, written in Clojure (using [yada](https://github.com/juxt/yada))
4. the frontend, written in ClojureScript, using [reagent](https://reagent-project.github.io/) along with [re-frame](https://github.com/Day8/re-frame) for the heavy lifting and [bulma](http://bulma.io) for styling. We use [c3](http://c3js.org/) for the graphs.

