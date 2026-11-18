import os, sys, glob, subprocess, requests

TOKEN = os.environ["BOT_TOKEN"]
CHANNEL = os.environ["CHANNEL_ID"]
URL = os.environ["TWEET_URL"].strip()
API = f"https://api.telegram.org/bot{TOKEN}"
MAX_BYTES = 49 * 1024 * 1024

fmt = "best[ext=mp4][filesize<49M]/bv*[filesize<40M]+ba/best[filesize<49M]/best"
r = subprocess.run(["yt-dlp", "-q", "--no-warnings", "-f", fmt, "--merge-output-format", "mp4",
                    "-o", "/tmp/v.%(ext)s", URL])
files = glob.glob("/tmp/v.*")
if r.returncode != 0 or not files:
    print("NO_VIDEO")
    sys.exit(2)
path = files[0]
if os.path.getsize(path) > MAX_BYTES:
    print("TOO_BIG")
    sys.exit(3)
with open(path, "rb") as f:
    res = requests.post(f"{API}/sendVideo", timeout=300,
                        data={"chat_id": CHANNEL, "supports_streaming": True},
                        files={"video": f}).json()
if not res.get("ok"):
    print("TG_FAIL", res.get("description"))
    sys.exit(4)
print("SENT")
