package com.suibari.skyputter.ui.draft

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suibari.skyputter.util.DraftData
import com.suibari.skyputter.util.DraftViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftScreen(
    draftViewModel: DraftViewModel,
    onBack: () -> Unit,
    onDraftSelected: (String, String) -> Unit // text, draftId
) {
    var drafts by remember { mutableStateOf(draftViewModel.getDrafts()) }
    var showDeleteDialog by remember { mutableStateOf<DraftData?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("下書き") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (drafts.isEmpty()) {
                // 下書きがない場合
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "下書きがありません",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                // 下書き一覧
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(drafts) { draft ->
                        SwipeToDeleteItem(
                            draft = draft,
                            onDraftClick = {
                                onDraftSelected(draft.text, draft.id)
                                onBack()
                            },
                            onDeleteClick = { showDeleteDialog = draft }
                        )
                    }
                }
            }
        }
    }

    // 削除確認ダイアログ
    showDeleteDialog?.let { draft ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("下書きを削除") },
            text = { Text("この下書きを削除しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        draftViewModel.deleteDraft(draft.id)
                        drafts = draftViewModel.getDrafts()
                        showDeleteDialog = null
                    }
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun SwipeToDeleteItem(
    draft: DraftData,
    onDraftClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val density = LocalDensity.current
    var offsetX by remember { mutableStateOf(0f) }
    val maxSwipeDistance = with(density) { 80.dp.toPx() }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 背景の削除ボタン
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.Red),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "削除",
                tint = Color.White,
                modifier = Modifier.padding(end = 24.dp)
            )
        }

        // メインカード
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < -maxSwipeDistance / 2) {
                                onDeleteClick()
                            }
                            offsetX = 0f
                        }
                    ) { _, dragAmount ->
                        val newOffset = offsetX + dragAmount
                        offsetX = newOffset.coerceIn(-maxSwipeDistance, 0f)
                    }
                }
                .clickable { onDraftClick() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = draft.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                        .format(draft.createdAt),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}