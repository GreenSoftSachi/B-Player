package live.mehiz.mpvkt.ui.player.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
  audioTracks: ImmutableList<TrackNode> = persistentListOf(),
  onSelectAudio: (Int) -> Unit = {},
  onAudioLongClick: () -> Unit = {},
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
        modifier = Modifier
          .background(Color(0xCC0D1420), RoundedCornerShape(14.dp))
          .border(BorderStroke(1.dp, Color(0x3300E5FF)), RoundedCornerShape(14.dp)),
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
        modifier = Modifier
          .background(Color(0xCC0D1420), RoundedCornerShape(14.dp))
          .border(BorderStroke(1.dp, Color(0x3300E5FF)), RoundedCornerShape(14.dp)),
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

    var isAudioMenuOpen by remember { mutableStateOf(false) }
    Box {
      ControlsButton(
        Icons.Default.Audiotrack,
        onClick = { isAudioMenuOpen = true },
        onLongClick = onAudioLongClick,
      )
      DropdownMenu(
        expanded = isAudioMenuOpen,
        onDismissRequest = { isAudioMenuOpen = false },
        modifier = Modifier
          .background(Color(0xCC0D1420), RoundedCornerShape(14.dp))
          .border(BorderStroke(1.dp, Color(0x3300E5FF)), RoundedCornerShape(14.dp)),
      ) {
        audioTracks.forEach { track ->
          val isSelected = (track.mainSelection?.toInt() ?: -1) > -1 || track.selected == true
          val title = getAudioDisplayName(track)
          DropdownMenuItem(
            text = {
              Text(
                title,
                color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              )
            },
            onClick = {
              isAudioMenuOpen = false
              onSelectAudio(track.id)
            },
          )
        }

        if (audioTracks.isNotEmpty()) {
          HorizontalDivider(color = Color(0x33FFFFFF))
        }

        DropdownMenuItem(
          text = {
            Text(
              "🎵 Audio Delay...",
              color = Color(0xFF00E5FF),
              fontWeight = FontWeight.SemiBold,
            )
          },
          onClick = {
            isAudioMenuOpen = false
            onOpenAudioDelay()
          },
        )
      }
    }

    var isMoreMenuOpen by remember { mutableStateOf(false) }
    Box {
      ControlsButton(
        Icons.Default.MoreVert,
        onClick = { isMoreMenuOpen = true },
      )
      DropdownMenu(
        expanded = isMoreMenuOpen,
        onDismissRequest = { isMoreMenuOpen = false },
        modifier = Modifier
          .background(Color(0xCC0D1420), RoundedCornerShape(14.dp))
          .border(BorderStroke(1.dp, Color(0x3300E5FF)), RoundedCornerShape(14.dp)),
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

private val LANGUAGE_MAP = mapOf(
  // South Asian Languages
  "hi" to "Hindi",
  "hin" to "Hindi",
  "ta" to "Tamil",
  "tam" to "Tamil",
  "ti" to "Tamil", // user-reported typo/tag for Tamil
  "te" to "Telugu",
  "tel" to "Telugu",
  "ml" to "Malayalam",
  "mal" to "Malayalam",
  "kn" to "Kannada",
  "kan" to "Kannada",
  "si" to "Sinhala",
  "sin" to "Sinhala",
  "bn" to "Bengali",
  "ben" to "Bengali",
  "mr" to "Marathi",
  "mar" to "Marathi",
  "gu" to "Gujarati",
  "guj" to "Gujarati",
  "pa" to "Punjabi",
  "pan" to "Punjabi",
  "ur" to "Urdu",
  "urd" to "Urdu",

  // Common World Languages
  "en" to "English",
  "eng" to "English",
  "es" to "Spanish",
  "spa" to "Spanish",
  "fr" to "French",
  "fre" to "French",
  "fra" to "French",
  "de" to "German",
  "ger" to "German",
  "deu" to "German",
  "it" to "Italian",
  "ita" to "Italian",
  "pt" to "Portuguese",
  "por" to "Portuguese",
  "ru" to "Russian",
  "rus" to "Russian",
  "ja" to "Japanese",
  "jpn" to "Japanese",
  "ko" to "Korean",
  "kor" to "Korean",
  "zh" to "Chinese",
  "chi" to "Chinese",
  "zho" to "Chinese",
  "ar" to "Arabic",
  "ara" to "Arabic",
  "tr" to "Turkish",
  "tur" to "Turkish",
  "th" to "Thai",
  "tha" to "Thai",
  "vi" to "Vietnamese",
  "vie" to "Vietnamese",
  "id" to "Indonesian",
  "ind" to "Indonesian",
  "ms" to "Malay",
  "may" to "Malay",
  "msa" to "Malay",
  "fa" to "Persian",
  "per" to "Persian",
  "fas" to "Persian",
  "el" to "Greek",
  "ell" to "Greek",
  "gre" to "Greek",
  "nl" to "Dutch",
  "dut" to "Dutch",
  "nld" to "Dutch",
  "pl" to "Polish",
  "pol" to "Polish",
  "sv" to "Swedish",
  "swe" to "Swedish",
  "no" to "Norwegian",
  "nor" to "Norwegian",
  "da" to "Danish",
  "dan" to "Danish",
  "fi" to "Finnish",
  "fin" to "Finnish",
  "cs" to "Czech",
  "ces" to "Czech",
  "cze" to "Czech",
  "hu" to "Hungarian",
  "hun" to "Hungarian",
  "ro" to "Romanian",
  "ron" to "Romanian",
  "rum" to "Romanian",
  "uk" to "Ukrainian",
  "ukr" to "Ukrainian",
  "he" to "Hebrew",
  "heb" to "Hebrew",
)

private fun resolveLanguageName(codeOrName: String?): String? {
  if (codeOrName.isNullOrBlank()) return null
  val raw = codeOrName.trim()
  val key = raw.lowercase(Locale.ROOT)

  // Direct lookup
  LANGUAGE_MAP[key]?.let { return it }

  // Check prefix before dash/underscore (e.g. en-US -> en)
  val baseKey = key.substringBefore("-").substringBefore("_")
  LANGUAGE_MAP[baseKey]?.let { return it }

  // Check Java Locale
  try {
    val loc = Locale.forLanguageTag(raw)
    val display = loc.getDisplayLanguage(Locale.ENGLISH)
    if (display.isNotBlank() && !display.equals(raw, true) && display.length > raw.length) {
      return display
    }
  } catch (_: Exception) {}

  try {
    val loc2 = Locale(baseKey)
    val display2 = loc2.getDisplayLanguage(Locale.ENGLISH)
    if (display2.isNotBlank() && !display2.equals(raw, true) && display2.length > baseKey.length) {
      return display2
    }
  } catch (_: Exception) {}

  return null
}

private fun getSubtitleDisplayName(track: TrackNode): String {
  val title = track.title?.trim()
  val lang = track.lang?.trim()

  val resolvedFromLang = resolveLanguageName(lang)
  val resolvedFromTitle = resolveLanguageName(title)

  return when {
    // If title was a short language code like "hi" or "ta", show "Hindi", "Tamil"
    resolvedFromTitle != null -> resolvedFromTitle
    
    // If title has descriptive text (e.g. "Hindi [Original]")
    !title.isNullOrBlank() && title.length > 4 -> title
    
    // If lang gave full name
    resolvedFromLang != null -> resolvedFromLang
    
    // If title is non-blank
    !title.isNullOrBlank() -> title
    
    // Fallback
    !lang.isNullOrBlank() -> lang
    else -> "Subtitle #${track.id}"
  }
}

private fun getAudioDisplayName(track: TrackNode): String {
  val title = track.title?.trim()
  val lang = track.lang?.trim()

  val resolvedFromLang = resolveLanguageName(lang)
  val resolvedFromTitle = resolveLanguageName(title)

  val channels = when ((track.demuxChannelCount ?: track.audioChannels)?.toInt()) {
    6 -> "5.1"
    8 -> "7.1"
    2 -> "Stereo"
    1 -> "Mono"
    else -> null
  }
  val channelSuffix = if (channels != null) " ($channels)" else ""

  return when {
    resolvedFromTitle != null -> resolvedFromTitle + channelSuffix
    !title.isNullOrBlank() && title.length > 4 -> title + channelSuffix
    resolvedFromLang != null -> resolvedFromLang + channelSuffix
    !title.isNullOrBlank() -> title + channelSuffix
    !lang.isNullOrBlank() -> lang + channelSuffix
    else -> "Audio Track #${track.id}$channelSuffix"
  }
}
