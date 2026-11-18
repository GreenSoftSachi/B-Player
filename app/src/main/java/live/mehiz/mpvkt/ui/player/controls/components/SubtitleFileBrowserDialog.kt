package live.mehiz.mpvkt.ui.player.controls.components

import android.os.Environment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File
import java.util.Locale

private val SUBTITLE_EXTENSIONS = setOf("srt", "ass", "ssa", "vtt", "sub", "txt")

@Composable
fun SubtitleFileBrowserDialog(
  initialDirectory: String?,
  onSelectSubtitleFile: (File) -> Unit,
  onDismissRequest: () -> Unit,
) {
  val defaultDir = remember(initialDirectory) {
    if (!initialDirectory.isNullOrBlank()) {
      val f = File(initialDirectory)
      if (f.exists() && f.isDirectory) f else f.parentFile ?: Environment.getExternalStorageDirectory()
    } else {
      val movies = File(Environment.getExternalStorageDirectory(), "Movies")
      if (movies.exists()) movies else Environment.getExternalStorageDirectory()
    }
  }

  var currentDir by remember { mutableStateOf(defaultDir) }

  val entries = remember(currentDir) {
    val filesList = currentDir.listFiles()?.toList() ?: emptyList()
    val folders = filesList.filter { it.isDirectory && !it.name.startsWith(".") }.sortedBy { it.name.lowercase(Locale.ROOT) }
    val subs = filesList.filter { it.isFile && it.extension.lowercase(Locale.ROOT) in SUBTITLE_EXTENSIONS }
      .sortedBy { it.name.lowercase(Locale.ROOT) }
    Pair(folders, subs)
  }

  Dialog(onDismissRequest = onDismissRequest) {
    Card(
      modifier = Modifier
        .widthIn(min = 320.dp, max = 500.dp)
        .padding(16.dp),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = Color(0xDD0D1420), // Translucent Frosted Glass
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
              Icons.Default.Subtitles,
              contentDescription = null,
              tint = Color(0xFF00E5FF),
              modifier = Modifier.size(22.dp),
            )
            Text(
              "Browse Subtitles",
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

        // Current Path Breadcrumb
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x331E2430))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
          Text(
            text = currentDir.absolutePath,
            color = Color(0xFF8A99AD),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        HorizontalDivider(color = Color(0x22FFFFFF))

        // Files & Folders List
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(top = 4.dp),
        ) {
          // Parent Folder Up Button
          val parent = currentDir.parentFile
          if (parent != null && parent.canRead()) {
            item {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { currentDir = parent }
                  .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  Icons.Default.TurnLeft,
                  contentDescription = "Up",
                  tint = Color(0xFF00E5FF),
                  modifier = Modifier.size(20.dp),
                )
                Text(
                  ".. (Parent Folder)",
                  color = Color(0xFF00E5FF),
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp,
                )
              }
            }
          }

          // Folders
          items(entries.first) { folder ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { currentDir = folder }
                .padding(vertical = 8.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = Color(0xFFFFB74D),
                modifier = Modifier.size(20.dp),
              )
              Text(
                folder.name,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
            }
          }

          // Subtitles
          items(entries.second) { subFile ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  onSelectSubtitleFile(subFile)
                  onDismissRequest()
                }
                .padding(vertical = 8.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                Icons.Default.Subtitles,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(20.dp),
              )
              Text(
                subFile.name,
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
            }
          }

          if (entries.first.isEmpty() && entries.second.isEmpty()) {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  "No subtitle files (.srt, .ass, etc.) found in this folder.",
                  color = Color(0xFF8A99AD),
                  fontSize = 12.sp,
                )
              }
            }
          }
        }
      }
    }
  }
}
