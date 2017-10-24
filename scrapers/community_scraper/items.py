# -*- coding: utf-8 -*-

import scrapy


class Item(scrapy.Item):
    id = scrapy.Field()
    title = scrapy.Field()
    title_sentiment = scrapy.Field()
    title_lemmas = scrapy.Field()

    permalink = scrapy.Field()

    created_at = scrapy.Field()

    # op = original post, the thread-starter post
    op = scrapy.Field()
    op_sentiment = scrapy.Field()


class ForumThread(Item):
    board = scrapy.Field()
    views = scrapy.Field()
    replies = scrapy.Field()


class RedditSubmission(Item):
    subreddit = scrapy.Field()

    rank = scrapy.Field()
    score = scrapy.Field()
    comments = scrapy.Field()


class BTCCoreRelease(scrapy.Item):
    version = scrapy.Field()
    rel_date = scrapy.Field()
    notable_changes = scrapy.Field()
    dl_link = scrapy.Field()
