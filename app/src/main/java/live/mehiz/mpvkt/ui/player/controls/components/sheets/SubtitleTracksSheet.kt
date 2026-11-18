package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.collections.immutable.ImmutableList
import live.mehiz.mpvkt.ui.player.TrackNode
import live.mehiz.mpvkt.ui.theme.spacing

@Composable
fun SubtitlesSheet(
  tracks: ImmutableList<TrackNode>,
  onSelect: (Int) -> Unit,
  onAddSubtitle: () -> Unit,
  onOpenSubtitleSettings: () -> Unit,
  onOpenSubtitleDelay: () -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  GenericTracksSheet(
    tracks,
    onDismissRequest = onDismissRequest,
    header = {
      AddTrackRow(
        "Open Subtitle File",
        onAddSubtitle,
      )
    },
    track = { track ->
      SubtitleTrackRow(
        title = getTrackTitle(track),
        selected = track.mainSelection?.toInt() ?: -1,
        onClick = { onSelect(track.id) },
      )
    },
    modifier = modifier,
  )
}

@Composable
fun SubtitleTrackRow(title: String, selected: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val isSelected = selected > -1
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(start = MaterialTheme.spacing.smaller, end = MaterialTheme.spacing.medium),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Checkbox(
      isSelected,
      onCheckedChange = { _ -> onClick() },
      colors = CheckboxDefaults.colors(
        checkedColor = Color(0xFF00E5FF),
        uncheckedColor = Color(0xFF8A99AD),
      ),
    )
    Text(
      title,
      color = if (isSelected) Color(0xFF00E5FF) else Color.White,
      fontStyle = if (isSelected) FontStyle.Italic else FontStyle.Normal,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
    )
    Spacer(modifier = Modifier.weight(1f))
    if (selected != -1) {
      Text(
        "#${selected + 1}",
        color = Color(0xFF00E5FF),
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}
