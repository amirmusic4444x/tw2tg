package com.amir.xlite

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Job(
    val id: String,
    val url: String,
    val time: Long,
    var state: String,   // local, dispatch_failed, queued, in_progress, sent, failed
    var runUrl: String = ""
)

object Store {
    private const val KEY = "jobs"
    private val listeners = mutableListOf<() -> Unit>()

    fun addListener(l: () -> Unit) { listeners.add(l) }
    fun removeListener(l: () -> Unit) { listeners.remove(l) }
    fun notifyChanged() { listeners.toList().forEach { it() } }

    @Synchronized
    fun all(ctx: Context): MutableList<Job> {
        val raw = ctx.getSharedPreferences("xlite", Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<Job>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(Job(o.getString("id"), o.getString("url"), o.getLong("time"),
                o.getString("state"), o.optString("runUrl", "")))
        }
        return out
    }

    @Synchronized
    fun save(ctx: Context, jobs: List<Job>) {
        val arr = JSONArray()
        jobs.take(300).forEach {
            arr.put(JSONObject().put("id", it.id).put("url", it.url).put("time", it.time)
                .put("state", it.state).put("runUrl", it.runUrl))
        }
        ctx.getSharedPreferences("xlite", Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }

    @Synchronized
    fun update(ctx: Context, block: (MutableList<Job>) -> Unit) {
        val jobs = all(ctx)
        block(jobs)
        save(ctx, jobs)
    }
}
