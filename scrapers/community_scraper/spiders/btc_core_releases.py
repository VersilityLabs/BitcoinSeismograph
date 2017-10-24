# -*- coding: utf-8 -*-
import datetime
import urllib

import scrapy
from scrapy import Request

from community_scraper.items import BTCCoreRelease


class BtcCoreReleasesSpider(scrapy.Spider):
    name = 'btc_core_releases'
    allowed_domains = ['bitcoin.org']
    start_urls = ['https://bitcoin.org/en/version-history']

    root = 'https://bitcoin.org'

    def parse(self, response):
        versions = response.css('.versiontext li a')
        for v in versions[:5]:
            yield Request(
                urllib.parse.urljoin(
                    self.root,
                    v.xpath('@href').extract_first()
                ),
                callback=self.parse_version
            )

    def parse_version(self, response):
        rel = BTCCoreRelease()
        url = response.url
        rel['version'] = url.split('/')[-1]
        rel_date = response.css('.versiontext h1 small::text').extract_first()
        try:
            rel['rel_date'] = datetime.datetime.strptime(
                rel_date,
                '%d %B %Y'
            )
        except ValueError:
            self.logger.error(
                'failed to parse created_at value: %s',
                rel_date
            )
        # FIXME: this works, but will probably break at some point
        rel['notable_changes'] = response.xpath(
            '//h2[preceding-sibling::h1[@id="notable-changes"]]/text()'
        ).extract()
        rel['dl_link'] = response.xpath(
            '//div[@class="versiontext"]/p/a/@href'
        ).extract_first()
        yield rel
