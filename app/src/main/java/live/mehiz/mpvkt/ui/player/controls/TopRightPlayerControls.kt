package live.mehiz.mpvkt.ui.player.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import live.mehiz.mpvkt.ui.player.Decoder
import live.mehiz.mpvkt.ui.player.TrackNode
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton
import java.util.Locale

@Composable
fun TopRightPlayerControls(
  // decoder
  decoder: Decoder,
  onSelectDecoder: (Decoder) -> Unit,
  // chapters
  isChaptersVisible: Boolean,
  onChaptersClick: () -> Unit,
  // subtitles
  subtitles: ImmutableList<TrackNode> = persistentListOf(),
  onSelectSubtitle: (Int) -> Unit = {},
  onDisableSubtitles: () -> Unit = {},
  onAddSubtitle: () -> Unit = {},
  onSubtitlesLongClick: () -> Unit = {},
  // audio
  onAudioClick: () -> Unit,
  onAudioLongClick: () -> Unit,
  // more dropdown actions
  onOpenVideoFilters: () -> Unit,
  onOpenPlaybackSpeed: () -> Unit,
  onOpenAudioDelay: () -> Unit,
  onOpenSubtitleSettings: () -> Unit,
  onOpenMoreSheet: () -> Unit,

  modifier: Modifier = Modifier,
) {
  Row(
    modifier,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    var isDecoderMenuOpen by remember { mutableStateOf(false) }
    Box {
      ControlsButton(
        decoder.title,
        onClick = { isDecoderMenuOpen = true },
      )
      DropdownMenu(
        expanded = isDecoderMenuOpen,
        onDismissRequest = { isDecoderMenuOpen = false },
        modifier = Modifier.background(Color(0xEE1E2430), RoundedCornerShape(12.dp)),
      ) {
        listOf(Decoder.Auto, Decoder.HWPlus, Decoder.HW, Decoder.SW).forEach { dec ->
          val isSelected = decoder == dec || (dec == Decoder.Auto && (decoder == Decoder.AutoCopy || decoder == Decoder.Auto))
          DropdownMenuItem(
            text = {
              Text(
                when (dec) {
                  Decoder.Auto -> "Auto"
                  Decoder.HWPlus -> "HW+ (Hardware Plus)"
                  Decoder.HW -> "HW (Hardware)"
                  Decoder.SW -> "SW (Software)"
                  else -> dec.title
                },
                color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              )
            },
            onClick = {
              onSelectDecoder(dec)
              isDecoderMenuOpen = false
            },
          )
        }
      }
    }

    if (isChaptersVisible) {
      ControlsButton(
        Icons.Default.Bookmarks,
        onClick = onChaptersClick,
      )
    }

    var isSubtitlesMenuOpen by remember { mutableStateOf(false) }
    Box {
      ControlsButton(
        Icons.Default.Subtitles,
        onClick = { isSubtitlesMenuOpen = true },
        onLongClick = onSubtitlesLongClick,
      )
      DropdownMenu(
        expanded = isSubtitlesMenuOpen,
        onDismissRequest = { isSubtitlesMenuOpen = false },
        modifier = Modifier.background(Color(0xEE1E2430), RoundedCornerShape(12.dp)),
      ) {
        DropdownMenuItem(
          text = {
            Text(
              "📂 Open Subtitle...",
              color = Color(0xFF00E5FF),
              fontWeight = FontWeight.SemiBold,
            )
          },
          onClick = {
            isSubtitlesMenuOpen = false
            onAddSubtitle()
          },
        )

        HorizontalDivider(color = Color(0x33FFFFFF))

        val hasActiveSub = subtitles.any { (it.mainSelection?.toInt() ?: -1) > -1 }
        DropdownMenuItem(
          text = {
            Text(
              "Off (None)",
              color = if (!hasActiveSub) Color(0xFF00E5FF) else Color(0xFF8A99AD),
              fontWeight = if (!hasActiveSub) FontWeight.Bold else FontWeight.Normal,
            )
          },
          onClick = {
            isSubtitlesMenuOpen = false
            onDisableSubtitles()
          },
        )

        subtitles.forEach { track ->
          val isSelected = (track.mainSelection?.toInt() ?: -1) > -1
          val title = getSubtitleDisplayName(track)
          DropdownMenuItem(
            text = {
              Text(
                title,
                color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              )
            },
            onClick = {
              isSubtitlesMenuOpen = false
              onSelectSubtitle(track.id)
            },
          )
        }
      }
    }

    ControlsButton(
      Icons.Default.Audiotrack,
      onClick = onAudioClick,
      onLongClick = onAudioLongClick,
    )

    var isMoreMenuOpen by remember { mutableStateOf(false) }
    Box {
      ControlsButton(
        Icons.Default.MoreVert,
        onClick = { isMoreMenuOpen = true },
      )
      DropdownMenu(
        expanded = isMoreMenuOpen,
        onDismissRequest = { isMoreMenuOpen = false },
        modifier = Modifier.background(Color(0xEE1E2430), RoundedCornerShape(12.dp)),
      ) {
        DropdownMenuItem(
          text = { Text("🎨 Video Filters", color = Color.White) },
          onClick = {
            onOpenVideoFilters()
            isMoreMenuOpen = false
          },
        )
        DropdownMenuItem(
          text = { Text("⏱️ Playback Speed", color = Color.White) },
          onClick = {
            onOpenPlaybackSpeed()
            isMoreMenuOpen = false
          },
        )
        DropdownMenuItem(
          text = { Text("🎵 Audio Delay", color = Color.White) },
          onClick = {
            onOpenAudioDelay()
            isMoreMenuOpen = false
          },
        )
        DropdownMenuItem(
          text = { Text("💬 Subtitle Settings", color = Color.White) },
          onClick = {
            onOpenSubtitleSettings()
            isMoreMenuOpen = false
          },
        )
        DropdownMenuItem(
          text = { Text("⚙️ More Controls & Timers", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
          onClick = {
            onOpenMoreSheet()
            isMoreMenuOpen = false
          },
        )
      }
    }
  }
}

private fun getSubtitleDisplayName(track: TrackNode): String {
  val title = track.title?.trim()
  if (!title.isNullOrBlank()) return title
  val lang = track.lang?.trim()
  if (!lang.isNullOrBlank()) {
    try {
      val displayLang = Locale.forLanguageTag(lang).getDisplayLanguage(Locale.ENGLISH)
      if (displayLang.isNotBlank()) return displayLang
    } catch (_: Exception) {}
    return lang
  }
  return "Track #${track.id}"
}
