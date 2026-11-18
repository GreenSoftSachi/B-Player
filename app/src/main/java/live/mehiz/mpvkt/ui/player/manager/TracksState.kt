package live.mehiz.mpvkt.ui.player.manager

import android.content.Context
import android.net.Uri
import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.MPVNode
import `is`.xyz.mpv.prop
import `is`.xyz.mpv.propFlow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import live.mehiz.mpvkt.ui.player.TrackNode
import live.mehiz.mpvkt.ui.player.mapState
import live.mehiz.mpvkt.ui.player.openContentFd
import live.mehiz.mpvkt.ui.player.toObject

class TracksState(private val mpv: MPV, private val context: Context, json: Json, scope: CoroutineScope) {
  private var manuallyDisabled = false

  private val trackList: StateFlow<List<TrackNode>> = mpv.propFlow<MPVNode>("track-list")
    .mapState(scope, { emptyList() }) { it.toObject<List<TrackNode>>(json) }

  val subtitles: StateFlow<ImmutableList<TrackNode>> =
    trackList.mapState(scope, { persistentListOf() }) { list -> list.filter { it.isSubtitle }.toImmutableList() }
  val audio: StateFlow<ImmutableList<TrackNode>> =
    trackList.mapState(scope, { persistentListOf() }) { list -> list.filter { it.isAudio }.toImmutableList() }

  init {
    scope.launch {
      subtitles.collect { subs ->
        if (!manuallyDisabled && subs.isNotEmpty()) {
          val sid = mpv.getPropertyInt("sid") ?: 0
          if (sid <= 0) {
            val autoTrack = subs.firstOrNull { it.default == true } ?: subs.firstOrNull { it.id > 0 } ?: subs.first()
            mpv.prop("sid", autoTrack.id)
            mpv.prop("secondary-sid", false)
          }
        }
      }
    }
  }

  fun disableSubtitles() {
    manuallyDisabled = true
    mpv.prop("sid", false)
    mpv.prop("secondary-sid", false)
  }

  fun selectSubtitle(id: Int) {
    val sid = mpv.getPropertyInt("sid") ?: 0
    if (id == sid) {
      manuallyDisabled = true
      mpv.prop("sid", false)
    } else {
      manuallyDisabled = false
      mpv.prop("sid", id)
    }
    // Always disable secondary-sid so subtitles are never duplicated at the top
    mpv.prop("secondary-sid", false)
  }

  fun toggleAudio(id: Int) {
    if (id == mpv.getPropertyInt("aid")) mpv.prop("aid", false) else mpv.prop("aid", id)
  }

  fun addSubtitle(uri: Uri) {
    manuallyDisabled = false
    val path = when (uri.scheme) {
      "content" -> uri.openContentFd(context) ?: uri.toString()
      "file" -> uri.path ?: uri.toString()
      else -> uri.toString()
    }
    mpv.command("sub-add", path, "select")
    mpv.prop("secondary-sid", false)
  }

  fun addAudio(uri: Uri) {
    val url = uri.toString()
    val path = if (url.startsWith("content://")) uri.openContentFd(context) else url
    mpv.command("audio-add", path ?: return, "cached")
  }
}
