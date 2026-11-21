package com.zenox.remotegallery.ui.screens.gallery

import android.os.Build
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.zenox.remotegallery.core.AppContainer
import com.zenox.remotegallery.data.local.db.MediaItemEntity
import com.zenox.remotegallery.ui.AppViewModelFactory
import com.zenox.remotegallery.ui.GalleryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(appContainer: AppContainer) {
    val vm: GalleryViewModel = viewModel(factory = AppViewModelFactory(appContainer))
    val query by vm.query.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val all by vm.media.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf<MediaItemEntity?>(null) }

    val filtered = remember(all, query, filter) {
        all.asSequence()
            .filter { item -> if (filter == "ALL") true else item.mediaType == filter }
            .filter { item ->
                val s = query.trim().lowercase()
                if (s.isBlank()) true else (item.fileName?.lowercase()?.contains(s) == true || item.mimeType?.lowercase()?.contains(s) == true)
            }
            .sortedByDescending { item -> item.dateModifiedSec ?: item.dateAddedSec ?: 0L }
            .toList()
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Gallery", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(query, { vm.setQuery(it) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search by name/type") }, leadingIcon = { Icon(Icons.Default.Search, null) })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == "ALL", onClick = { vm.setFilter("ALL") }, label = { Text("All") })
            FilterChip(selected = filter == "IMAGE", onClick = { vm.setFilter("IMAGE") }, label = { Text("Images") })
            FilterChip(selected = filter == "VIDEO", onClick = { vm.setFilter("VIDEO") }, label = { Text("Videos") })
        }
        Text("${filtered.size} accessible items indexed on this device", style = MaterialTheme.typography.bodySmall)
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No accessible media found. Grant permissions and sync.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 110.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { item -> "${item.mediaType}:${item.mediaStoreId}" }) { item ->
                    MediaGridItem(item = item, onClick = { selected = item })
                }
            }
        }
    }

    if (selected != null) {
        val item = selected!!
        AlertDialog(
            onDismissRequest = { selected = null },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } },
            title = { Text(item.fileName ?: "Media details", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AsyncImage(
                        model = ImageRequest.Builder(ctx).data(item.contentUri).apply { if (item.mediaType == "VIDEO") videoFrameMillis(1000) }.crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Detail("Type", item.mediaType)
                    Detail("MIME", item.mimeType ?: "-")
                    Detail("Size", item.sizeBytes?.let { Formatter.formatShortFileSize(ctx, it) } ?: "-")
                    Detail("Modified", formatDate(item.dateModifiedSec))
                    Detail("Added", formatDate(item.dateAddedSec))
                    Detail("Dimensions", if (item.width != null && item.height != null) "${item.width} x ${item.height}" else "-")
                    if (item.mediaType == "VIDEO") Detail("Duration", item.durationMs?.let { "${it / 1000}s" } ?: "-")
                    Detail("MediaStore ID", item.mediaStoreId.toString())
                    Detail("URI", item.contentUri)
                    Detail("Status", if (item.deletedOnDevice) "Deleted" else "Accessible")
                }
            }
        )
    }
}

@Composable
private fun MediaGridItem(item: MediaItemEntity, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Card(Modifier.aspectRatio(1f).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(ctx).data(item.contentUri).apply { if (item.mediaType == "VIDEO") videoFrameMillis(800) }.crossfade(true).build(),
                contentDescription = item.fileName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (item.mediaType == "VIDEO") {
                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.align(Alignment.Center).size(28.dp))
            }
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)).padding(4.dp)) {
                Text(item.fileName ?: "Unknown", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}

@Composable private fun Detail(k: String, v: String) { Row { Text("$k: ", style = MaterialTheme.typography.labelLarge); Text(v, style = MaterialTheme.typography.bodySmall) } }
private fun formatDate(sec: Long?): String = sec?.let { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it * 1000)) } ?: "-"
