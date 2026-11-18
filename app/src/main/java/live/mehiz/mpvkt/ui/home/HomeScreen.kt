package live.mehiz.mpvkt.ui.home

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.ui.player.PlayerActivity
import live.mehiz.mpvkt.ui.preferences.PreferencesScreen
import live.mehiz.mpvkt.ui.utils.LocalBackStack
import java.io.File
import java.util.Locale

data class VideoItem(
  val id: Long,
  val title: String,
  val path: String,
  val durationMs: Long,
  val sizeBytes: Long,
  val uri: Uri,
  val resolution: String,
  val dateAdded: Long,
) {
  fun formatDuration(): String {
    val totalSec = durationMs / 1000
    val sec = totalSec % 60
    val min = (totalSec / 60) % 60
    val hrs = totalSec / 3600
    return if (hrs > 0) {
      String.format(Locale.US, "%d:%02d:%02d", hrs, min, sec)
    } else {
      String.format(Locale.US, "%02d:%02d", min, sec)
    }
  }

  fun formatSize(): String {
    val mb = sizeBytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
      String.format(Locale.US, "%.2f GB", mb / 1024.0)
    } else {
      String.format(Locale.US, "%.1f MB", mb)
    }
  }
}

data class FolderGroup(
  val name: String,
  val path: String,
  val videos: List<VideoItem>,
)

@Serializable
object HomeScreen : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val coroutineScope = rememberCoroutineScope()

    var allVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var folderGroups by remember { mutableStateOf<List<FolderGroup>>(emptyList()) }
    var activeFolder by remember { mutableStateOf<FolderGroup?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isGridView by remember { mutableStateOf(false) }
    var isFoldersMode by remember { mutableStateOf(true) }

    fun scanMedia() {
      coroutineScope.launch {
        isLoading = true
        val videos = withContext(Dispatchers.IO) { queryVideos(context) }
        val folders = withContext(Dispatchers.IO) { groupIntoFolders(videos) }
        allVideos = videos
        folderGroups = folders
        if (activeFolder != null) {
          activeFolder = folders.find { it.path == activeFolder?.path }
        }
        isLoading = false
      }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
      val granted = permissions.values.any { it }
      if (granted) {
        scanMedia()
      } else {
        isLoading = false
      }
    }

    LaunchedEffect(Unit) {
      val neededPermissions = mutableListOf<String>()
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
          neededPermissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        }
      } else {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
          neededPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
      }

      if (neededPermissions.isNotEmpty()) {
        permissionLauncher.launch(neededPermissions.toTypedArray())
      } else {
        scanMedia()
      }
    }

    // Handle back button: if inside a folder, return to folder list!
    BackHandler(enabled = activeFolder != null) {
      activeFolder = null
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Column {
              Text(
                text = if (activeFolder != null) activeFolder!!.name else "B-Player",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (activeFolder != null) {
                Text(
                  text = "${activeFolder!!.videos.size} videos",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          },
          navigationIcon = {
            if (activeFolder != null) {
              IconButton(onClick = { activeFolder = null }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            } else {
              IconButton(onClick = { isFoldersMode = !isFoldersMode }) {
                Icon(
                  if (isFoldersMode) Icons.Default.Folder else Icons.Default.VideoFile,
                  contentDescription = "Toggle Folders / Files",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
          },
          actions = {
            // Grid / List toggle
            IconButton(onClick = { isGridView = !isGridView }) {
              Icon(
                if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                contentDescription = "Toggle Grid / List",
              )
            }
            // Refresh Scanner
            IconButton(onClick = { scanMedia() }) {
              Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
            // Settings
            IconButton(onClick = { backstack.add(PreferencesScreen) }) {
              Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
        )
      },
    ) { padding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
      ) {
        if (isLoading) {
          Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Scanning media files...", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        } else if (allVideos.isEmpty()) {
          Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Icon(
              Icons.Default.Movie,
              contentDescription = null,
              modifier = Modifier.size(64.dp),
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              "No videos found",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              "Check storage permissions or refresh",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else if (isFoldersMode && activeFolder == null) {
          // Folders List View
          if (isGridView) {
            LazyVerticalGrid(
              columns = GridCells.Fixed(2),
              contentPadding = PaddingValues(12.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(folderGroups) { folder ->
                FolderCardGrid(folder = folder, onClick = { activeFolder = folder })
              }
            }
          } else {
            LazyColumn(
              contentPadding = PaddingValues(vertical = 8.dp),
            ) {
              items(folderGroups) { folder ->
                FolderRowItem(folder = folder, onClick = { activeFolder = folder })
              }
            }
          }
        } else {
          // Videos List (Inside Active Folder or Flat Files view)
          val videosToShow = if (activeFolder != null) activeFolder!!.videos else allVideos
          if (isGridView) {
            LazyVerticalGrid(
              columns = GridCells.Fixed(2),
              contentPadding = PaddingValues(12.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(videosToShow) { video ->
                VideoCardGrid(video = video, onClick = { playVideo(context, video) })
              }
            }
          } else {
            LazyColumn(
              contentPadding = PaddingValues(vertical = 6.dp),
            ) {
              items(videosToShow) { video ->
                VideoRowItem(video = video, onClick = { playVideo(context, video) })
              }
            }
          }
        }
      }
    }
  }

  fun playFile(filepath: String, context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, filepath.toUri()).apply {
      setClass(context, PlayerActivity::class.java)
    }
    context.startActivity(intent)
  }

  fun playFile(uri: Uri, context: Context) {
    val intent = Intent(context, PlayerActivity::class.java).apply {
      data = uri
    }
    context.startActivity(intent)
  }

  private fun playVideo(context: Context, video: VideoItem) {
    val intent = Intent(context, PlayerActivity::class.java).apply {
      data = video.uri
      putExtra("title", video.title)
    }
    context.startActivity(intent)
  }

  private fun queryVideos(context: Context): List<VideoItem> {
    val list = mutableListOf<VideoItem>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    val projection = arrayOf(
      MediaStore.Video.Media._ID,
      MediaStore.Video.Media.TITLE,
      MediaStore.Video.Media.DATA,
      MediaStore.Video.Media.DURATION,
      MediaStore.Video.Media.SIZE,
      MediaStore.Video.Media.DATE_ADDED,
      MediaStore.Video.Media.WIDTH,
      MediaStore.Video.Media.HEIGHT,
    )

    // Filter junk dev files: duration >= 3s and size >= 200KB
    val selection = "${MediaStore.Video.Media.SIZE} >= 204800 AND ${MediaStore.Video.Media.DURATION} >= 3000"

    try {
      context.contentResolver.query(
        collection,
        projection,
        selection,
        null,
        "${MediaStore.Video.Media.DATE_ADDED} DESC",
      )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
        val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
        val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        val wCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
        val hCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

        while (cursor.moveToNext()) {
          val id = cursor.getLong(idCol)
          val title = cursor.getString(titleCol) ?: "Video"
          val path = cursor.getString(dataCol) ?: ""
          val duration = cursor.getLong(durCol)
          val size = cursor.getLong(sizeCol)
          val date = cursor.getLong(dateCol)
          val width = cursor.getInt(wCol)
          val height = cursor.getInt(hCol)

          // Skip hidden or system cache folders
          val lower = path.lowercase(Locale.ROOT)
          if (lower.contains("/. ") || lower.contains("/android/data/") || lower.contains("/cache/")) {
            continue
          }

          val uri = ContentUris.withAppendedId(collection, id)
          val resolution = if (width >= 3840 || height >= 2160) "4K" else if (width >= 1280 || height >= 720) "HD" else "SD"

          list.add(
            VideoItem(
              id = id,
              title = title,
              path = path,
              durationMs = duration,
              sizeBytes = size,
              uri = uri,
              resolution = resolution,
              dateAdded = date,
            ),
          )
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return list
  }

  private fun groupIntoFolders(videos: List<VideoItem>): List<FolderGroup> {
    val map = mutableMapOf<String, MutableList<VideoItem>>()
    for (v in videos) {
      val parent = File(v.path).parent ?: "Internal Storage"
      val group = map.getOrPut(parent) { mutableListOf() }
      group.add(v)
    }
    return map.map { (path, vids) ->
      FolderGroup(
        name = File(path).name.ifEmpty { "Videos" },
        path = path,
        videos = vids,
      )
    }.sortedBy { it.name.lowercase(Locale.ROOT) }
  }
}

@Composable
fun FolderRowItem(
  folder: FolderGroup,
  onClick: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 5.dp)
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Folder icon with red badge count
      BadgedBox(
        badge = {
          val count = folder.videos.size
          Badge(
            containerColor = Color(0xFFFF3B30),
            contentColor = Color.White,
          ) {
            Text(if (count > 99) "99+" else count.toString(), fontWeight = FontWeight.Bold, fontSize = 10.sp)
          }
        },
      ) {
        Icon(
          Icons.Default.Folder,
          contentDescription = null,
          modifier = Modifier.size(44.dp),
          tint = MaterialTheme.colorScheme.primary,
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = folder.name,
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${folder.videos.size} videos",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Icon(
        Icons.Default.PlayArrow,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
fun FolderCardGrid(
  folder: FolderGroup,
  onClick: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(1.1f)
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(14.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Icon(
          Icons.Default.Folder,
          contentDescription = null,
          modifier = Modifier.size(42.dp),
          tint = MaterialTheme.colorScheme.primary,
        )
        Surface(
          shape = CircleShape,
          color = Color(0xFFFF3B30),
          modifier = Modifier.padding(2.dp),
        ) {
          Text(
            text = if (folder.videos.size > 99) "99+" else folder.videos.size.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
          )
        }
      }

      Column {
        Text(
          text = folder.name,
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${folder.videos.size} videos",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
fun VideoRowItem(
  video: VideoItem,
  onClick: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 5.dp)
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Thumbnail Box with Duration Badge
      Box(
        modifier = Modifier
          .size(width = 96.dp, height = 64.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          Icons.Default.PlayArrow,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(28.dp),
        )
        Surface(
          color = Color.Black.copy(alpha = 0.75f),
          shape = RoundedCornerShape(4.dp),
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(4.dp),
        ) {
          Text(
            text = video.formatDuration(),
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
          )
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = video.title,
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            shape = RoundedCornerShape(4.dp),
          ) {
            Text(
              text = video.resolution,
              color = MaterialTheme.colorScheme.primary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = video.formatSize(),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
fun VideoCardGrid(
  video: VideoItem,
  onClick: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ),
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 9f)
          .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          Icons.Default.PlayArrow,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(36.dp),
        )
        Surface(
          color = Color.Black.copy(alpha = 0.75f),
          shape = RoundedCornerShape(4.dp),
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(6.dp),
        ) {
          Text(
            text = video.formatDuration(),
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
          )
        }
      }

      Column(modifier = Modifier.padding(10.dp)) {
        Text(
          text = video.title,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = video.resolution,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = video.formatSize(),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
