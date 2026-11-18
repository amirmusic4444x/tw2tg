"""Fetch every photo/video of a tweet and post it to the Telegram channel with a caption."""
import os, re, json, glob, subprocess, urllib.request, uuid

TOKEN = os.environ["BOT_TOKEN"]
CHANNEL = os.environ["CHANNEL_ID"]
API = f"https://api.telegram.org/bot{TOKEN}"
MAX = 49 * 1024 * 1024
UA = {"User-Agent": "Mozilla/5.0 (tw2tg)"}
RE = re.compile(r"https?://(?:www\.|mobile\.)?(?:twitter|x)\.com/(\w+)/status/(\d+)")


class SendError(Exception):
    pass


def _get(url, timeout=60):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=timeout) as r:
        return r.read()


def _size(url):
    try:
        req = urllib.request.Request(url, headers=UA, method="HEAD")
        with urllib.request.urlopen(req, timeout=30) as r:
            return int(r.headers.get("Content-Length") or 0)
    except Exception:
        return 0


def _tg(method, fields, files=None):
    b = uuid.uuid4().hex
    body = b""
    for k, v in fields.items():
        body += f'--{b}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode()
    for k, (name, data) in (files or {}).items():
        body += (f'--{b}\r\nContent-Disposition: form-data; name="{k}"; filename="{name}"\r\n'
                 f'Content-Type: application/octet-stream\r\n\r\n').encode() + data + b"\r\n"
    body += f"--{b}--\r\n".encode()
    req = urllib.request.Request(f"{API}/{method}", data=body,
                                 headers={"Content-Type": f"multipart/form-data; boundary={b}"})
    try:
        with urllib.request.urlopen(req, timeout=300) as r:
            res = json.loads(r.read())
    except urllib.error.HTTPError as e:
        res = json.loads(e.read() or b"{}")
    if not res.get("ok"):
        raise SendError("TG_FAIL " + str(res.get("description")))
    return res


def _media_fx(handle, tid):
    d = json.loads(_get(f"https://api.fxtwitter.com/{handle}/status/{tid}"))
    t = d.get("tweet") or {}
    author = (t.get("author") or {}).get("screen_name") or handle
    items = []
    for m in (t.get("media") or {}).get("all") or []:
        if m.get("type") == "photo":
            u = m["url"]
            items.append(("photo", [u + ("&" if "?" in u else "?") + "name=orig", u]))
        else:  # video / gif
            fm = [f for f in (m.get("formats") or m.get("variants") or [])
                  if (f.get("container") == "mp4" or f.get("content_type") == "video/mp4")]
            fm.sort(key=lambda f: f.get("bitrate") or 0, reverse=True)
            urls = [f["url"] for f in fm] or [m["url"]]
            items.append(("video", urls))
    return author, items


def _download(kind, urls):
    for u in urls:
        if kind == "video":
            s = _size(u)
            if s and s > MAX:
                continue
        try:
            data = _get(u, timeout=180)
        except Exception:
            continue
        if len(data) <= MAX:
            return data
    return None


def _ytdlp(url):
    for f in glob.glob("/tmp/yv.*"):
        os.remove(f)
    fmt = "best[ext=mp4][filesize<49M]/bv*[filesize<40M]+ba/best[filesize<49M]/best"
    subprocess.run(["yt-dlp", "-q", "--no-warnings", "-f", fmt, "--merge-output-format", "mp4",
                    "-o", "/tmp/yv.%(ext)s", url])
    files = glob.glob("/tmp/yv.*")
    if files and os.path.getsize(files[0]) <= MAX:
        return open(files[0], "rb").read()
    return None


def process(url):
    """Returns a short status string. Raises SendError on failure."""
    m = RE.search(url)
    if not m:
        raise SendError("BAD_LINK")
    handle, tid = m.group(1), m.group(2)
    try:
        author, items = _media_fx(handle, tid)
    except Exception:
        author, items = handle, []
    link = f"https://x.com/{author}/status/{tid}"
    caption = f"👤 @{author}\n🔗 {link}"

    files = []
    for kind, urls in items[:10]:
        data = _download(kind, urls)
        if data:
            files.append((kind, data))
    if not files:
        data = _ytdlp(link)
        if data:
            files.append(("video", data))
    if not files:
        raise SendError("NO_MEDIA")

    if len(files) == 1:
        kind, data = files[0]
        method, field = ("sendPhoto", "photo") if kind == "photo" else ("sendVideo", "video")
        fields = {"chat_id": CHANNEL, "caption": caption}
        if kind == "video":
            fields["supports_streaming"] = "true"
        _tg(method, fields, {field: ("f.jpg" if kind == "photo" else "f.mp4", data)})
    else:
        media, up = [], {}
        for i, (kind, data) in enumerate(files):
            item = {"type": kind, "media": f"attach://f{i}"}
            if i == 0:
                item["caption"] = caption
            if kind == "video":
                item["supports_streaming"] = True
            media.append(item)
            up[f"f{i}"] = (f"f{i}." + ("jpg" if kind == "photo" else "mp4"), data)
        _tg("sendMediaGroup", {"chat_id": CHANNEL, "media": json.dumps(media)}, up)
    return f"SENT {len(files)}"
