import datetime
from math import log

# 45000 is the default decay value for the reddit hotness algorithm
REDDIT_HOT_DECAY = 45000


def reddit_hot(score, created_at):
    """Calculates the 'hot' score of a reddit post."""
    order = log(max(abs(score), 1), 10)
    sign = 1 if score > 0 else -1 if score < 0 else 0
    seconds = (datetime.datetime.utcnow() - created_at).seconds
    return round(sign * order + seconds / REDDIT_HOT_DECAY, 7)
