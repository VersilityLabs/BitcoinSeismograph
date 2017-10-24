# -*- coding: utf-8 -*-
import datetime
import json
import re

import scrapy
from scrapy import Request

from community_scraper.items import RedditSubmission
from community_scraper.rankings import reddit_hot


class RedditSpider(scrapy.Spider):
    name = 'reddit'
    allowed_domains = ['reddit.com']

    root = 'https://www.reddit.com/r/'
    targets = [
        'Bitcoin',
        'BitcoinMarkets',
        'btc',
    ]

    def __init__(self):
        super(RedditSpider, self).__init__()
        self.pattern = re.compile(r'comments\/([\w\d]+)\/')

    def start_requests(self):
        return [Request(self.root + t + '/.json',
                        callback=self.parse_subreddit)
                for t in self.targets]

    def parse_subreddit(self, response):
        json_blob = json.loads(response.body_as_unicode())
        submissions = json_blob['data']['children']

        for s in submissions:
            data = s['data']
            if data['stickied']:
                continue

            sub = RedditSubmission()
            sub['title'] = data['title']
            sub['permalink'] = data['url']

            if data['is_self']:
                sub['op'] = data['selftext']

            sub['created_at'] = datetime.datetime.fromtimestamp(
                data['created_utc']
            )

            sub['subreddit'] = data['subreddit']
            sub['comments'] = data['num_comments']
            sub['score'] = data['score']
            sub['id'] = data['id']

            sub['rank'] = reddit_hot(sub['score'], sub['created_at'])

            yield sub
