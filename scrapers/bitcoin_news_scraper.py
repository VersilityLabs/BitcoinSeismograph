#!/usr/bin/env python
import datetime
import hashlib
import time

import feedparser
import logging
import requests
import sys
from elasticsearch import Elasticsearch

from community_scraper import settings

news_bitcoin_com_endpoint = 'https://widgets.bitcoin.com/news.json'
bitcoincharts_endpoint = 'https://bitcoincharts.com/headlines.rss'
# TODO: add support
bitcointalk_endpoint = 'https://bitcointalk.org/index.php?type=rss;action=.xml;board=77.0;limit=25'
coindesk_endpoint = 'http://www.coindesk.com/category/markets-news/markets-markets-news/markets-bitcoin/feed/'
headers = {
    'User-Agent': settings.USER_AGENT,
}
logging.basicConfig(stream=sys.stdout, level=logging.DEBUG)
logger = logging.getLogger('news_scraper')


def news_bitcoin_com(es_client):
    index = 'news'
    doc_type = 'item'
    source = 'news.bitcoin.com'

    logger.debug('retrieving data from {}'.format(source))
    r = requests.get(news_bitcoin_com_endpoint, headers=headers)
    blob = r.json()
    for item in blob:
        title = item['title']
        timestamp = item['stamp']
        permalink = item['url']

        ts = datetime.datetime.strptime(
            timestamp,
            '%a, %d %b %Y %H:%M:%S %z'
        )

        hash = hashlib.sha1()
        hash.update(permalink.encode('utf-8'))
        item_id = hash.hexdigest()

        es_client.index(
            index=index,
            doc_type=doc_type,
            id=item_id,
            body={
                'title': title,
                'timestamp': ts,
                'url': permalink,
                'source': source,
            }
        )


def news_coindesk(es_client):
    index = 'news'
    doc_type = 'item'
    source = 'coindesk.com'

    logger.debug('retrieving data from {}'.format(source))
    feed = feedparser.parse(bitcoincharts_endpoint)
    for entry in feed.entries:
        timestamp = datetime.datetime.fromtimestamp(
            time.mktime(entry.published_parsed)
        )
        title = entry.title
        link = entry.link
        guid = entry.id
        hash = hashlib.sha1()
        hash.update(guid.encode('utf-8'))
        entry_id = hash.hexdigest()

        es_client.index(
            index=index,
            doc_type=doc_type,
            id=entry_id,
            body={
                'title': title,
                'timestamp': timestamp,
                'url': link,
                'source': source,
            }
        )


if __name__ == "__main__":
    es = Elasticsearch(
        hosts=[settings.ES_URL],
        timeout=settings.ES_TIMEOUT
    )
    news_bitcoin_com(es)
    news_coindesk(es)
