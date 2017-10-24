# -*- coding: utf-8 -*-
import datetime
import re
import unicodedata

import lxml.html
from elasticsearch import Elasticsearch
from elasticsearch.helpers import bulk
from nltk.corpus import stopwords
from textblob import TextBlob

from community_scraper.items import ForumThread, \
    RedditSubmission, BTCCoreRelease

# taken from https://gist.github.com/gruber/8891611
# URL_MATCHER = re.compile(
#     '(?i)\b((?:https?:(?:/{1,3}|[a-z0-9%])|[a-z0-9.\-]+[.](?:com|net|org|edu|gov|mil|aero|asia|biz|cat|coop|info|int|jobs|mobi|museum|name|post|pro|tel|travel|xxx|ac|ad|ae|af|ag|ai|al|am|an|ao|aq|ar|as|at|au|aw|ax|az|ba|bb|bd|be|bf|bg|bh|bi|bj|bm|bn|bo|br|bs|bt|bv|bw|by|bz|ca|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|co|cr|cs|cu|cv|cx|cy|cz|dd|de|dj|dk|dm|do|dz|ec|ee|eg|eh|er|es|et|eu|fi|fj|fk|fm|fo|fr|ga|gb|gd|ge|gf|gg|gh|gi|gl|gm|gn|gp|gq|gr|gs|gt|gu|gw|gy|hk|hm|hn|hr|ht|hu|id|ie|il|im|in|io|iq|ir|is|it|je|jm|jo|jp|ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|ma|mc|md|me|mg|mh|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|my|mz|na|nc|ne|nf|ng|ni|nl|no|np|nr|nu|nz|om|pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|ps|pt|pw|py|qa|re|ro|rs|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|Ja|sk|sl|sm|sn|so|sr|ss|st|su|sv|sx|sy|sz|tc|td|tf|tg|th|tj|tk|tl|tm|tn|to|tp|tr|tt|tv|tw|tz|ua|ug|uk|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt|yu|za|zm|zw)/)(?:[^\s()<>{}\[\]]+|\([^\s()]*?\([^\s()]+\)[^\s()]*?\)|\([^\s]+?\))+(?:\([^\s()]*?\([^\s()]+\)[^\s()]*?\)|\([^\s]+?\)|[^\s`!()\[\]{};:\'\".,<>?«»“”‘’])|(?:(?<!@)[a-z0-9]+(?:[.\-][a-z0-9]+)*[.](?:com|net|org|edu|gov|mil|aero|asia|biz|cat|coop|info|int|jobs|mobi|museum|name|post|pro|tel|travel|xxx|ac|ad|ae|af|ag|ai|al|am|an|ao|aq|ar|as|at|au|aw|ax|az|ba|bb|bd|be|bf|bg|bh|bi|bj|bm|bn|bo|br|bs|bt|bv|bw|by|bz|ca|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|co|cr|cs|cu|cv|cx|cy|cz|dd|de|dj|dk|dm|do|dz|ec|ee|eg|eh|er|es|et|eu|fi|fj|fk|fm|fo|fr|ga|gb|gd|ge|gf|gg|gh|gi|gl|gm|gn|gp|gq|gr|gs|gt|gu|gw|gy|hk|hm|hn|hr|ht|hu|id|ie|il|im|in|io|iq|ir|is|it|je|jm|jo|jp|ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|ma|mc|md|me|mg|mh|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|my|mz|na|nc|ne|nf|ng|ni|nl|no|np|nr|nu|nz|om|pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|ps|pt|pw|py|qa|re|ro|rs|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|Ja|sk|sl|sm|sn|so|sr|ss|st|su|sv|sx|sy|sz|tc|td|tf|tg|th|tj|tk|tl|tm|tn|to|tp|tr|tt|tv|tw|tz|ua|ug|uk|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt|yu|za|zm|zw)\b/?(?!@)))',
#     re.VERBOSE
# )


class ProcessBTCCoreReleases(object):
    def process_item(self, item, spider):
        # Currently no special processing necessary
        return item


class NLPPipeline(object):
    def __init__(self):
        stop = set(stopwords.words('english'))
        stop.update(
            ['.', ',', '"', "'", '?', '!', ':',
             ';', '(', ')', '[', ']', '{', '}']
        )

        self.stop_words = stop

    def analyze_sentiment(self, string):
        s = TextBlob(string)
        return s.sentiment

    def extract_keywords(self, string):
        title = TextBlob(string)

        # remove stopwords & lemmatize the words
        content = set(title.words) - self.stop_words
        # TODO: is this enough?
        return TextBlob(' '.join(content)).words.lemmatize()


class ProcessForumThread(NLPPipeline):
    def _sanitize(self, string):
        body = unicodedata.normalize('NFKC', string)
        doc = lxml.html.fromstring(body)

        # this should be "good enough" for now
        for child in list(doc):
            if child.tag == 'br':
                child.tail = '\n' + child.tail if child.tail else '\n'
            else:
                child.drop_tree()
        return doc.text_content()

    def process_item(self, item, spider):
        if isinstance(item, ForumThread):
            if 'op' in item:
                item['op'] = self._sanitize(item['op'])
                item['op_sentiment'] = self.analyze_sentiment(item['op'])
            title = item['title']
            item['title_sentiment'] = self.analyze_sentiment(title)
            item['title_lemmas'] = self.extract_keywords(title)
        return item


class ProcessRedditSubmission(NLPPipeline):
    def process_item(self, item, spider):
        if isinstance(item, RedditSubmission):
            # FIXME: make this DRY
            if 'op' in item:
                item['op_sentiment'] = self.analyze_sentiment(item['op'])
            title = item['title']
            item['title_sentiment'] = self.analyze_sentiment(title)
            item['title_lemmas'] = self.extract_keywords(title)
        return item


class PersistencePipeline(object):
    buffer = []

    @classmethod
    def from_crawler(cls, crawler):
        ext = cls()
        ext.settings = crawler.settings

        ext.es = Elasticsearch(
            hosts=[ext.settings.get('ES_URL', 'localhost:9200')],
            timeout=ext.settings.get('ES_TIMEOUT', 60)
        )
        return ext

    def process_item(self, item, spider):
        # TODO: extract the type specific handling into methods
        if isinstance(item, ForumThread):
            thread_id = item['id']
            index = 'threads'
            doc_type = 'thread'
            already_scraped = self.es.exists(index=index,
                                             doc_type=doc_type,
                                             id=thread_id)
            scrape = {
                'scraped_at': datetime.datetime.now(),
                'views': item['views'],
                'replies': item['replies'],
            }
            if already_scraped:
                action = {
                    '_op_type': 'update',
                    '_index': index,
                    '_type': doc_type,
                    '_id': thread_id,
                    'script': {
                        'inline': 'ctx._source.scrapes.add(params.scrape)',
                        'lang': 'painless',
                        'params': {'scrape': scrape}
                    }
                }
            else:
                sentiment = {
                    'title': {
                        'polarity': item['title_sentiment'].polarity,
                        'subjectivity': item['title_sentiment'].subjectivity,
                    },
                    'op': {
                        'polarity': item['op_sentiment'].polarity,
                        'subjectivity': item['op_sentiment'].subjectivity
                    }
                }
                action = {
                    '_op_type': 'index',
                    '_index': index,
                    '_type': doc_type,
                    '_id': thread_id,
                    '_source': {
                        'title': item['title'],
                        'keywords': [k for k in item.get('title_lemmas', [])],
                        'op': item.get('op'),
                        'created_at': item['created_at'],
                        'sentiment': sentiment,
                        'community': spider.name,
                        'board': item['board'],
                        'permalink': item['permalink'],
                        'scrapes': [
                            scrape
                        ]
                    }
                }

        elif isinstance(item, RedditSubmission):
            submission_id = item['id']
            index = 'submissions'
            doc_type = 'submission'
            already_scraped = self.es.exists(index=index,
                                             doc_type=doc_type,
                                             id=submission_id)
            scrape = {
                'scraped_at': datetime.datetime.now(),
                'rank': item['rank'],
                'score': item['score'],
                'comments': item['comments'],
            }
            if already_scraped:
                action = {
                    '_op_type': 'update',
                    '_index': index,
                    '_type': doc_type,
                    '_id': submission_id,
                    'script': {
                        'inline': 'ctx._source.scrapes.add(params.scrape)',
                        'lang': 'painless',
                        'params': {'scrape': scrape}
                    }
                }
            else:
                sentiment = {
                    'title': {
                        'polarity': item['title_sentiment'].polarity,
                        'subjectivity': item['title_sentiment'].subjectivity,
                    },
                }
                if 'op' in item:
                    sentiment.update({'op': {
                        'polarity': item['op_sentiment'].polarity,
                        'subjectivity': item['op_sentiment'].subjectivity
                    }})

                action = {
                    '_op_type': 'index',
                    '_index': index,
                    '_type': doc_type,
                    '_id': submission_id,
                    '_source': {
                        'title': item['title'],
                        'keywords': [k for k in item.get('title_lemmas', [])],
                        'op': item.get('op'),
                        'created_at': item['created_at'],
                        'sentiment': sentiment,
                        'community': spider.name,
                        'subreddit': item['subreddit'],
                        'permalink': item['permalink'],
                        'scrapes': [
                            scrape
                        ]
                    }
                }

        elif isinstance(item, BTCCoreRelease):
            rel_id = item['version'].replace('.', '-')
            index = 'releases'
            doc_type = 'release'
            already_scraped = self.es.exists(index=index,
                                             doc_type=doc_type,
                                             id=rel_id)
            if not already_scraped:
                action = {
                    '_op_type': 'create',
                    '_index': index,
                    '_type': doc_type,
                    '_id': rel_id,
                    '_source': {
                        'version': item['version'],
                        'release_date': item['rel_date'],
                        'notable_changes': item['notable_changes'],
                        'url': item['dl_link'],
                    }
                }
            else:
                return item
        else:
            spider.logger.warn('unknown item encountered')
            return item
        self.buffer.append(action)
        if len(self.buffer) >= self.settings.get('ES_ACTION_BUFFER', 100):
            self.commit()
            self.buffer.clear()

        return item

    def commit(self):
        bulk(self.es, self.buffer)

    def close_spider(self, spider):
        if len(self.buffer):
            self.commit()
