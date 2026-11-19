package live.mehiz.mpvkt.ui.home

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.media.MediaScannerConnection
import android.provider.MediaStore
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
  val segments: List<String> = emptyList(),
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

data class SubFolderInfo(
  val name: String,
  val fullPathList: List<String>,
  val videoCount: Int,
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
    var currentPathSegments by rememberSaveable { mutableStateOf("") }
    val currentPath = remember(currentPathSegments) {
      if (currentPathSegments.isEmpty()) emptyList<String>() else currentPathSegments.split("|||")
    }
    val updatePath: (List<String>) -> Unit = { list ->
      currentPathSegments = if (list.isEmpty()) "" else list.joinToString("|||")
    }
    var isLoading by remember { mutableStateOf(true) }
    var isGridView by rememberSaveable { mutableStateOf(false) }
    var isFoldersMode by rememberSaveable { mutableStateOf(true) }

    var selectedVideoForAction by remember { mutableStateOf<VideoItem?>(null) }
    var selectedFolderForAction by remember { mutableStateOf<SubFolderInfo?>(null) }
    var isActionDialogOpen by remember { mutableStateOf(false) }
    var isDeleteDialogOpen by remember { mutableStateOf(false) }
    var clipboardPath by remember { mutableStateOf<String?>(null) }
    var clipboardIsFolder by remember { mutableStateOf(false) }
    var clipboardName by remember { mutableStateOf<String?>(null) }

    fun scanMedia() {
      coroutineScope.launch {
        isLoading = true
        val videos = withContext(Dispatchers.IO) { queryVideos(context) }
        allVideos = videos

        // Always start at root Storage view (Internal Storage & SD Card)
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

    // Back button: navigate up one folder level!
    BackHandler(enabled = isFoldersMode && currentPath.isNotEmpty()) {
      updatePath(currentPath.dropLast(1))
    }

    // Compute visible subfolders and direct videos for currentPath
    val matchingVideos = remember(allVideos, currentPath) {
      allVideos.filter { v ->
        v.segments.size >= currentPath.size && v.segments.take(currentPath.size) == currentPath
      }
    }

    val visibleSubFolders = remember(matchingVideos, currentPath) {
      if (!isFoldersMode) return@remember emptyList()
      val depth = currentPath.size
      val groups = matchingVideos
        .filter { it.segments.size > depth }
        .groupBy { it.segments[depth] }

      groups.map { (subName, vids) ->
        SubFolderInfo(
          name = subName,
          fullPathList = currentPath + subName,
          videoCount = vids.size,
        )
      }.sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    val visibleDirectVideos = remember(matchingVideos, currentPath, isFoldersMode, allVideos) {
      if (!isFoldersMode) {
        allVideos
      } else {
        matchingVideos.filter { it.segments.size == currentPath.size }
      }
    }

    val currentFolderTitle = remember(currentPath, isFoldersMode) {
      if (!isFoldersMode) {
        "All Videos"
      } else if (currentPath.isEmpty()) {
        "Storage"
      } else {
        currentPath.joinToString(" / ")
      }
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Column {
              Text(
                text = currentFolderTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (isFoldersMode && currentPath.isNotEmpty()) {
                Text(
                  text = "${matchingVideos.size} videos",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          },
          navigationIcon = {
            if (isFoldersMode && currentPath.isNotEmpty()) {
              IconButton(onClick = { updatePath(currentPath.dropLast(1)) }) {
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
            if (clipboardPath != null) {
              IconButton(
                onClick = {
                  coroutineScope.launch(Dispatchers.IO) {
                    try {
                      val src = File(clipboardPath!!)
                      if (src.exists()) {
                        val targetDir = resolveRealDirectory(currentPath)
                        if (!targetDir.exists()) targetDir.mkdirs()
                        val dest = File(targetDir, src.name)
                        if (src.isDirectory) {
                          src.copyRecursively(dest, overwrite = true)
                        } else {
                          src.copyTo(dest, overwrite = true)
                        }
                        MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), null, null)
                        withContext(Dispatchers.Main) {
                          Toast.makeText(context, "Pasted '${dest.name}' successfully", Toast.LENGTH_SHORT).show()
                          clipboardPath = null
                          clipboardName = null
                          scanMedia()
                        }
                      }
                    } catch (e: Exception) {
                      e.printStackTrace()
                      withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Paste failed: ${e.message}", Toast.LENGTH_SHORT).show()
                      }
                    }
                  }
                },
              ) {
                Icon(
                  Icons.Default.ContentPaste,
                  contentDescription = "Paste here",
                  tint = Color(0xFF00E5FF),
                )
              }
            }
            IconButton(onClick = { isGridView = !isGridView }) {
              Icon(
                if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                contentDescription = "Toggle Grid / List",
              )
            }
            IconButton(onClick = { scanMedia() }) {
              Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
            IconButton(onClick = { backstack.add(PreferencesScreen) }) {
              Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
        )
      },
    ) { paddingValues ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
      ) {
        if (isLoading) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        } else if (allVideos.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                Icons.Default.Movie,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "No videos found",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
              )
            }
          }
        } else if (isFoldersMode && currentPath.isEmpty()) {
          // PC-Style Storage Drives View ("This PC" style)
          LazyColumn(
            contentPadding = PaddingValues(vertical = 12.dp),
            modifier = Modifier.fillMaxSize(),
          ) {
            item {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Devices and drives",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            items(visibleSubFolders, key = { "drive_" + it.fullPathList.joinToString("/") }) { folder ->
              DriveCardItem(
                folder = folder,
                onClick = { updatePath(folder.fullPathList) },
              )
            }
          }
        } else if (isGridView) {
          // Compact Adaptive Grid: ~105dp tiles, looks compact like app launcher icons!
          LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (visibleSubFolders.isNotEmpty()) 105.dp else 140.dp),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
          ) {
            // 1. Folders
            items(visibleSubFolders, key = { "folder_" + it.fullPathList.joinToString("/") }) { folder ->
              CompactFolderGridCard(
                folder = folder,
                onClick = { updatePath(folder.fullPathList) },
                onLongClick = {
                  selectedFolderForAction = folder
                  selectedVideoForAction = null
                  isActionDialogOpen = true
                },
              )
            }

            // 2. Videos
            items(visibleDirectVideos, key = { "vid_" + it.id }) { video ->
              CompactVideoGridCard(
                video = video,
                onClick = { playVideo(context, video) },
                onLongClick = {
                  selectedVideoForAction = video
                  selectedFolderForAction = null
                  isActionDialogOpen = true
                },
              )
            }
          }
        } else {
          // List Mode
          LazyColumn(
            contentPadding = PaddingValues(vertical = 6.dp),
            modifier = Modifier.fillMaxSize(),
          ) {
            // Folders first
            items(visibleSubFolders, key = { "folder_" + it.fullPathList.joinToString("/") }) { folder ->
              FolderRowItem(
                folder = folder,
                onClick = { updatePath(folder.fullPathList) },
                onLongClick = {
                  selectedFolderForAction = folder
                  selectedVideoForAction = null
                  isActionDialogOpen = true
                },
              )
            }

            // Videos
            items(visibleDirectVideos, key = { "vid_" + it.id }) { video ->
              VideoRowItem(
                video = video,
                onClick = { playVideo(context, video) },
                onLongClick = {
                  selectedVideoForAction = video
                  selectedFolderForAction = null
                  isActionDialogOpen = true
                },
              )
            }
          }
        }
      }

      if (isActionDialogOpen) {
        val itemName = selectedVideoForAction?.title ?: selectedFolderForAction?.name ?: ""
        val isFolder = selectedFolderForAction != null
        ItemActionDialog(
          title = itemName,
          isFolder = isFolder,
          onCopy = {
            if (selectedVideoForAction != null) {
              clipboardPath = selectedVideoForAction!!.path
              clipboardIsFolder = false
              clipboardName = selectedVideoForAction!!.title
            } else if (selectedFolderForAction != null) {
              val targetDir = resolveRealDirectory(selectedFolderForAction!!.fullPathList)
              clipboardPath = targetDir.absolutePath
              clipboardIsFolder = true
              clipboardName = selectedFolderForAction!!.name
            }
            Toast.makeText(context, "Copied: $clipboardName", Toast.LENGTH_SHORT).show()
            isActionDialogOpen = false
          },
          onDelete = {
            isActionDialogOpen = false
            isDeleteDialogOpen = true
          },
          onDismissRequest = { isActionDialogOpen = false },
        )
      }

      if (isDeleteDialogOpen) {
        val itemName = selectedVideoForAction?.title ?: selectedFolderForAction?.name ?: ""
        val itemPath = selectedVideoForAction?.path ?: resolveRealDirectory(selectedFolderForAction?.fullPathList ?: emptyList()).absolutePath

        DeleteConfirmDialog(
          title = itemName,
          onRecycleBin = {
            coroutineScope.launch(Dispatchers.IO) {
              try {
                val src = File(itemPath)
                if (src.exists()) {
                  val trashDir = File(Environment.getExternalStorageDirectory(), ".bplayer_trash")
                  if (!trashDir.exists()) trashDir.mkdirs()
                  val noMedia = File(trashDir, ".nomedia")
                  if (!noMedia.exists()) noMedia.createNewFile()

                  val dest = File(trashDir, "${System.currentTimeMillis()}_${src.name}")
                  val ok = src.renameTo(dest) || (src.copyTo(dest, overwrite = true).also { src.delete() }.exists())
                  if (ok && selectedVideoForAction != null) {
                    try {
                      context.contentResolver.delete(selectedVideoForAction!!.uri, null, null)
                    } catch (_: Exception) {}
                  }
                  MediaScannerConnection.scanFile(context, arrayOf(src.absolutePath, dest.absolutePath), null, null)
                }
              } catch (e: Exception) {
                e.printStackTrace()
              }
              withContext(Dispatchers.Main) {
                Toast.makeText(context, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                isDeleteDialogOpen = false
                selectedVideoForAction = null
                selectedFolderForAction = null
                scanMedia()
              }
            }
          },
          onPermanentDelete = {
            coroutineScope.launch(Dispatchers.IO) {
              try {
                val src = File(itemPath)
                if (src.exists()) {
                  if (src.isDirectory) {
                    src.deleteRecursively()
                  } else {
                    src.delete()
                  }
                }
                if (selectedVideoForAction != null) {
                  try {
                    context.contentResolver.delete(selectedVideoForAction!!.uri, null, null)
                  } catch (_: Exception) {}
                }
                MediaScannerConnection.scanFile(context, arrayOf(src.absolutePath), null, null)
              } catch (e: Exception) {
                e.printStackTrace()
              }
              withContext(Dispatchers.Main) {
                Toast.makeText(context, "Deleted permanently", Toast.LENGTH_SHORT).show()
                isDeleteDialogOpen = false
                selectedVideoForAction = null
                selectedFolderForAction = null
                scanMedia()
              }
            }
          },
          onDismissRequest = { isDeleteDialogOpen = false },
        )
      }
    }
  }

  fun playFile(filepath: String, context: Context) {
    val uri = if (filepath.startsWith("content://") || filepath.startsWith("file://")) {
      filepath.toUri()
    } else {
      Uri.fromFile(File(filepath))
    }
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
      setClass(context, PlayerActivity::class.java)
      putExtra("video_path", filepath)
      try {
        val parent = File(filepath).parent
        if (parent != null) putExtra("folder_path", parent)
      } catch (_: Exception) {}
    }
    context.startActivity(intent)
  }

  fun playFile(uri: Uri, context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
      setClass(context, PlayerActivity::class.java)
      putExtra("video_path", uri.toString())
    }
    context.startActivity(intent)
  }

  fun playVideo(context: Context, video: VideoItem) {
    val intent = Intent(Intent.ACTION_VIEW, video.uri).apply {
      setClass(context, PlayerActivity::class.java)
      putExtra("title", video.title)
      putExtra("video_path", video.path)
      try {
        val parent = File(video.path).parent
        if (parent != null) putExtra("folder_path", parent)
      } catch (_: Exception) {}

      // Auto-load external subtitles in folder
      try {
        val videoFile = File(video.path)
        val parentDir = videoFile.parentFile
        if (parentDir != null && parentDir.exists() && parentDir.isDirectory) {
          val baseName = videoFile.nameWithoutExtension.lowercase(Locale.ROOT)
          val subExts = setOf("srt", "ass", "ssa", "vtt", "sub")

          val searchDirs = mutableListOf(parentDir)
          File(parentDir, "subs").takeIf { it.exists() && it.isDirectory }?.let { searchDirs.add(it) }
          File(parentDir, "subtitles").takeIf { it.exists() && it.isDirectory }?.let { searchDirs.add(it) }
          File(parentDir, "Subs").takeIf { it.exists() && it.isDirectory }?.let { searchDirs.add(it) }

          val foundSubs = mutableListOf<File>()
          for (dir in searchDirs) {
            dir.listFiles()?.filter { f ->
              f.isFile && f.extension.lowercase(Locale.ROOT) in subExts
            }?.let { files ->
              for (f in files) {
                if (f.nameWithoutExtension.lowercase(Locale.ROOT).startsWith(baseName) || files.size <= 5) {
                  foundSubs.add(f)
                }
              }
            }
          }

          if (foundSubs.isNotEmpty()) {
            val subUris = foundSubs.distinctBy { it.absolutePath }.map { Uri.fromFile(it) }.toTypedArray()
            putExtra("subs", subUris)
            putExtra("subs.enable", arrayOf(subUris.first()))
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
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

          val lower = path.lowercase(Locale.ROOT)
          if (lower.contains("/. ") || lower.contains("/android/data/") || lower.contains("/cache/")) {
            continue
          }

          val uri = ContentUris.withAppendedId(collection, id)
          val resolution = if (width >= 3840 || height >= 2160) "4K" else if (width >= 1280 || height >= 720) "HD" else "SD"
          val segments = getRelativeDirSegments(path)

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
              segments = segments,
            ),
          )
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return list
  }

  private fun resolveRealDirectory(pathSegments: List<String>): File {
    if (pathSegments.isEmpty()) return Environment.getExternalStorageDirectory()
    val root = pathSegments.first()
    val subSegments = pathSegments.drop(1)
    val baseDir = if (root == "Internal Storage") {
      Environment.getExternalStorageDirectory()
    } else {
      val storage = File("/storage")
      val sdRoot = storage.listFiles()?.firstOrNull {
        it.isDirectory && it.canRead() && !it.name.equals("emulated", true) && !it.name.equals("self", true)
      } ?: Environment.getExternalStorageDirectory()
      sdRoot
    }
    var target = baseDir
    for (seg in subSegments) {
      target = File(target, seg)
    }
    return target
  }

  private fun getRelativeDirSegments(filePath: String): List<String> {
    val file = File(filePath)
    val parent = file.parentFile ?: return emptyList()
    val parentPath = parent.absolutePath.replace('\\', '/')

    val (storageRoot, relPath) = when {
      parentPath.startsWith("/storage/emulated/0") -> {
        Pair("Internal Storage", parentPath.removePrefix("/storage/emulated/0").trim('/'))
      }
      parentPath.startsWith("/sdcard") -> {
        Pair("Internal Storage", parentPath.removePrefix("/sdcard").trim('/'))
      }
      parentPath.startsWith("/storage/") -> {
        val afterStorage = parentPath.removePrefix("/storage/")
        val slashIdx = afterStorage.indexOf('/')
        if (slashIdx >= 0) {
          Pair("SD Card", afterStorage.substring(slashIdx).trim('/'))
        } else {
          Pair("SD Card", "")
        }
      }
      else -> Pair("Internal Storage", parentPath.trim('/'))
    }

    val segments = mutableListOf(storageRoot)
    if (relPath.isNotEmpty()) {
      segments.addAll(relPath.split('/').filter { it.isNotBlank() })
    }
    return segments
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderRowItem(
  folder: SubFolderInfo,
  onClick: () -> Unit,
  onLongClick: () -> Unit = {},
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .combinedClickable(onClick = onClick, onLongClick = onLongClick)
      .padding(horizontal = 20.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val folderTint = when (folder.name) {
      "Internal Storage" -> Color(0xFF00E5FF)
      "SD Card" -> Color(0xFF00E676)
      else -> Color(0xFFFFB74D)
    }
    Box(
      contentAlignment = Alignment.TopEnd,
    ) {
      Icon(
        Icons.Default.Folder,
        contentDescription = null,
        modifier = Modifier.size(38.dp),
        tint = folderTint,
      )
      if (folder.videoCount > 0) {
        Box(
          modifier = Modifier
            .offset(x = 4.dp, y = (-3).dp)
            .size(16.dp)
            .clip(CircleShape)
            .background(Color(0xFFFF2A2A)),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (folder.videoCount > 99) "99+" else folder.videoCount.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            lineHeight = 8.sp,
            textAlign = TextAlign.Center,
          )
        }
      }
    }

    Spacer(modifier = Modifier.width(16.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = folder.name,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "${folder.videoCount} videos",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Icon(
      Icons.Default.ChevronRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
      modifier = Modifier.size(20.dp),
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactFolderGridCard(
  folder: SubFolderInfo,
  onClick: () -> Unit,
  onLongClick: () -> Unit = {},
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .height(98.dp)
      .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      val folderTint = when (folder.name) {
        "Internal Storage" -> Color(0xFF00E5FF)
        "SD Card" -> Color(0xFF00E676)
        else -> Color(0xFFFFB74D)
      }
      Box(
        contentAlignment = Alignment.TopEnd,
        modifier = Modifier.padding(bottom = 4.dp),
      ) {
        Icon(
          Icons.Default.Folder,
          contentDescription = null,
          modifier = Modifier.size(34.dp),
          tint = folderTint,
        )
        if (folder.videoCount > 0) {
          Box(
            modifier = Modifier
              .offset(x = 4.dp, y = (-3).dp)
              .size(16.dp)
              .clip(CircleShape)
              .background(Color(0xFFFF2A2A)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (folder.videoCount > 99) "99+" else folder.videoCount.toString(),
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 8.sp,
              lineHeight = 8.sp,
              textAlign = TextAlign.Center,
            )
          }
        }
      }

      Text(
        text = folder.name,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
fun rememberVideoThumbnail(uri: Uri, width: Int, height: Int): ImageBitmap? {
  val context = LocalContext.current
  var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
  LaunchedEffect(uri) {
    withContext(Dispatchers.IO) {
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          val bm = context.contentResolver.loadThumbnail(uri, Size(width, height), null)
          bitmap = bm.asImageBitmap()
        }
      } catch (_: Exception) {}
    }
  }
  return bitmap
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoRowItem(
  video: VideoItem,
  onClick: () -> Unit,
  onLongClick: () -> Unit = {},
) {
  val thumbnail = rememberVideoThumbnail(video.uri, 180, 110)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .combinedClickable(onClick = onClick, onLongClick = onLongClick)
      .padding(horizontal = 20.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(76.dp, 48.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(Color(0xFF1E2430)),
      contentAlignment = Alignment.Center,
    ) {
      if (thumbnail != null) {
        Image(
          bitmap = thumbnail,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Icon(
          Icons.Default.PlayArrow,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
          modifier = Modifier.size(22.dp),
        )
      }
      Surface(
        shape = RoundedCornerShape(2.dp),
        color = Color(0xCC000000),
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(2.dp),
      ) {
        Text(
          text = video.formatDuration(),
          color = Color.White,
          fontSize = 8.sp,
          fontWeight = FontWeight.Medium,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
        )
      }
    }

    Spacer(modifier = Modifier.width(16.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = video.title,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          shape = RoundedCornerShape(3.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        ) {
          Text(
            text = video.resolution,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactVideoGridCard(
  video: VideoItem,
  onClick: () -> Unit,
  onLongClick: () -> Unit = {},
) {
  val thumbnail = rememberVideoThumbnail(video.uri, 280, 160)
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .height(130.dp)
      .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(76.dp)
          .background(Color(0xFF1E2430)),
        contentAlignment = Alignment.Center,
      ) {
        if (thumbnail != null) {
          Image(
            bitmap = thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Icon(
            Icons.Default.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(28.dp),
          )
        }
        Surface(
          shape = RoundedCornerShape(2.dp),
          color = Color(0xCC000000),
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(3.dp),
        ) {
          Text(
            text = video.formatDuration(),
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
          )
        }
      }

      Column(modifier = Modifier.padding(6.dp)) {
        Text(
          text = video.title,
          fontWeight = FontWeight.Medium,
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = video.resolution,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = video.formatSize(),
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
fun DriveCardItem(
  folder: SubFolderInfo,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isSdCard = folder.name.equals("SD Card", ignoreCase = true)
  val driveLetter = if (isSdCard) "D:" else "C:"
  val driveLabel = if (isSdCard) "SD Card ($driveLetter)" else "Internal Storage ($driveLetter)"
  val driveTint = if (isSdCard) Color(0xFF00E676) else Color(0xFF00E5FF)
  val borderColor = if (isSdCard) Color(0x5500E676) else Color(0x5500E5FF)

  val (storageText, progress) = remember(folder.name) {
    try {
      val path = if (!isSdCard) {
        Environment.getDataDirectory().path
      } else {
        val storage = File("/storage")
        val sd = storage.listFiles()?.firstOrNull {
          it.isDirectory && it.canRead() && !it.name.equals("emulated", true) && !it.name.equals("self", true)
        }
        sd?.path ?: Environment.getExternalStorageDirectory().path
      }
      val stat = StatFs(path)
      val totalBytes = stat.totalBytes
      val freeBytes = stat.availableBytes
      val usedBytes = totalBytes - freeBytes
      val prog = if (totalBytes > 0) (usedBytes.toDouble() / totalBytes.toDouble()).toFloat() else 0.5f
      val freeGb = freeBytes / (1024.0 * 1024.0 * 1024.0)
      val totalGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
      Pair(String.format(Locale.US, "%.1f GB free of %.1f GB", freeGb, totalGb), prog.coerceIn(0f, 1f))
    } catch (_: Exception) {
      Pair("${folder.videoCount} videos", 0.5f)
    }
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = Color(0xDD0D1420),
    ),
    border = BorderStroke(1.5.dp, borderColor),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(52.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(driveTint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          if (isSdCard) Icons.Default.SdStorage else Icons.Default.Storage,
          contentDescription = null,
          modifier = Modifier.size(32.dp),
          tint = driveTint,
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = driveLabel,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
          )
          Text(
            text = "${folder.videoCount} videos",
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = driveTint,
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = driveTint,
          trackColor = Color(0x33FFFFFF),
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (storageText.isNotBlank()) {
          Text(
            text = storageText,
            fontSize = 11.sp,
            color = Color(0xFF8A99AD),
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Icon(
        Icons.Default.ChevronRight,
        contentDescription = null,
        tint = Color(0xFF8A99AD),
        modifier = Modifier.size(24.dp),
      )
    }
  }
}

@Composable
fun ItemActionDialog(
  title: String,
  isFolder: Boolean,
  onCopy: () -> Unit,
  onDelete: () -> Unit,
  onDismissRequest: () -> Unit,
) {
  Dialog(onDismissRequest = onDismissRequest) {
    Card(
      modifier = Modifier
        .widthIn(min = 280.dp, max = 400.dp)
        .padding(16.dp),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = Color(0xDD0D1420),
      ),
      border = BorderStroke(1.dp, Color(0x3300E5FF)),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              if (isFolder) Icons.Default.Folder else Icons.Default.Movie,
              contentDescription = null,
              tint = Color(0xFF00E5FF),
              modifier = Modifier.size(22.dp),
            )
            Text(
              text = title,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          IconButton(onClick = onDismissRequest, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF8A99AD), modifier = Modifier.size(20.dp))
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0x22FFFFFF))

        // Copy option
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onCopy)
            .padding(vertical = 10.dp, horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
          Text("Copy", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        // Delete option
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onDelete)
            .padding(vertical = 10.dp, horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
          Text("Delete", color = Color(0xFFFF5252), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
      }
    }
  }
}

@Composable
fun DeleteConfirmDialog(
  title: String,
  onRecycleBin: () -> Unit,
  onPermanentDelete: () -> Unit,
  onDismissRequest: () -> Unit,
) {
  Dialog(onDismissRequest = onDismissRequest) {
    Card(
      modifier = Modifier
        .widthIn(min = 300.dp, max = 450.dp)
        .padding(16.dp),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = Color(0xDD0D1420),
      ),
      border = BorderStroke(1.dp, Color(0x3300E5FF)),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(24.dp))
          Text(
            text = "Delete",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Color.White,
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "\"$title\"",
          fontWeight = FontWeight.SemiBold,
          fontSize = 14.sp,
          color = Color(0xFF00E5FF),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Do you want to move this file to the Recycle Bin or delete it permanently?",
          fontSize = 13.sp,
          color = Color(0xFFB0BEC5),
          lineHeight = 18.sp,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color(0x22FFFFFF))

        // Options
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Recycle Bin
          Button(
            onClick = onRecycleBin,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0x3300E5FF),
            ),
            border = BorderStroke(1.dp, Color(0xFF00E5FF)),
            shape = RoundedCornerShape(10.dp),
          ) {
            Icon(Icons.Default.RestoreFromTrash, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Move to Recycle Bin", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
          }

          // Permanent Delete
          Button(
            onClick = onPermanentDelete,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0x33FF5252),
            ),
            border = BorderStroke(1.dp, Color(0xFFFF5252)),
            shape = RoundedCornerShape(10.dp),
          ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete Permanently", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
          }

          // Cancel
          TextButton(
            onClick = onDismissRequest,
            modifier = Modifier.align(Alignment.End),
          ) {
            Text("Cancel", color = Color(0xFF8A99AD))
          }
        }
      }
    }
  }
}
