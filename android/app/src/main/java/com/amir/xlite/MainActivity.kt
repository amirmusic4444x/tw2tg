package com.amir.xlite

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var browser: BrowserFragment
    private lateinit var dash: DashboardFragment
    private var current: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fm = supportFragmentManager
        browser = fm.findFragmentByTag("x") as? BrowserFragment ?: BrowserFragment()
        dash = fm.findFragmentByTag("d") as? DashboardFragment ?: DashboardFragment()
        if (savedInstanceState == null) {
            fm.beginTransaction()
                .add(R.id.container, browser, "x")
                .add(R.id.container, dash, "d").hide(dash)
                .commitNow()
        }
        current = if (dash.isHidden) browser else dash

        findViewById<BottomNavigationView>(R.id.nav).setOnItemSelectedListener { item ->
            val target: Fragment = if (item.itemId == R.id.nav_dash) dash else browser
            if (target !== current) {
                fm.beginTransaction().hide(current!!).show(target).commit()
                current = target
            }
            true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (current === browser && browser.web.canGoBack()) browser.web.goBack()
                else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })
        handleShare(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        val m = Regex("""https?://(?:www\.|mobile\.)?(?:twitter|x)\.com/\w+/status/\d+""").find(text) ?: return
        Sender.enqueue(this, m.value)
        Toast.makeText(this, "رفت تو صف ارسال ✓", Toast.LENGTH_SHORT).show()
    }
}
