package com.example.neurozen_front.presentation.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.neurozen_front.BuildConfig
import com.example.neurozen_front.domain.logic.YouTube

/**
 * Plays a YouTube video (also the library "audios") inside the app with the official embedded player
 * (youtube-nocookie). No extra library: a WebView that only loads this page and the YouTube player.
 * Any other navigation is blocked. If the owner does not allow embedding, YouTube shows its own error and the
 * screen offers «Abrir en YouTube».
 */
@SuppressLint("SetJavaScriptEnabled") // The YouTube player needs JavaScript; the WebView only loads YouTube.
@Composable
fun YouTubePlayer(videoId: String, title: String, modifier: Modifier = Modifier) {
    val html = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1">
        <style>html,body{margin:0;padding:0;background:#000;height:100%}iframe{border:0;width:100%;height:100%}</style>
        </head><body><iframe src="${YouTube.embedUrl(videoId)}" title="YouTube"
        allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe></body></html>
    """.trimIndent()
    AndroidView(
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp))
            .testTag("youtube_player").semantics { contentDescription = "Reproductor de YouTube: $title" },
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    // Only the YouTube player may load; links inside it (e.g. the YouTube logo) are not followed.
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val host = request.url.host.orEmpty()
                        return !(host.endsWith("youtube-nocookie.com") || host.endsWith("youtube.com") || host.endsWith("ytimg.com") || host.endsWith("google.com"))
                    }
                }
                // An https base URL gives the embed a referrer (YouTube refuses to play embeds without one).
                loadDataWithBaseURL("https://${BuildConfig.APPLICATION_ID}", html, "text/html", "utf-8", null)
            }
        },
        onRelease = { it.destroy() }
    )
}
