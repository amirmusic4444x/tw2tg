package com.amir.xlite

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.UUID
import java.util.concurrent.Executors

object Sender {
    private val pool = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun enqueue(ctx: Context, url: String) {
        val app = ctx.applicationContext
        val id = UUID.randomUUID().toString().substring(0, 8)
        Store.update(app) { it.add(0, Job(id, url, System.currentTimeMillis(), "local")) }
        main.post { Store.notifyChanged() }
        dispatch(app, id, url)
    }

    fun retry(ctx: Context, job: Job) {
        val app = ctx.applicationContext
        val id = UUID.randomUUID().toString().substring(0, 8)
        Store.update(app) { list ->
            list.removeAll { it.id == job.id }
            list.add(0, Job(id, job.url, System.currentTimeMillis(), "local"))
        }
        main.post { Store.notifyChanged() }
        dispatch(app, id, job.url)
    }

    private fun dispatch(app: Context, id: String, url: String) {
        pool.execute {
            val ok = GitHub.dispatch(url, id)
            Store.update(app) { list -> list.find { it.id == id }?.state = if (ok) "queued" else "dispatch_failed" }
            main.post { Store.notifyChanged() }
        }
    }

    fun refresh(ctx: Context, done: (() -> Unit)? = null) {
        val app = ctx.applicationContext
        pool.execute {
            val runs = GitHub.runs()
            if (runs != null) {
                Store.update(app) { list ->
                    list.forEach { j ->
                        runs[j.id]?.let { (st, link) -> j.state = st; j.runUrl = link }
                    }
                }
            }
            main.post { Store.notifyChanged(); done?.invoke() }
        }
    }

    fun isSent(ctx: Context, url: String): Boolean =
        Store.all(ctx.applicationContext).any { it.url == url && it.state == "sent" }
}
