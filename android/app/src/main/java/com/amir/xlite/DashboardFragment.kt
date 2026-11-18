package com.amir.xlite

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class DashboardFragment : Fragment() {
    private val adapter = JobAdapter()
    private val handler = Handler(Looper.getMainLooper())
    private var root: View? = null
    private val listener: () -> Unit = { render() }
    private val ticker = object : Runnable {
        override fun run() {
            context?.let { Sender.refresh(it) }
            handler.postDelayed(this, 6000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_dashboard, container, false)
        root = v
        v.findViewById<TextView>(R.id.channel).text = "کانال: @aassddhub  •  ارسال از طریق ابر، بدون مصرف حجم گوشی"
        val list = v.findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        val swipe = v.findViewById<SwipeRefreshLayout>(R.id.swipe)
        swipe.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.accent))
        swipe.setOnRefreshListener { Sender.refresh(requireContext()) { swipe.isRefreshing = false } }
        render()
        return v
    }

    override fun onResume() {
        super.onResume()
        Store.addListener(listener)
        if (!isHidden) handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        Store.removeListener(listener)
        handler.removeCallbacks(ticker)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) handler.removeCallbacks(ticker) else { render(); handler.post(ticker) }
    }

    private fun render() {
        val v = root ?: return
        val ctx = context ?: return
        val jobs = Store.all(ctx)
        val queued = jobs.count { it.state in listOf("local", "queued", "in_progress") }
        val sent = jobs.count { it.state == "sent" }
        val failed = jobs.count { it.state in listOf("failed", "dispatch_failed") }
        v.findViewById<TextView>(R.id.cQueue).text = "در صف\n$queued"
        v.findViewById<TextView>(R.id.cSent).text = "ارسال‌شده\n$sent"
        v.findViewById<TextView>(R.id.cFail).text = "خطا\n$failed"
        v.findViewById<View>(R.id.empty).visibility = if (jobs.isEmpty()) View.VISIBLE else View.GONE
        adapter.submit(jobs)
    }

    inner class JobAdapter : RecyclerView.Adapter<JobAdapter.VH>() {
        private var items: List<Job> = emptyList()
        fun submit(list: List<Job>) { items = list; notifyDataSetChanged() }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val dot: View = v.findViewById(R.id.dot)
            val title: TextView = v.findViewById(R.id.title)
            val sub: TextView = v.findViewById(R.id.sub)
            val status: TextView = v.findViewById(R.id.status)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_job, p, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, i: Int) {
            val j = items[i]
            val ctx = h.itemView.context
            val m = Regex("""/([^/]+)/status/(\d+)""").find(j.url)
            h.title.text = if (m != null) "@${m.groupValues[1]}" else j.url
            h.sub.text = DateUtils.getRelativeTimeSpanString(j.time)
            val (label, colorRes) = when (j.state) {
                "sent" -> "ارسال شد" to R.color.ok
                "in_progress" -> "در حال ارسال" to R.color.accent2
                "queued", "local" -> "در صف" to R.color.warn
                "dispatch_failed" -> "بدون اینترنت • تلاش دوباره" to R.color.err
                else -> "خطا • تلاش دوباره" to R.color.err
            }
            val color = ContextCompat.getColor(ctx, colorRes)
            h.status.text = label
            h.status.setTextColor(color)
            h.dot.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
            h.itemView.setOnClickListener {
                if (j.state == "failed" || j.state == "dispatch_failed") Sender.retry(ctx, j)
                else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(j.url)))
            }
        }
    }
}
