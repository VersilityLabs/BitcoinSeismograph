# -*- coding: utf-8 -*-

BOT_NAME = 'BitcoinSeismograph'

SPIDER_MODULES = ['community_scraper.spiders']
NEWSPIDER_MODULE = 'community_scraper.spiders'

# TODO: add something like (+URL_TO_GITHUB_REPO)
USER_AGENT = 'BitcoinSeismograph/0.1.0'

# Obey robots.txt rules
# ROBOTSTXT_OBEY = True

# Enable logging
LOG_ENABLED = True
# TODO: might be useful later
# LOG_LEVEL = 'INFO'

DOWNLOAD_DELAY = 10  # 3*60

# CONCURRENT_REQUESTS_PER_DOMAIN = 16
# TELNETCONSOLE_ENABLED = False
# EXTENSIONS = {
#    'scrapy.extensions.telnet.TelnetConsole': None,
# }

ITEM_PIPELINES = {
    'community_scraper.pipelines.ProcessForumThread': 100,
    'community_scraper.pipelines.ProcessBTCCoreReleases': 100,
    'community_scraper.pipelines.ProcessRedditSubmission': 100,
    'community_scraper.pipelines.PersistencePipeline': 999,
}

ES_TIMEOUT = 60
ES_URL = 'localhost:9200'

MAX_THREADS_PER_BOARD = 10

try:
    from community_scraper.settings_local import *
except ImportError:
    pass
