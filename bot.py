import os, re, json, urllib.request, urllib.parse
from xmedia import process, SendError, RE

TOKEN = os.environ["BOT_TOKEN"]
OWNER = str(os.environ.get("OWNER_ID", "")).strip()
API = f"https://api.telegram.org/bot{TOKEN}"
MSG = {"NO_MEDIA": "No photo or video found in this post.",
       "BAD_LINK": "That doesn't look like an X post link."}


def tg(method, **params):
    data = urllib.parse.urlencode(params).encode()
    with urllib.request.urlopen(f"{API}/{method}", data=data, timeout=60) as r:
        return json.loads(r.read())


def main():
    updates = tg("getUpdates", timeout=0).get("result", [])
    if not updates:
        return
    seen = set()
    for u in updates:
        msg = u.get("message") or {}
        if msg.get("chat", {}).get("type") != "private":
            continue
        if OWNER and str(msg.get("from", {}).get("id")) != OWNER:
            continue
        for m in RE.finditer(msg.get("text", "") or ""):
            url = m.group(0)
            if url in seen:
                continue
            seen.add(url)
            try:
                process(url)
                reply = "Sent to channel ✅"
            except SendError as e:
                reply = MSG.get(str(e), "Failed ❌ " + str(e))
            except Exception as e:
                reply = "Failed ❌"
            tg("sendMessage", chat_id=msg["chat"]["id"], text=reply)
    tg("getUpdates", offset=updates[-1]["update_id"] + 1, timeout=0)


if __name__ == "__main__":
    main()
