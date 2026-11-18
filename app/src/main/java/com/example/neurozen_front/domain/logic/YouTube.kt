package com.example.neurozen_front.domain.logic

/** YouTube links of the library (videos and audios): the backend sends https://www.youtube.com/watch?v=ID. */
object YouTube {
    // watch?v= (www., m. or no subdomain, with extra parameters), youtu.be/, /embed/ and /shorts/. Ids have 11 chars.
    private val ID = Regex("""(?:youtube(?:-nocookie)?\.com/(?:watch\?(?:.*&)?v=|embed/|shorts/)|youtu\.be/)([A-Za-z0-9_-]{11})(?![A-Za-z0-9_-])""")

    fun videoId(url: String?): String? = url?.let { ID.find(it)?.groupValues?.get(1) }

    /** Built only from the validated id, never from arbitrary text of the API. */
    fun watchUrl(id: String): String = "https://www.youtube.com/watch?v=$id"

    /** Privacy-enhanced mode; playsinline keeps the video inside the app. */
    fun embedUrl(id: String): String = "https://www.youtube-nocookie.com/embed/$id?rel=0&playsinline=1"

    fun thumbnailUrl(id: String): String = "https://img.youtube.com/vi/$id/hqdefault.jpg"
}
