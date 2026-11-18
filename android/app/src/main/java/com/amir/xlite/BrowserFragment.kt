package com.amir.xlite

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.progressindicator.LinearProgressIndicator

class BrowserFragment : Fragment() {
    lateinit var web: WebView
    private var script: String = ""

    inner class Bridge {
        @JavascriptInterface
        fun send(url: String): Boolean {
            val ctx = context ?: return false
            Sender.enqueue(ctx, url)
            activity?.runOnUiThread {
                Toast.makeText(ctx, "رفت تو صف ارسال ✓", Toast.LENGTH_SHORT).show()
            }
            return true
        }

        @JavascriptInterface
        fun isSent(url: String): Boolean {
            val ctx = context ?: return false
            return Sender.isSent(ctx, url)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_browser, container, false)
        web = v.findViewById(R.id.web)
        val progress = v.findViewById<LinearProgressIndicator>(R.id.progress)
        script = requireContext().assets.open("inject.js").bufferedReader().use { it.readText() }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportMultipleWindows(false)
            userAgentString = userAgentString.replace("; wv", "")
        }
        web.setBackgroundColor(0xFF000000.toInt())
        web.addJavascriptInterface(Bridge(), "XLite")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
                val host = req.url.host ?: return false
                return !(host.endsWith("x.com") || host.endsWith("twitter.com") ||
                        host.endsWith("twimg.com") || host.endsWith("t.co"))
            }
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                progress.visibility = View.VISIBLE
            }
            override fun onPageFinished(view: WebView, url: String?) {
                progress.visibility = View.GONE
                view.evaluateJavascript(script, null)
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, p: Int) {
                progress.setProgressCompat(p, true)
                if (p > 60) view.evaluateJavascript(script, null)
            }
        }
        if (s != null) web.restoreState(s) else web.loadUrl("https://x.com/home")
        return v
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (this::web.isInitialized) web.saveState(outState)
    }
}
