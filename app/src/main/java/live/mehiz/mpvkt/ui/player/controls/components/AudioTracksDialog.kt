package live.mehiz.mpvkt.ui.player.controls.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.collections.immutable.ImmutableList
import live.mehiz.mpvkt.ui.player.TrackNode
import java.util.Locale

private val LANGUAGE_MAP = mapOf(
  "en" to "English",
  "eng" to "English",
  "hi" to "Hindi",
  "hin" to "Hindi",
  "ta" to "Tamil",
  "tam" to "Tamil",
  "si" to "Sinhala",
  "sin" to "Sinhala",
  "te" to "Telugu",
  "tel" to "Telugu",
  "ml" to "Malayalam",
  "mal" to "Malayalam",
  "kn" to "Kannada",
  "kan" to "Kannada",
  "mr" to "Marathi",
  "mar" to "Marathi",
  "bn" to "Bengali",
  "ben" to "Bengali",
  "ur" to "Urdu",
  "urd" to "Urdu",
  "ja" to "Japanese",
  "jpn" to "Japanese",
  "ko" to "Korean",
  "kor" to "Korean",
  "zh" to "Chinese",
  "chi" to "Chinese",
  "zho" to "Chinese",
  "es" to "Spanish",
  "spa" to "Spanish",
  "fr" to "French",
  "fre" to "French",
  "fra" to "French",
  "de" to "German",
  "ger" to "German",
  "deu" to "German",
  "ru" to "Russian",
  "rus" to "Russian",
  "it" to "Italian",
  "ita" to "Italian",
  "pt" to "Portuguese",
  "por" to "Portuguese",
  "ar" to "Arabic",
  "ara" to "Arabic",
  "tr" to "Turkish",
  "tur" to "Turkish",
  "vi" to "Vietnamese",
  "vie" to "Vietnamese",
  "th" to "Thai",
  "tha" to "Thai",
  "id" to "Indonesian",
  "ind" to "Indonesian",
  "ms" to "Malay",
  "may" to "Malay",
  "msa" to "Malay",
  "fa" to "Persian",
  "per" to "Persian",
  "fas" to "Persian",
  "nl" to "Dutch",
  "dut" to "Dutch",
  "nld" to "Dutch",
  "pl" to "Polish",
  "pol" to "Polish",
  "sv" to "Swedish",
  "swe" to "Swedish",
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

  LANGUAGE_MAP[key]?.let { return it }

  val baseKey = key.substringBefore("-").substringBefore("_")
  LANGUAGE_MAP[baseKey]?.let { return it }

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

fun formatAudioTrackLabel(index: Int, track: TrackNode): String {
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

  val langOrTitle = when {
    resolvedFromTitle != null -> resolvedFromTitle + channelSuffix
    !title.isNullOrBlank() && title.length > 4 -> title + channelSuffix
    resolvedFromLang != null -> resolvedFromLang + channelSuffix
    !title.isNullOrBlank() -> title + channelSuffix
    !lang.isNullOrBlank() -> lang + channelSuffix
    else -> channelSuffix.trim().ifEmpty { null }
  }

  return if (langOrTitle != null) {
    "Audio Track #$index - $langOrTitle"
  } else {
    "Audio Track #$index"
  }
}

@Composable
fun AudioTracksDialog(
  tracks: ImmutableList<TrackNode>,
  useSwAudioDecoder: Boolean,
  onToggleSwAudioDecoder: (Boolean) -> Unit,
  onSelectTrack: (TrackNode) -> Unit,
  onDisableAudio: () -> Unit,
  onOpenAudioBrowser: () -> Unit,
  onOpenAudioDelay: () -> Unit,
  onDismissRequest: () -> Unit,
) {
  val anyTrackSelected = remember(tracks) {
    tracks.any { it.selected == true || (it.mainSelection?.toInt() ?: -1) > -1 }
  }

  Dialog(onDismissRequest = onDismissRequest) {
    Card(
      modifier = Modifier
        .widthIn(min = 320.dp, max = 500.dp)
        .padding(16.dp),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = Color(0xDD0D1420), // Frosted Translucent Acrylic Glass
      ),
      border = BorderStroke(1.dp, Color(0x3300E5FF)),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              Icons.Default.Audiotrack,
              contentDescription = null,
              tint = Color(0xFF00E5FF),
              modifier = Modifier.size(22.dp),
            )
            Text(
              "Audio track",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
            )
          }
          IconButton(
            onClick = onDismissRequest,
            modifier = Modifier.size(28.dp),
          ) {
            Icon(
              Icons.Default.Close,
              contentDescription = "Close",
              tint = Color(0xFF8A99AD),
              modifier = Modifier.size(20.dp),
            )
          }
        }

        // Use SW audio decoder option
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSwAudioDecoder(!useSwAudioDecoder) }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Checkbox(
            checked = useSwAudioDecoder,
            onCheckedChange = onToggleSwAudioDecoder,
            colors = CheckboxDefaults.colors(
              checkedColor = Color(0xFF00E5FF),
              checkmarkColor = Color(0xFF0D1420),
              uncheckedColor = Color(0xFF8A99AD),
            ),
          )
          Text(
            "Use SW audio decoder",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
          )
        }

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 8.dp),
          color = Color(0x22FFFFFF),
        )

        // Tracks List
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp),
        ) {
          // Disable Option
          item {
            val isAudioDisabled = !anyTrackSelected
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onDisableAudio()
                  onDismissRequest()
                }
                .padding(vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              RadioButton(
                selected = isAudioDisabled,
                onClick = {
                  onDisableAudio()
                  onDismissRequest()
                },
                colors = RadioButtonDefaults.colors(
                  selectedColor = Color(0xFF00E5FF),
                  unselectedColor = Color(0xFF8A99AD),
                ),
              )
              Text(
                "Disable",
                color = if (isAudioDisabled) Color(0xFF00E5FF) else Color(0xFFEF5350),
                fontWeight = if (isAudioDisabled) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
              )
            }
          }

          // Audio Tracks
          itemsIndexed(tracks) { index, track ->
            val isSelected = track.selected == true || (track.mainSelection?.toInt() ?: -1) > -1
            val label = formatAudioTrackLabel(index + 1, track)

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onSelectTrack(track)
                  onDismissRequest()
                }
                .padding(vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              RadioButton(
                selected = isSelected,
                onClick = {
                  onSelectTrack(track)
                  onDismissRequest()
                },
                colors = RadioButtonDefaults.colors(
                  selectedColor = Color(0xFF00E5FF),
                  unselectedColor = Color(0xFF8A99AD),
                ),
              )
              Text(
                label,
                color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          if (tracks.isEmpty()) {
            item {
              Text(
                "No embedded audio tracks found.",
                color = Color(0xFF8A99AD),
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
              )
            }
          }
        }

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 8.dp),
          color = Color(0x22FFFFFF),
        )

        // Bottom Actions Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Button(
            onClick = onOpenAudioBrowser,
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0x3300E5FF),
            ),
            border = BorderStroke(1.dp, Color(0xFF00E5FF)),
            shape = RoundedCornerShape(10.dp),
          ) {
            Icon(
              Icons.Default.FolderOpen,
              contentDescription = null,
              tint = Color(0xFF00E5FF),
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
              "Open",
              color = Color(0xFF00E5FF),
              fontWeight = FontWeight.Bold,
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = onOpenAudioDelay,
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White,
              ),
              border = BorderStroke(1.dp, Color(0x44FFFFFF)),
              shape = RoundedCornerShape(10.dp),
            ) {
              Text(
                "🎵 Audio Delay",
                fontSize = 12.sp,
                color = Color.White,
              )
            }

            TextButton(onClick = onDismissRequest) {
              Text("Close", color = Color(0xFF8A99AD))
            }
          }
        }
      }
    }
  }
}
