package com.amir.xlite

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GitHub {
    private const val API = "https://api.github.com/repos/" + BuildConfig.GH_OWNER + "/" + BuildConfig.GH_REPO

    private fun open(path: String, method: String): HttpURLConnection {
        val c = URL(API + path).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 15000
        c.readTimeout = 20000
        c.setRequestProperty("Authorization", "Bearer " + BuildConfig.GH_TOKEN)
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        return c
    }

    fun dispatch(url: String, reqId: String): Boolean {
        return try {
            val c = open("/actions/workflows/send.yml/dispatches", "POST")
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            val body = JSONObject().put("ref", "main")
                .put("inputs", JSONObject().put("url", url).put("req_id", reqId))
            c.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = c.responseCode
            c.disconnect()
            code in 200..299
        } catch (e: Exception) { false }
    }

    /** Map of req_id -> Pair(state, html_url) */
    fun runs(): Map<String, Pair<String, String>>? {
        return try {
            val c = open("/actions/workflows/send.yml/runs?per_page=100", "GET")
            val text = c.inputStream.bufferedReader().use { it.readText() }
            c.disconnect()
            val arr = JSONObject(text).getJSONArray("workflow_runs")
            val out = HashMap<String, Pair<String, String>>()
            for (i in 0 until arr.length()) {
                val r = arr.getJSONObject(i)
                val title = r.optString("display_title", "")
                if (!title.startsWith("send ")) continue
                val id = title.removePrefix("send ").trim()
                val status = r.optString("status")
                val concl = r.optString("conclusion")
                val state = when {
                    status == "completed" && concl == "success" -> "sent"
                    status == "completed" -> "failed"
                    status == "in_progress" -> "in_progress"
                    else -> "queued"
                }
                if (!out.containsKey(id)) out[id] = Pair(state, r.optString("html_url"))
            }
            out
        } catch (e: Exception) { null }
    }
}
