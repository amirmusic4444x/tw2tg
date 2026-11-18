import os, re, glob, subprocess, requests

TOKEN = os.environ["BOT_TOKEN"]
CHANNEL = os.environ["CHANNEL_ID"]
OWNER = str(os.environ.get("OWNER_ID", "")).strip()
API = f"https://api.telegram.org/bot{TOKEN}"
LINK_RE = re.compile(r"https?://(?:www\.|mobile\.)?(?:twitter|x)\.com/\w+/status/\d+")
MAX_BYTES = 49 * 1024 * 1024


def tg(method, **kw):
    return requests.post(f"{API}/{method}", timeout=120, **kw).json()


def notify(chat_id, text):
    tg("sendMessage", data={"chat_id": chat_id, "text": text})


def download(url, idx):
    out = f"/tmp/v{idx}.%(ext)s"
    fmt = "best[ext=mp4][filesize<49M]/bv*[filesize<40M]+ba/best[filesize<49M]/best"
    subprocess.run(["yt-dlp", "-q", "--no-warnings", "-f", fmt,
                    "--merge-output-format", "mp4", "-o", out, url], check=True)
    files = glob.glob(f"/tmp/v{idx}.*")
    return files[0] if files else None


def main():
    updates = tg("getUpdates", data={"timeout": 0}).get("result", [])
    if not updates:
        return
    jobs = []
    for u in updates:
        msg = u.get("message") or {}
        if msg.get("chat", {}).get("type") != "private":
            continue
        sender = str(msg.get("from", {}).get("id"))
        if OWNER and sender != OWNER:
            continue
        for link in LINK_RE.findall(msg.get("text", "") or ""):
            jobs.append((link, msg["chat"]["id"]))
    seen = set()
    for i, (url, chat) in enumerate(jobs):
        if url in seen:
            continue
        seen.add(url)
        try:
            path = download(url, i)
            if not path:
                notify(chat, "No video found in this tweet.")
                continue
            if os.path.getsize(path) > MAX_BYTES:
                notify(chat, "Video is over 50 MB, the bot can't send it.")
                continue
            with open(path, "rb") as f:
                r = tg("sendVideo", data={"chat_id": CHANNEL, "supports_streaming": True},
                       files={"video": f})
            notify(chat, "Sent to channel" if r.get("ok") else "Failed to post to channel")
        except Exception:
            notify(chat, "Download failed for this video.")
    tg("getUpdates", data={"offset": updates[-1]["update_id"] + 1, "timeout": 0})


if __name__ == "__main__":
    main()
