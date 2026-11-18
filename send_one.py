import os, sys
from xmedia import process, SendError

try:
    print(process(os.environ["TWEET_URL"].strip()))
except SendError as e:
    print(e)
    sys.exit(2)
