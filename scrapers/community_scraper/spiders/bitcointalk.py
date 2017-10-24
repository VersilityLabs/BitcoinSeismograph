# -*- coding: utf-8 -*-
import datetime
import re
import urllib.parse

import scrapy
from scrapy import Request

from community_scraper.items import ForumThread


class BitcointalkSpider(scrapy.Spider):
    name = 'bitcointalk'
    allowed_domains = ['bitcointalk.org']

    root = 'https://bitcointalk.org/index.php?'
    targets = {
        '1.0': 'discussion',
        '74.0': 'legal',
        '77.0': 'press',
        '87.0': 'announcements',
        '6.0': 'Development & Technical Discussion',
        '14.0': 'Mining',
        '81.0': 'Mining Speculation',
        '7.0': 'Economics',
        '57.0': 'Speculation',
        '8.0': 'Trading Discussion',
    }

    def __init__(self):
        super(BitcointalkSpider, self).__init__()
        self.id_pattern = re.compile('topic=([\w\d]+)')
        self.today_pattern = re.compile('\s+at\s+\d{2}:\d{2}:\d{2} (?:AM|PM)', re.IGNORECASE)

    def start_requests(self):
        return [Request(self.root + urllib.parse.urlencode({'board': b}),
                        callback=self.parse_board,
                        meta={'board': name})
                for b, name in self.targets.items()]

    def parse_board(self, response):
        wrappers = response.css('#bodyarea .tborder')
        thread_list = wrappers[len(wrappers) - 2]
        threads = thread_list.css('tr:not(.ignored_topic)')

        lim = self.settings.get('MAX_THREADS_PER_BOARD', 10)
        # first element is the header
        for t in threads[1:lim]:
            subject = t.xpath('td[3]')
            # check and skip sticky threads
            if len(subject.xpath('img')) > 0:
                self.logger.debug('sticky thread %s',
                                  subject.css('span a').extract_first())
                continue

            link = subject.css('span a')

            thread = ForumThread()
            thread['board'] = response.meta['board']
            thread['title'] = link.xpath('text()').extract_first()
            href = link.xpath('@href').extract_first()
            thread['permalink'] = href
            thread['id'] = self.id_pattern.search(href).groups()[0]
            thread['replies'] = self.parse_integer(
                t.xpath('td[5]/text()').extract_first()
            )
            thread['views'] = self.parse_integer(
                t.xpath('td[6]/text()').extract_first()
            )

            yield Request(href,
                          callback=self.parse_thread,
                          meta={'thread': thread})

    def parse_thread(self, response):
        t = response.meta['thread']

        op = response.xpath('//td[@class="td_headerandpost"][1]')

        # NOTE: this is actually a "last edit" value
        created_at = op.css(
            '.smalltext span.edited::text'
        ).extract_first()

        if created_at is None:
            # this is the actual "created" value
            created_at = op.css(
                '.smalltext::text'
            ).extract_first()

            # handle posts created today, which are displayed differently
            if self.today_pattern.match(created_at):
                now = datetime.datetime.now()
                it = created_at[-11:]

                d = self.parse_date(it, '%I:%M:%S %p')
                d = d.replace(year=now.year, month=now.month, day=now.day)
                t['created_at'] = d
            # FIXME: I don't like the duplication here...
            else:
                t['created_at'] = self.parse_date(
                    created_at,
                    '%B %d, %Y, %I:%M:%S %p'
                )
        else:
            t['created_at'] = self.parse_date(
                created_at,
                '%B %d, %Y, %I:%M:%S %p'
            )

        replies = response.css('.post')
        t['op'] = replies.extract_first()
        # next_page = response.css('td.middletext .prevnext .navPages')
        # //td[@class='middletext']/*[last()-1]
        yield t

    def parse_date(self, d, pattern):
        try:
            return datetime.datetime.strptime(
                d,
                pattern
            )
        except ValueError:
            self.logger.error(
                'failed to parse date value: %s - with %s',
                d, pattern
            )
            return datetime.datetime.utcnow()

    @staticmethod
    def parse_integer(i):
        try:
            return int(i)
        except ValueError:
            return 0
