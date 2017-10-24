# -*- coding: utf-8 -*-
import datetime
import re
import urllib.parse

import scrapy
from scrapy import Request

from community_scraper.items import ForumThread


class ForumBitcoinComSpider(scrapy.Spider):
    name = 'forum_bitcoin_com'
    allowed_domains = ['forum.bitcoin.com']

    root = 'https://forum.bitcoin.com/'
    targets = {
        'bitcoin-discussion': 'discussion',
        'press': 'press',
        'legal': 'legal',
        'important-announcements': 'announcements',
        'economics': 'economics',
        'speculation': 'speculation',
        'trading-discussion': 'trading',
    }

    def __init__(self):
        super(ForumBitcoinComSpider, self).__init__()
        self.pattern = re.compile('-([\w\d]+).html')

    def start_requests(self):
        return [Request(self.root + t,
                        callback=self.parse_board,
                        meta={'board': name})
                for t, name in self.targets.items()]

    def parse_board(self, response):
        threads = response.css(
            '.forumbg:not(.announcement) .topiclist.topics .row:not(.sticky)'
        )

        lim = self.settings.get('MAX_THREADS_PER_BOARD', 10)
        for t in threads[:lim]:
            link = t.css('.topictitle')

            thread = ForumThread()
            thread['board'] = response.meta['board']
            thread['title'] = link.xpath('text()').extract_first()
            href = link.xpath('@href').extract_first()

            p = urllib.parse.urlparse(href)
            p = p._replace(query='')  # remove querystring

            perma = p.geturl()

            thread['permalink'] = perma
            thread['id'] = self.pattern.search(perma).groups()[0]
            thread['replies'] = self.parse_count(
                t.xpath('dl/dd[@class="posts"]/text()').extract_first()
            )
            thread['views'] = self.parse_count(
                t.xpath('dl/dd[@class="views"]/text()').extract_first()
            )

            yield Request(href,
                          callback=self.parse_thread,
                          meta={'thread': thread})

    def parse_thread(self, response):
        t = response.meta['thread']

        # Note: why not do this in parse_board?
        # because it's much easier to do it here :p
        created_at = response.css('.postbody .author a::text').extract_first()
        try:
            t['created_at'] = datetime.datetime.strptime(
                created_at,
                '%a %b %d, %Y %I:%M %p'
            )
        except ValueError:
            self.logger.error(
                'failed to parse created_at value: %s',
                created_at
            )

        replies = response.css('.postbody .content')
        t['op'] = replies.extract_first()
        yield t

    @staticmethod
    def parse_count(count_str):
        if not count_str:
            return 0
        # first item = the count, second item = <dfn> value
        parts = count_str.split(' ')
        count = parts[0]
        try:
            return int(count)
        except ValueError:
            return 0
