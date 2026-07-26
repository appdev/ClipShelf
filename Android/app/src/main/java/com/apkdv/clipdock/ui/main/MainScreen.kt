package com.apkdv.clipdock.ui.main

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed as staggeredItemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apkdv.clipdock.MainDestination
import com.apkdv.clipdock.SettingsDetailDestination
import com.apkdv.clipdock.data.ClipDockUiState
import com.apkdv.clipdock.data.ClipHistoryItem
import com.apkdv.clipdock.data.ClipItemType
import com.apkdv.clipdock.data.OverlayClickAction
import com.apkdv.clipdock.data.OverlaySnapEdge
import com.apkdv.clipdock.data.PayloadState
import com.apkdv.clipdock.data.P2pDeviceInfo
import com.apkdv.clipdock.data.TransferState
import com.apkdv.clipdock.overlay.FloatingOverlayService
import com.apkdv.clipdock.ui.components.ActionChip
import com.apkdv.clipdock.ui.components.BottomNavItem
import com.apkdv.clipdock.ui.components.ClipDockBottomNav
import com.apkdv.clipdock.ui.components.ClipDockCard
import com.apkdv.clipdock.ui.components.ClipDockHeroBanner
import com.apkdv.clipdock.ui.components.ClipDockIconButton
import com.apkdv.clipdock.ui.components.ClipDockIconKind
import com.apkdv.clipdock.ui.components.ClipDockScreenHeader
import com.apkdv.clipdock.ui.components.ClipDockSymbol
import com.apkdv.clipdock.ui.components.ClipDockTone
import com.apkdv.clipdock.ui.components.IconTile
import com.apkdv.clipdock.ui.components.RowCard
import com.apkdv.clipdock.ui.components.SegmentedControl
import com.apkdv.clipdock.ui.components.SettingDivider
import com.apkdv.clipdock.ui.components.SettingGroup
import com.apkdv.clipdock.ui.components.SettingRow
import com.apkdv.clipdock.ui.components.SliderSettingCard
import com.apkdv.clipdock.ui.components.StatusPill
import com.apkdv.clipdock.ui.components.SwitchSettingRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.apkdv.clipdock.theme.ClipTheme
import com.apkdv.clipdock.ui.components.ClipBottomBar
import com.apkdv.clipdock.ui.components.ClipTab
import com.apkdv.clipdock.ui.components.CodeClipCard
import com.apkdv.clipdock.ui.components.ColorClipCard
import com.apkdv.clipdock.ui.components.FileClipCard
import com.apkdv.clipdock.ui.components.ImageClipCard
import com.apkdv.clipdock.ui.components.LinkClipCard
import com.apkdv.clipdock.ui.components.PairingCodeInput
import com.apkdv.clipdock.ui.components.SettingsGroup
import com.apkdv.clipdock.ui.components.SettingsToggleRow
import com.apkdv.clipdock.ui.components.SettingsValueRow
import com.apkdv.clipdock.ui.components.TextClipCard

@Composable
fun MainScreen(
  selectedDestination: MainDestination = MainDestination.History,
  settingsDetail: SettingsDetailDestination? = null,
  itemDetailStableId: String? = null,
  initialDetailSheet: MobileV4InitialSheet? = null,
  onDestinationSelected: (MainDestination) -> Unit = {},
  onOpenSettingsDetail: (SettingsDetailDestination) -> Unit = {},
  onOpenItemDetail: (String) -> Unit = {},
  onBackFromDetail: () -> Unit = {},
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val actionState by viewModel.v4ActionState.collectAsStateWithLifecycle()
  ClipDockApp(
    state = state,
    selectedDestination = selectedDestination,
    settingsDetail = settingsDetail,
    itemDetailStableId = itemDetailStableId,
    initialDetailSheet = initialDetailSheet,
    actionState = actionState,
    onDestinationSelected = onDestinationSelected,
    onBackFromDetail = onBackFromDetail,
    onServerUrlChange = viewModel::setServerUrl,
    onDeviceNameChange = viewModel::setDeviceName,
    onSyncNow = viewModel::syncNow,
    onCheckHealth = viewModel::checkHealth,
    onCreateSyncSpace = viewModel::createSyncSpace,
    onJoinSyncSpace = viewModel::joinSyncSpace,
    onCreateInvite = viewModel::createInvite,
    onRefreshInfo = viewModel::refreshInfo,
    onUseItem = viewModel::useItem,
    onCopyItem = viewModel::copyItem,
    onDownloadToCache = viewModel::downloadToCache,
    onCopyThumbnail = viewModel::copyThumbnail,
    onDeleteSyncRecord = viewModel::deleteSyncRecord,
    onRemoveLocalCache = viewModel::removeLocalCache,
    onP2pEnabledChange = viewModel::setP2pEnabled,
    onWifiOnlyChange = viewModel::setWifiOnly,
    onOverlayEnabledChange = viewModel::setOverlayEnabled,
    onOverlayClickActionChange = viewModel::setOverlayClickAction,
    onOverlaySnapEdgeChange = viewModel::setOverlaySnapEdge,
    onOverlaySizeChange = viewModel::setOverlaySizeDp,
    onOverlayIdleOpacityChange = viewModel::setOverlayIdleOpacityPercent,
    onOverlayVerticalFractionChange = viewModel::setOverlayVerticalFraction,
    onEncryptionEnabledChange = viewModel::setEncryptionEnabled,
    onOpenSettingsDetail = onOpenSettingsDetail,
    onOpenItemDetail = onOpenItemDetail,
    modifier = modifier.fillMaxSize(),
  )
}

@Composable
internal fun ClipDockApp(
  state: ClipDockUiState,
  selectedDestination: MainDestination,
  settingsDetail: SettingsDetailDestination?,
  itemDetailStableId: String?,
  initialDetailSheet: MobileV4InitialSheet?,
  actionState: MobileV4ActionState,
  onDestinationSelected: (MainDestination) -> Unit,
  onBackFromDetail: () -> Unit,
  onServerUrlChange: (String) -> Unit,
  onDeviceNameChange: (String) -> Unit,
  onSyncNow: () -> Unit,
  onCheckHealth: () -> Unit,
  onCreateSyncSpace: () -> Unit,
  onJoinSyncSpace: (String) -> Unit,
  onCreateInvite: () -> Unit,
  onRefreshInfo: () -> Unit,
  onUseItem: (ClipHistoryItem) -> Unit,
  onCopyItem: (ClipHistoryItem) -> Unit,
  onDownloadToCache: (ClipHistoryItem) -> Unit,
  onCopyThumbnail: (ClipHistoryItem) -> Unit,
  onDeleteSyncRecord: (ClipHistoryItem) -> Unit,
  onRemoveLocalCache: (ClipHistoryItem) -> Unit,
  onP2pEnabledChange: (Boolean) -> Unit,
  onWifiOnlyChange: (Boolean) -> Unit,
  onOverlayEnabledChange: (Boolean) -> Unit,
  onOverlayClickActionChange: (OverlayClickAction) -> Unit,
  onOverlaySnapEdgeChange: (OverlaySnapEdge) -> Unit,
  onOverlaySizeChange: (Int) -> Unit,
  onOverlayIdleOpacityChange: (Int) -> Unit,
  onOverlayVerticalFractionChange: (Float) -> Unit,
  onEncryptionEnabledChange: (Boolean) -> Unit,
  onOpenSettingsDetail: (SettingsDetailDestination) -> Unit,
  onOpenItemDetail: (String) -> Unit,
  modifier: Modifier = Modifier,
  includeReferenceStatusBar: Boolean = true,
) {
  val context = LocalContext.current
  val wifiOnlyBlocked = state.wifiOnly && !isWifiConnected(context)

  val clipColors = ClipTheme.colors
  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = clipColors.background,
    contentWindowInsets = if (includeReferenceStatusBar) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
    bottomBar = {
      if (itemDetailStableId == null) {
        val currentTab = when (selectedDestination) {
          MainDestination.History -> ClipTab.RECENTS
          MainDestination.Files -> ClipTab.PINBOARD
          MainDestination.Devices -> ClipTab.DEVICES
          MainDestination.Settings -> ClipTab.SETTINGS
        }
        ClipBottomBar(
          current = currentTab,
          onSelect = { tab ->
            val dest = when (tab) {
              ClipTab.RECENTS -> MainDestination.History
              ClipTab.PINBOARD -> MainDestination.Files
              ClipTab.DEVICES -> MainDestination.Devices
              ClipTab.SETTINGS -> MainDestination.Settings
            }
            onDestinationSelected(dest)
          },
        )
      }
    },
  ) { innerPadding ->
    Column(
      Modifier
        .padding(innerPadding)
        .fillMaxSize()
        .background(clipColors.background),
    ) {
      if (includeReferenceStatusBar) {
        ReferenceStatusBar()
      }
      if (state.isSyncing || state.isSyncSetupInFlight) {
        LinearProgressIndicator(Modifier.fillMaxWidth(), color = clipColors.coral)
      }
      state.diagnostics.lastError?.let { FeedbackBanner(it, isError = true) }
      when {
        itemDetailStableId != null ->
          ItemDetailPage(
            item = state.items.firstOrNull { it.stableId == itemDetailStableId },
            state = state,
            actionState = actionState,
            wifiOnlyBlocked = wifiOnlyBlocked,
            initialSheet = initialDetailSheet,
            onBack = onBackFromDetail,
            onCopyItem = onCopyItem,
            onDownloadToCache = onDownloadToCache,
            onCopyThumbnail = onCopyThumbnail,
            onDeleteSyncRecord = onDeleteSyncRecord,
            onRemoveLocalCache = onRemoveLocalCache,
          )
        settingsDetail == SettingsDetailDestination.KeepAlive ->
          KeepAlivePage(state = state, onBack = onBackFromDetail)
        settingsDetail == SettingsDetailDestination.FloatingBall ->
          FloatingBallSettingsPage(
            state = state,
            onBack = onBackFromDetail,
            onOverlayEnabledChange = onOverlayEnabledChange,
            onOverlayClickActionChange = onOverlayClickActionChange,
            onOverlaySnapEdgeChange = onOverlaySnapEdgeChange,
            onOverlaySizeChange = onOverlaySizeChange,
            onOverlayIdleOpacityChange = onOverlayIdleOpacityChange,
            onOverlayVerticalFractionChange = onOverlayVerticalFractionChange,
          )
        settingsDetail == SettingsDetailDestination.Pairing ->
          PairingPage(
            state = state,
            onBack = onBackFromDetail,
            onDeviceNameChange = onDeviceNameChange,
            onCreateSyncSpace = onCreateSyncSpace,
            onJoinSyncSpace = onJoinSyncSpace,
            onCreateInvite = onCreateInvite,
          )
        settingsDetail == SettingsDetailDestination.ServerAdvanced ->
          ServerAdvancedPage(
            state = state,
            onBack = onBackFromDetail,
            onServerUrlChange = onServerUrlChange,
            onCheckHealth = onCheckHealth,
            onRefreshInfo = onRefreshInfo,
            onSyncNow = onSyncNow,
          )
        selectedDestination == MainDestination.History ->
          HistoryPage(
            state = state,
            onOpenSettings = { onDestinationSelected(MainDestination.Settings) },
            onSyncNow = onSyncNow,
            onOpenItemDetail = onOpenItemDetail,
            onDownloadToCache = onDownloadToCache,
          )
        selectedDestination == MainDestination.Devices ->
          DevicesPage(
            state = state,
            onCreateInvite = onCreateInvite,
            onRefresh = onRefreshInfo,
          )
        selectedDestination == MainDestination.Files ->
          FilesPage(
            state = state,
            wifiOnlyBlocked = wifiOnlyBlocked,
            onUseItem = onUseItem,
            onOpenItemDetail = onOpenItemDetail,
          )
        selectedDestination == MainDestination.Settings ->
          SettingsOverviewPage(
            state = state,
            onSyncNow = onSyncNow,
            onP2pEnabledChange = onP2pEnabledChange,
            onWifiOnlyChange = onWifiOnlyChange,
            onOverlayEnabledChange = onOverlayEnabledChange,
            onEncryptionEnabledChange = onEncryptionEnabledChange,
            onServerUrlChange = onServerUrlChange,
            onCheckHealth = onCheckHealth,
            onCreateSyncSpace = onCreateSyncSpace,
            onJoinSyncSpace = onJoinSyncSpace,
            onOpenSettingsDetail = onOpenSettingsDetail,
          )
      }
    }
  }
}

@Composable
private fun ReferenceStatusBar() {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .height(22.dp)
        .padding(start = 18.dp, end = 18.dp, top = 4.dp),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text("22:44", color = ClipTheme.colors.ink, fontSize = 13.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold)
    Text("5G 100%", color = ClipTheme.colors.ink, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailPage(
  item: ClipHistoryItem?,
  state: ClipDockUiState,
  actionState: MobileV4ActionState,
  wifiOnlyBlocked: Boolean,
  initialSheet: MobileV4InitialSheet?,
  onBack: () -> Unit,
  onCopyItem: (ClipHistoryItem) -> Unit,
  onDownloadToCache: (ClipHistoryItem) -> Unit,
  onCopyThumbnail: (ClipHistoryItem) -> Unit,
  onDeleteSyncRecord: (ClipHistoryItem) -> Unit,
  onRemoveLocalCache: (ClipHistoryItem) -> Unit,
) {
  var showRemoteSheet by remember { mutableStateOf(initialSheet == MobileV4InitialSheet.RemoteRetrieval) }
  var showDeleteConfirm by remember { mutableStateOf(initialSheet == MobileV4InitialSheet.DeleteConfirm) }

  LaunchedEffect(item?.stableId) {
    if (item == null) {
      onBack()
    }
  }
  BackHandler {
    when {
      showDeleteConfirm -> showDeleteConfirm = false
      showRemoteSheet -> showRemoteSheet = false
      else -> onBack()
    }
  }
  if (item == null) {
    EmptyState("记录已删除", "这条同步记录已经从当前历史中移除。", null, null)
    return
  }

  val actions =
    mobileV4DetailActions(
      item = item,
      p2pEnabled = state.p2pEnabled,
      wifiOnlyBlocked = wifiOnlyBlocked,
      inFlightKinds = actionState.inFlightKinds(item.stableId),
    )
  Box(Modifier.fillMaxSize().testTag(MobileV4Tags.ItemDetailScreen)) {
    if (item.type == ClipItemType.Image) {
      ImageDetailPhotoPage(
        item = item,
        state = state,
        actions = actions,
        onBack = onBack,
        onCopyItem = onCopyItem,
        onDownloadToCache = onDownloadToCache,
        onCopyThumbnail = onCopyThumbnail,
        onRemoveLocalCache = onRemoveLocalCache,
        onDelete = { showDeleteConfirm = true },
      )
    } else {
      LazyColumn(
        contentPadding = PaddingValues(start = 14.dp, top = 15.dp, end = 14.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        item {
          ItemDetailTopBar(item = item, onBack = onBack)
        }
        item {
          ItemDetailSummary(item = item)
        }
        item {
          ItemDetailContentPreview(item = item)
        }
        item {
          ItemDetailInlineActions(
            actions = actions,
            onPrimary = {
              when (actions.primary.kind) {
                MobileV4ActionKind.Copy -> onCopyItem(item)
                MobileV4ActionKind.ShowRemoteRetrieval -> onDownloadToCache(item)
                else -> Unit
              }
            },
            onDelete = { showDeleteConfirm = true },
          )
        }
        item {
          ItemDetailMetaGrid(item = item, state = state)
        }
      }
    }
    if (showRemoteSheet) {
      V4SheetOverlay(testTag = MobileV4Tags.RemoteRetrievalSheet, withGrabber = true) {
        RemoteRetrievalSheetContent(
          item = item,
          actions = actions,
          onDownloadToCache = {
            showRemoteSheet = false
            onDownloadToCache(item)
          },
          onCopyThumbnail = {
            showRemoteSheet = false
            onCopyThumbnail(item)
          },
        )
      }
    }
    if (showDeleteConfirm) {
      V4SheetOverlay(testTag = MobileV4Tags.DeleteConfirmSheet, withGrabber = false) {
        DeleteConfirmSheetContent(
          actions = actions,
          onRemoveLocalCache = {
            showDeleteConfirm = false
            onRemoveLocalCache(item)
          },
          onDeleteSyncRecord = {
            showDeleteConfirm = false
            onBack()
            onDeleteSyncRecord(item)
          },
          onCancel = { showDeleteConfirm = false },
        )
      }
    }
  }
}

@Composable
private fun ImageDetailPhotoPage(
  item: ClipHistoryItem,
  state: ClipDockUiState,
  actions: MobileV4DetailActions,
  onBack: () -> Unit,
  onCopyItem: (ClipHistoryItem) -> Unit,
  onDownloadToCache: (ClipHistoryItem) -> Unit,
  onCopyThumbnail: (ClipHistoryItem) -> Unit,
  onRemoveLocalCache: (ClipHistoryItem) -> Unit,
  onDelete: () -> Unit,
) {
  LazyColumn(
    contentPadding = PaddingValues(start = 14.dp, top = 15.dp, end = 14.dp, bottom = 18.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxSize().background(ClipTheme.colors.background),
  ) {
    item { ItemDetailTopBar(item = item, onBack = onBack) }
    item {
      val isRetrieving = actions.primary.kind == MobileV4ActionKind.ShowRemoteRetrieval && !actions.primary.enabled && actions.primary.label == "取回中"
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(ClipTheme.colors.sunken),
      ) {
        val bitmap by rememberImageBitmap(item.thumbnailUri ?: item.localUri)
        if (bitmap != null) {
          Image(bitmap = bitmap!!, contentDescription = item.displayTitle, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
          HistoryMediaFallback(HistoryCardVariant.Image, Modifier.fillMaxSize())
        }
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color(0xB80F172A)))))
        if (isRetrieving) {
          LinearProgressIndicator(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
            color = ClipTheme.colors.accentLink.fg,
            trackColor = Color.White.copy(alpha = 0.18f),
          )
        }
        Row(
          modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          ClipDockSymbol(ClipDockIconKind.Image, Modifier.size(16.dp), color = Color.White)
          Text("图片预览", color = Color.White, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
      }
    }
    item {
      ItemDetailInlineActions(
        actions = actions,
        onPrimary = {
          when (actions.primary.kind) {
            MobileV4ActionKind.Copy -> onCopyItem(item)
            MobileV4ActionKind.ShowRemoteRetrieval -> onDownloadToCache(item)
            else -> Unit
          }
        },
        onDelete = onDelete,
      )
    }
    item {
      ImageDetailInfoList(item = item, state = state)
    }
  }
}

@Composable
private fun ImageDetailPhotoCanvas(
  item: ClipHistoryItem,
  modifier: Modifier = Modifier,
) {
  val c = ClipTheme.colors
  val localBitmap by rememberImageBitmap(item.localUri)
  val thumbnailBitmap by rememberImageBitmap(item.thumbnailUri)
  val displayBitmap = localBitmap ?: thumbnailBitmap
  val showingOriginal = localBitmap != null && item.payloadState == PayloadState.Ready && !item.localUri.isNullOrBlank()
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    if (displayBitmap != null) {
      Image(
        bitmap = displayBitmap,
        contentDescription = item.displayTitle,
        contentScale = ContentScale.Fit,
        modifier =
          Modifier
            .fillMaxSize(),
      )
    } else {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 36.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(c.background),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
          ClipDockSymbol(ClipDockIconKind.Image, Modifier.size(42.dp), color = c.accentLink.fg)
          Text("等待图片预览", color = c.ink2, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
    ImageDetailStageOverlay(
      item = item,
      showingOriginal = showingOriginal,
      modifier = Modifier.align(Alignment.TopCenter),
    )
  }
}

@Composable
private fun ImageDetailStageOverlay(
  item: ClipHistoryItem,
  showingOriginal: Boolean,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(start = 18.dp, top = 96.dp, end = 18.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    ImageDetailStageBadge(item = item, showingOriginal = showingOriginal)
    Text("1 / 1", color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp, lineHeight = 14.sp, fontFamily = FontFamily.Monospace)
  }
}

@Composable
private fun ImageDetailStageBadge(item: ClipHistoryItem, showingOriginal: Boolean) {
  val c = ClipTheme.colors
  val label =
    when {
      showingOriginal -> "清晰原图 · 本机可复制"
      item.transferState == TransferState.DiscoveringPeer -> "正在查找来源设备"
      item.transferState == TransferState.Downloading -> "正在下载原图"
      item.transferState == TransferState.Failed || item.payloadState == PayloadState.Failed -> "原图取回失败"
      item.thumbnailUri.isNullOrBlank() -> "原图在远端"
      else -> "同步缩略图 · 原图在远端"
    }
  Surface(
    shape = CircleShape,
    color = Color.Black.copy(alpha = 0.58f),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ClipDockSymbol(
        if (item.transferState == TransferState.Failed || item.payloadState == PayloadState.Failed) ClipDockIconKind.Alert else if (showingOriginal) ClipDockIconKind.Check else ClipDockIconKind.Download,
        Modifier.size(16.dp),
        color = if (showingOriginal) c.online else c.accentLink.fg,
      )
      Text(label, color = Color.White, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
  }
}

@Composable
private fun ImageDetailTopOverlay(
  item: ClipHistoryItem,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .height(96.dp)
        .background(
          Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0.68f), Color.Black.copy(alpha = 0.24f), Color.Transparent),
          ),
        )
  ) {
    Spacer(Modifier.height(24.dp))
    Row(
      modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      PhotoOverlayIconButton(ClipDockIconKind.Chevron, "返回", onBack)
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(item.displayTitle, color = Color.White, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${item.sourceName?.takeIf(String::isNotBlank) ?: "未知来源"} · ${relativeTimeLabel(item.copiedAtMillis)}", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      PhotoOverlayIconButton(ClipDockIconKind.More, "更多", onClick = {}, enabled = false)
    }
  }
}

@Composable
private fun PhotoOverlayIconButton(
  icon: ClipDockIconKind,
  contentDescription: String,
  onClick: () -> Unit,
  enabled: Boolean = true,
) {
  Surface(
    shape = CircleShape,
    color = Color.Black.copy(alpha = 0.42f),
    contentColor = Color.White,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    modifier =
      Modifier
        .size(40.dp)
        .clip(CircleShape)
        .clickable(enabled = enabled, onClick = onClick)
        .semantics { this.contentDescription = contentDescription },
  ) {
    Box(contentAlignment = Alignment.Center) {
      ClipDockSymbol(icon, Modifier.size(19.dp), color = Color.White.copy(alpha = if (enabled) 1f else 0.38f))
    }
  }
}

@Composable
private fun ImageDetailInfoDrawer(
  item: ClipHistoryItem,
  state: ClipDockUiState,
  actions: MobileV4DetailActions,
  expanded: Boolean,
  height: androidx.compose.ui.unit.Dp,
  onExpandedChange: (Boolean) -> Unit,
  onCopyItem: () -> Unit,
  onDownloadToCache: () -> Unit,
  onCopyThumbnail: () -> Unit,
  onRemoveLocalCache: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val c = ClipTheme.colors
  Surface(
    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    color = c.surface,
    border = BorderStroke(1.dp, c.hairline),
    shadowElevation = 18.dp,
    modifier =
      modifier
        .fillMaxWidth()
        .height(height)
        .pointerInput(expanded) {
          detectVerticalDragGestures { _, dragAmount ->
            when {
              dragAmount < -9f -> onExpandedChange(true)
              dragAmount > 9f -> onExpandedChange(false)
            }
          }
        },
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Box(
            Modifier
              .width(42.dp)
              .height(4.dp)
              .clip(CircleShape)
              .background(c.hairline),
          )
          ImageDetailDrawerHeader(
            item = item,
            actions = actions,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            onCopyItem = onCopyItem,
            onDownloadToCache = onDownloadToCache,
          )
        }
      }
      item {
        ImageDetailStatusPills(item = item, state = state)
      }
      item {
        if (expanded) {
          ImageDetailExpandedActions(
            item = item,
            actions = actions,
            onCopyItem = onCopyItem,
            onDownloadToCache = onDownloadToCache,
            onCopyThumbnail = onCopyThumbnail,
            onRemoveLocalCache = onRemoveLocalCache,
          )
        } else {
          ImageDetailUtilityActions(
            item = item,
            actions = actions,
            onExpandedChange = onExpandedChange,
            onCopyThumbnail = onCopyThumbnail,
            onRemoveLocalCache = onRemoveLocalCache,
          )
        }
      }
      if (expanded) {
        item {
          ImageDetailInfoSection(item = item, state = state)
        }
        item {
          ImageDetailDangerActions(
            actions = actions,
            onDelete = onDelete,
          )
        }
      }
    }
  }
}

@Composable
private fun ImageDetailDrawerHeader(
  item: ClipHistoryItem,
  actions: MobileV4DetailActions,
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  onCopyItem: () -> Unit,
  onDownloadToCache: () -> Unit,
) {
  val c = ClipTheme.colors
  val hasLocalOriginal = mobileV4HasLocalCopySemantics(item)
  val primaryLabel =
    when {
      hasLocalOriginal -> "复制原图"
      expanded -> actions.downloadToCache.label
      else -> actions.primary.label
    }
  val primaryEnabled =
    when {
      hasLocalOriginal -> actions.primary.enabled
      expanded -> actions.downloadToCache.enabled
      else -> actions.primary.enabled
    }
  val primaryTone = if (hasLocalOriginal) ClipDockTone.Green else actions.primary.tone
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(item.displayTitle, color = c.ink, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(imageDrawerSubtitle(item), color = c.ink2, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    ImageDetailPrimaryButton(
      label = primaryLabel,
      tone = primaryTone,
      enabled = primaryEnabled,
      onClick = {
        when {
          hasLocalOriginal -> onCopyItem()
          expanded -> onDownloadToCache()
          else -> onExpandedChange(true)
        }
      },
      loading = primaryLabel == "取回中",
    )
  }
}

@Composable
private fun ImageDetailPrimaryButton(
  label: String,
  tone: ClipDockTone,
  enabled: Boolean,
  onClick: () -> Unit,
  loading: Boolean = false,
) {
  val c = ClipTheme.colors
  val background =
    when (tone) {
      ClipDockTone.Green -> c.online
      ClipDockTone.Amber -> Color(0xFFD97706)
      ClipDockTone.Red -> Color(0xFFDC2626)
      ClipDockTone.Blue -> c.accentLink.fg
      ClipDockTone.Neutral -> c.ink2
    }
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (enabled) background else c.sunken,
    contentColor = if (enabled) Color.White else c.ink3,
    modifier =
      Modifier
        .width(104.dp)
        .height(42.dp)
        .clip(RoundedCornerShape(12.dp))
        .clickable(enabled = enabled, onClick = onClick)
        .testTag(MobileV4Tags.DetailPrimaryAction),
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
      if (loading) {
        CircularProgressIndicator(
          modifier = Modifier.size(18.dp),
          strokeWidth = 2.dp,
          color = if (enabled) Color.White else c.accentLink.fg,
          trackColor = Color.Transparent,
        )
      } else {
        Text(label, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
  }
}

@Composable
private fun ImageDetailStatusPills(
  item: ClipHistoryItem,
  state: ClipDockUiState,
) {
  val hasLocalOriginal = mobileV4HasLocalCopySemantics(item)
  Row(
    horizontalArrangement = Arrangement.spacedBy(7.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth(),
  ) {
    ImageDetailPill(
      label = historyDetailStatus(item),
      tone =
        when {
          item.transferState == TransferState.Failed || item.payloadState == PayloadState.Failed -> ClipDockTone.Red
          hasLocalOriginal -> ClipDockTone.Green
          else -> ClipDockTone.Amber
        },
    )
    ImageDetailPill(
      label = if (hasLocalOriginal) "原图已下载" else if (item.thumbnailUri.isNullOrBlank()) "无缩略图" else "缩略图可用",
      tone = if (hasLocalOriginal) ClipDockTone.Green else ClipDockTone.Blue,
    )
    ImageDetailPill(
      label = if (state.wifiOnly) "仅 Wi-Fi" else "任意网络",
      tone = ClipDockTone.Neutral,
    )
  }
}

@Composable
private fun ImageDetailPill(label: String, tone: ClipDockTone) {
  val c = ClipTheme.colors
  val background =
    when (tone) {
      ClipDockTone.Green -> c.onlineSoft
      ClipDockTone.Blue -> c.accentLink.bg
      ClipDockTone.Amber -> Color(0xFFFEF3C7)
      ClipDockTone.Red -> Color(0xFFFEE2E2)
      ClipDockTone.Neutral -> c.surface
    }
  val foreground =
    when (tone) {
      ClipDockTone.Green -> c.online
      ClipDockTone.Blue -> c.accentLink.fg
      ClipDockTone.Amber -> Color(0xFFD97706)
      ClipDockTone.Red -> Color(0xFFDC2626)
      ClipDockTone.Neutral -> c.ink2
    }
  Surface(
    shape = RoundedCornerShape(7.dp),
    color = background,
    contentColor = foreground,
    border = if (tone == ClipDockTone.Neutral) BorderStroke(1.dp, c.hairline) else null,
  ) {
    Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
  }
}

@Composable
private fun ImageDetailUtilityActions(
  item: ClipHistoryItem,
  actions: MobileV4DetailActions,
  onExpandedChange: (Boolean) -> Unit,
  onCopyThumbnail: () -> Unit,
  onRemoveLocalCache: () -> Unit,
) {
  val hasLocalOriginal = mobileV4HasLocalCopySemantics(item)
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ImageDetailUtilityAction(
      icon = ClipDockIconKind.Image,
      label = "复制缩略图",
      tone = ClipDockTone.Blue,
      enabled = actions.copyThumbnail.enabled,
      onClick = onCopyThumbnail,
      modifier = Modifier.weight(1f),
    )
    ImageDetailUtilityAction(
      icon = ClipDockIconKind.Share,
      label = "分享",
      tone = ClipDockTone.Neutral,
      enabled = false,
      onClick = {},
      modifier = Modifier.weight(1f),
    )
    if (hasLocalOriginal) {
      ImageDetailUtilityAction(
        icon = ClipDockIconKind.Folder,
        label = "移除缓存",
        tone = ClipDockTone.Amber,
        enabled = actions.removeLocalCache.enabled,
        onClick = onRemoveLocalCache,
        modifier = Modifier.weight(1f),
      )
    } else {
      ImageDetailUtilityAction(
        icon = ClipDockIconKind.More,
        label = "详情",
        tone = ClipDockTone.Neutral,
        enabled = true,
        onClick = { onExpandedChange(true) },
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun ImageDetailUtilityAction(
  icon: ClipDockIconKind,
  label: String,
  tone: ClipDockTone,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val c = ClipTheme.colors
  val background =
    when (tone) {
      ClipDockTone.Green -> c.onlineSoft
      ClipDockTone.Blue -> c.accentLink.bg
      ClipDockTone.Amber -> Color(0xFFFEF3C7)
      ClipDockTone.Red -> Color(0xFFFEE2E2)
      ClipDockTone.Neutral -> c.surface
    }
  val foreground =
    when (tone) {
      ClipDockTone.Green -> c.online
      ClipDockTone.Blue -> c.accentLink.fg
      ClipDockTone.Amber -> Color(0xFFD97706)
      ClipDockTone.Red -> Color(0xFFDC2626)
      ClipDockTone.Neutral -> c.ink
    }
  Surface(
    shape = RoundedCornerShape(13.dp),
    color = if (enabled) background else c.surface,
    border = BorderStroke(1.dp, if (tone == ClipDockTone.Neutral) c.hairline else Color.Transparent),
    contentColor = if (enabled) foreground else c.ink3,
    modifier =
      modifier
        .height(58.dp)
        .clip(RoundedCornerShape(13.dp))
        .clickable(enabled = enabled, onClick = onClick),
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
      ClipDockSymbol(icon, Modifier.size(19.dp), color = if (enabled) foreground else c.ink3)
      Text(label, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

@Composable
private fun ImageDetailExpandedActions(
  item: ClipHistoryItem,
  actions: MobileV4DetailActions,
  onCopyItem: () -> Unit,
  onDownloadToCache: () -> Unit,
  onCopyThumbnail: () -> Unit,
  onRemoveLocalCache: () -> Unit,
) {
  val hasLocalOriginal = mobileV4HasLocalCopySemantics(item)
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
    if (hasLocalOriginal) {
      ImageDetailActionCard("复制原图", "写入剪贴板", ClipDockTone.Green, actions.primary.enabled, onCopyItem, Modifier.weight(1f), primary = true)
      ImageDetailActionCard("移除缓存", "保留同步记录", ClipDockTone.Amber, actions.removeLocalCache.enabled, onRemoveLocalCache, Modifier.weight(1f))
      ImageDetailActionCard(actions.copyThumbnail.label, "快速使用预览", ClipDockTone.Blue, actions.copyThumbnail.enabled, onCopyThumbnail, Modifier.weight(1f))
    } else {
      ImageDetailActionCard(actions.downloadToCache.label, "下载到本机缓存", ClipDockTone.Blue, actions.downloadToCache.enabled, onDownloadToCache, Modifier.weight(1f), primary = true)
      ImageDetailActionCard("复制缩略图", "保留远端原图", ClipDockTone.Neutral, actions.copyThumbnail.enabled, onCopyThumbnail, Modifier.weight(1f))
    }
  }
}

@Composable
private fun ImageDetailActionCard(
  title: String,
  subtitle: String,
  tone: ClipDockTone,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  primary: Boolean = false,
) {
  val c = ClipTheme.colors
  val primaryColor = if (tone == ClipDockTone.Green) c.online else c.accentLink.fg
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (primary && enabled) primaryColor else c.surface,
    border = BorderStroke(1.dp, if (primary && enabled) Color.Transparent else c.hairline),
    contentColor = if (primary && enabled) Color.White else if (enabled) c.ink else c.ink3,
    modifier =
      modifier
        .height(64.dp)
        .clip(RoundedCornerShape(14.dp))
        .clickable(enabled = enabled, onClick = onClick),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
      verticalArrangement = Arrangement.Center,
    ) {
      Text(title, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(subtitle, fontSize = 10.sp, lineHeight = 12.sp, color = if (primary && enabled) Color.White.copy(alpha = 0.72f) else c.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

@Composable
private fun ImageDetailInfoSection(item: ClipHistoryItem, state: ClipDockUiState) {
  Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
    ImageDetailInfoGroup("内容") {
      ImageDetailMetaRow("文件名", item.displayTitle)
      ImageDetailMetaRow("尺寸", imageOriginalDimensionsLabel(item))
      ImageDetailMetaRow("原图大小", imageOriginalSizeLabel(item))
    }
    ImageDetailInfoGroup("同步") {
      ImageDetailMetaRow("来源设备", item.sourceName?.takeIf(String::isNotBlank) ?: "未知来源")
      ImageDetailMetaRow("同步空间", state.syncId ?: "未加入")
      ImageDetailMetaRow("assetId", item.assetId?.let(::shortIdentifier) ?: "无可取回资产")
      ImageDetailMetaRow("contentHash", shortIdentifier(item.contentHash))
    }
    ImageDetailInfoGroup("缓存") {
      ImageDetailMetaRow("本机缓存", imageLocalCacheLabel(item))
      ImageDetailMetaRow("缩略图", imageThumbnailDescription(item))
      ImageDetailMetaRow("原图状态", imageOriginalStateLabel(item))
      ImageDetailMetaRow("保留策略", "30 天")
      ImageDetailMetaRow("复制次数", "${item.copyCount} 次 · ${relativeTimeLabel(item.copiedAtMillis)}")
    }
  }
}

@Composable
private fun ImageDetailInfoGroup(
  title: String,
  content: @Composable () -> Unit,
) {
  val c = ClipTheme.colors
  Column(
    modifier = Modifier.fillMaxWidth().padding(top = 11.dp, bottom = 10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(title, color = c.ink2, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.ExtraBold)
    content()
  }
}

@Composable
private fun ImageDetailMetaRow(label: String, value: String) {
  val c = ClipTheme.colors
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Text(label, color = c.ink2, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.width(82.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(value, color = c.ink, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun ImageDetailDangerActions(
  actions: MobileV4DetailActions,
  onDelete: () -> Unit,
) {
  val c = ClipTheme.colors
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0xFFFEE2E2),
    contentColor = Color(0xFFDC2626),
    modifier =
      Modifier
        .fillMaxWidth()
        .height(44.dp)
        .clip(RoundedCornerShape(14.dp))
        .clickable(enabled = actions.deleteSyncRecord.enabled, onClick = onDelete)
        .testTag(MobileV4Tags.DetailTrashAction),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ClipDockSymbol(ClipDockIconKind.Trash, Modifier.size(17.dp), color = Color(0xFFDC2626))
      Text(actions.deleteSyncRecord.label, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
  }
}

@Composable
private fun V4SheetOverlay(
  testTag: String,
  withGrabber: Boolean,
  content: @Composable () -> Unit,
) {
  val c = ClipTheme.colors
  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(Color(0x6B060D0D)),
    contentAlignment = Alignment.BottomCenter,
  ) {
    Surface(
      shape = RoundedCornerShape(if (withGrabber) 28.dp else 26.dp),
      color = c.surface,
      border = BorderStroke(1.dp, c.hairline),
      shadowElevation = 18.dp,
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
          .testTag(testTag),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(if (withGrabber) 12.dp else 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (withGrabber) {
          Box(
            modifier =
              Modifier
                .align(Alignment.CenterHorizontally)
                .width(42.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(c.hairline),
          )
        }
        content()
      }
    }
  }
}

@Composable
private fun ItemDetailTopBar(item: ClipHistoryItem, onBack: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    ClipDockIconButton(ClipDockIconKind.Chevron, "返回", onClick = onBack)
    Text("${item.type.label}详情", color = ClipTheme.colors.ink, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f))
    ClipDockIconButton(ClipDockIconKind.Share, "分享", onClick = {}, enabled = false)
  }
}

@Composable
private fun ItemDetailSummary(item: ClipHistoryItem) {
  val display = historyDetailDisplay(item)
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
    HistoryActionPill(display.contentType, display.previewIcon, typeTone(item.type))
    HistoryActionPill(display.source, if (item.sourceName?.contains("Pixel", ignoreCase = true) == true) ClipDockIconKind.Devices else ClipDockIconKind.Window, ClipDockTone.Neutral)
    HistoryActionPill(display.timeLabel, ClipDockIconKind.History, ClipDockTone.Neutral)
  }
}

@Composable
private fun ItemDetailImagePreview(item: ClipHistoryItem) {
  val previewUri =
    when (item.type) {
      ClipItemType.Link -> item.linkPreviewUri
      else -> item.thumbnailUri ?: item.localUri
    }
  val bitmap by rememberImageBitmap(previewUri)
  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .height(182.dp)
        .clip(RoundedCornerShape(20.dp))
        .background(ClipTheme.colors.surface),
    contentAlignment = Alignment.Center,
  ) {
    if (bitmap != null) {
      Image(bitmap = bitmap!!, contentDescription = item.displayTitle, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    } else {
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ClipDockSymbol(if (item.type == ClipItemType.Link) ClipDockIconKind.Link else ClipDockIconKind.Image, Modifier.size(34.dp), color = ClipTheme.colors.accentLink.fg)
        Text(if (item.type == ClipItemType.Link) "链接预览" else "远端原图", style = MaterialTheme.typography.labelMedium, color = ClipTheme.colors.ink2)
      }
    }
  }
}

@Composable
private fun ItemDetailContentPreview(item: ClipHistoryItem) {
  val text =
    when (item.type) {
      ClipItemType.Text,
      ClipItemType.RichText,
      ClipItemType.Link -> historyFullText(item)
      ClipItemType.Image -> "列表只显示同步缩略图；原图需要 P2P 取回。"
      ClipItemType.File -> item.detail.ifBlank { item.body.ifBlank { "文件内容按需下载到本机缓存。" } }
      else -> item.body.ifBlank { item.detail }
    }
  ClipDockCard {
    Text(
      text,
      color = ClipTheme.colors.ink,
      fontSize = 16.sp,
      lineHeight = 24.sp,
      fontWeight = FontWeight.Normal,
      maxLines = 12,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ItemDetailInlineActions(
  actions: MobileV4DetailActions,
  onPrimary: () -> Unit,
  onDelete: () -> Unit,
) {
  Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
    DetailActionTile(
      actions.primary.icon,
      actions.primary.label,
      ClipDockTone.Green,
      actions.primary.enabled,
      onPrimary,
      Modifier.weight(1f),
      primary = true,
      loading = actions.primary.label == "取回中" || actions.primary.label == "复制中",
    )
    DetailActionTile(ClipDockIconKind.Pin, "固定", ClipDockTone.Neutral, enabled = false, onClick = {}, modifier = Modifier.weight(1f))
    DetailActionTile(ClipDockIconKind.Share, "分享", ClipDockTone.Neutral, enabled = false, onClick = {}, modifier = Modifier.weight(1f))
  }
}

@Composable
private fun DetailActionTile(
  icon: ClipDockIconKind,
  label: String,
  tone: ClipDockTone,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  primary: Boolean = false,
  loading: Boolean = false,
) {
  val c = ClipTheme.colors
  val colors = historyToneColors(tone)
  Column(
    modifier =
      modifier
        .height(62.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(if (primary) c.coralSoft else c.surface)
        .clickable(enabled = enabled, onClick = onClick)
        .padding(vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    if (loading) {
      CircularProgressIndicator(
        modifier = Modifier.size(22.dp),
        strokeWidth = 2.dp,
        color = colors.first,
        trackColor = colors.first.copy(alpha = 0.14f),
      )
    } else {
      ClipDockSymbol(icon, Modifier.size(23.dp), color = if (enabled) colors.first else c.ink2)
    }
    Text(label, color = if (enabled) colors.first else c.ink2, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun ItemDetailMetaGrid(item: ClipHistoryItem, state: ClipDockUiState) {
  val display = historyDetailDisplay(item)
  DetailInfoList(
    rows =
      listOf(
        "来源" to display.source,
        "类型" to display.contentType,
        "同步状态" to display.status,
        "空间" to (state.syncId ?: "未加入"),
        "内容长度" to "${historyFullText(item).length} 个字符",
      ),
  )
}

@Composable
private fun ImageDetailInfoList(item: ClipHistoryItem, state: ClipDockUiState) {
  DetailInfoList(
    rows =
      listOf(
        "文件名" to item.displayTitle.ifBlank { "图片" },
        "尺寸" to imageOriginalDimensionsLabel(item),
        "大小" to imageOriginalSizeLabel(item),
        "来源" to (item.sourceName?.takeIf(String::isNotBlank) ?: "未知来源"),
        "同步状态" to historyDetailStatus(item),
      ),
  )
}

@Composable
private fun DetailInfoList(rows: List<Pair<String, String>>) {
  val c = ClipTheme.colors
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = c.surface,
    border = BorderStroke(1.dp, c.hairline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column {
      rows.forEachIndexed { index, row ->
        Row(
          modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(horizontal = 14.dp, vertical = 9.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(row.first, color = c.ink2, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(84.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(row.second, color = c.ink, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (index != rows.lastIndex) {
          SettingDivider()
        }
      }
    }
  }
}

@Composable
private fun ItemDetailActionDock(
  actions: MobileV4DetailActions,
  modifier: Modifier = Modifier,
  onPrimary: () -> Unit,
  onDelete: () -> Unit,
) {
  val c = ClipTheme.colors
  Surface(
    color = c.surface.copy(alpha = 0.97f),
    border = BorderStroke(1.dp, c.hairline),
    shadowElevation = 8.dp,
    modifier = modifier.fillMaxWidth().padding(14.dp).clip(RoundedCornerShape(22.dp)),
  ) {
    Row(
      modifier = Modifier.height(66.dp).padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Button(
        onClick = onPrimary,
        enabled = actions.primary.enabled,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.weight(1f).height(50.dp).testTag(MobileV4Tags.DetailPrimaryAction),
      ) {
        ClipDockSymbol(actions.primary.icon, Modifier.size(18.dp), color = Color.White)
        Spacer(Modifier.width(8.dp))
        Text(actions.primary.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      listOf(ClipDockIconKind.Pin, ClipDockIconKind.Share, ClipDockIconKind.Text).forEach { icon ->
        DockIconButton(icon = icon, enabled = false, onClick = {})
      }
      DockIconButton(
        icon = ClipDockIconKind.Trash,
        enabled = actions.deleteSyncRecord.enabled,
        danger = true,
        onClick = onDelete,
        modifier = Modifier.testTag(MobileV4Tags.DetailTrashAction),
      )
    }
  }
}

@Composable
private fun DockIconButton(
  icon: ClipDockIconKind,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  danger: Boolean = false,
) {
  val c = ClipTheme.colors
  Surface(
    shape = RoundedCornerShape(15.dp),
    color = if (danger) Color(0xFFFEE2E2) else c.surface,
    contentColor = if (danger) Color(0xFFDC2626) else c.ink2,
    modifier =
      modifier
        .size(50.dp)
        .clip(RoundedCornerShape(15.dp))
        .clickable(enabled = enabled, onClick = onClick),
  ) {
    Box(contentAlignment = Alignment.Center) {
      ClipDockSymbol(icon, Modifier.size(19.dp), color = if (danger) Color(0xFFDC2626) else c.ink2)
    }
  }
}

@Composable
private fun RemoteRetrievalSheetContent(
  item: ClipHistoryItem,
  actions: MobileV4DetailActions,
  onDownloadToCache: () -> Unit,
  onCopyThumbnail: () -> Unit,
) {
  Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      IconTile(ClipDockIconKind.Download, tone = ClipDockTone.Blue)
      Column(Modifier.weight(1f)) {
        Text("取回远端${if (item.type == ClipItemType.Image) "图片" else "文件"}", style = MaterialTheme.typography.titleMedium)
        Text("选择下载后如何处理，不自动覆盖剪贴板。", style = MaterialTheme.typography.bodySmall, color = ClipTheme.colors.ink2)
      }
    }
    SheetActionRow(actions.downloadToCache, "下载到本机缓存；下载后可自行复制。", MobileV4Tags.RemoteDownloadToCache, onDownloadToCache, primary = true)
    SheetActionRow(actions.copyThumbnail, "快速使用预览图，保留原图远端状态", MobileV4Tags.RemoteCopyThumbnail, onCopyThumbnail)
  }
}

@Composable
private fun DeleteConfirmSheetContent(
  actions: MobileV4DetailActions,
  onRemoveLocalCache: () -> Unit,
  onDeleteSyncRecord: () -> Unit,
  onCancel: () -> Unit,
) {
  Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
    IconTile(ClipDockIconKind.Trash, tone = ClipDockTone.Red, modifier = Modifier.padding(bottom = 13.dp))
    Text("删除这条历史？", fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold, color = ClipTheme.colors.ink)
    Text(
      "如果只清理本机缓存，其他设备和同步空间仍保留记录。删除同步记录会从所有设备历史中移除。",
      fontSize = 12.sp,
      lineHeight = 17.sp,
      color = ClipTheme.colors.ink2,
      modifier = Modifier.padding(top = 7.dp, bottom = 14.dp),
    )
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
      ConfirmSheetButton(actions.removeLocalCache, MobileV4Tags.DeleteRemoveLocalCache, onRemoveLocalCache)
      ConfirmSheetButton(actions.deleteSyncRecord, MobileV4Tags.DeleteSyncRecord, onDeleteSyncRecord, danger = true)
      ConfirmSheetButton(
        MobileV4DetailAction(
          kind = MobileV4ActionKind.DeleteSyncRecord,
          label = "取消",
          icon = ClipDockIconKind.X,
          enabled = true,
          tone = ClipDockTone.Neutral,
          message = "",
        ),
        MobileV4Tags.DeleteCancel,
        onCancel,
        iconVisible = false,
      )
    }
  }
}

@Composable
private fun SheetActionRow(
  action: MobileV4DetailAction,
  subtitle: String,
  testTag: String,
  onClick: () -> Unit,
  primary: Boolean = false,
) {
  val c = ClipTheme.colors
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .height(62.dp)
        .clip(RoundedCornerShape(18.dp))
        .background(if (primary) c.onlineSoft else c.surface)
        .clickable(enabled = action.enabled, onClick = onClick)
        .testTag(testTag)
        .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    IconTile(action.icon, tone = if (primary) ClipDockTone.Green else action.tone)
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(action.label, style = MaterialTheme.typography.titleSmall, color = if (primary) c.onlineInk else c.ink)
      Text(
        if (action.enabled) subtitle else action.message,
        style = MaterialTheme.typography.bodySmall,
        color = if (primary) c.onlineInk.copy(alpha = 0.74f) else c.ink2,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun ConfirmSheetButton(
  action: MobileV4DetailAction,
  testTag: String,
  onClick: () -> Unit,
  danger: Boolean = false,
  iconVisible: Boolean = true,
) {
  val c = ClipTheme.colors
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .height(46.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(if (danger) Color(0xFFDC2626) else c.surface)
        .clickable(enabled = action.enabled, onClick = onClick)
        .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    if (iconVisible) {
      ClipDockSymbol(action.icon, Modifier.size(16.dp), color = if (danger) Color.White else c.ink)
      Spacer(Modifier.width(8.dp))
    }
    Text(
      action.label,
      color = if (danger) Color.White else if (action.enabled) c.ink else c.ink2,
      fontSize = 13.sp,
      lineHeight = 16.sp,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
internal fun HistoryPage(
  state: ClipDockUiState,
  onOpenSettings: () -> Unit,
  onSyncNow: () -> Unit,
  onOpenItemDetail: (String) -> Unit,
  onDownloadToCache: (ClipHistoryItem) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var selectedVisualFilter by remember { mutableStateOf(HistoryVisualFilter.All) }
  val filtered = filteredHistoryItems(state.items, selectedVisualFilter)

  Box(modifier = modifier.fillMaxSize()) {
    LazyVerticalStaggeredGrid(
      columns = StaggeredGridCells.Adaptive(164.dp),
      contentPadding = PaddingValues(start = 14.dp, top = 15.dp, end = 14.dp, bottom = 18.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalItemSpacing = 10.dp,
      modifier = Modifier.fillMaxSize(),
    ) {
      item(span = StaggeredGridItemSpan.FullLine) {
        HistoryStableTopBar(
          state = state,
          onOpenSettings = onOpenSettings,
          onSyncNow = onSyncNow,
        )
      }
      item(span = StaggeredGridItemSpan.FullLine) {
        HistorySearchPill()
      }
      item(span = StaggeredGridItemSpan.FullLine) {
        HistoryVisualFilterRow(
          selected = selectedVisualFilter,
          onSelected = { selectedVisualFilter = it },
          allItems = state.items,
        )
      }
      item(span = StaggeredGridItemSpan.FullLine) {
        HistoryHealthStrip(
          state = state,
          onClick = if (state.tokenPresent) onSyncNow else onOpenSettings,
        )
      }
      if (filtered.isEmpty()) {
        item(span = StaggeredGridItemSpan.FullLine) {
          EmptyState(
            title = if (state.tokenPresent) "暂无同步记录" else "先连接服务端",
            subtitle = if (state.tokenPresent) "点击同步拉取最近剪贴板历史。" else "ClipDock Android 会从同步空间读取最近历史。",
            actionLabel = if (state.tokenPresent) "立即同步" else "打开设置",
            onAction = if (state.tokenPresent) onSyncNow else onOpenSettings,
          )
        }
      } else {
        staggeredItemsIndexed(filtered, key = { _, item -> item.stableId }) { index, item ->
          HistoryStableCard(
            item = item,
            selected = index == 0,
            onOpenDetail = { onOpenItemDetail(item.stableId) },
            onDownload = { onDownloadToCache(item) },
          )
        }
      }
    }
  }
}

private enum class HistoryVisualFilter(val label: String) {
  All("全部"),
  Text("文本"),
  Link("链接"),
  Image("图片"),
  File("文件"),
}

private enum class HistoryCardVariant(val label: String) {
  Link("Link"),
  Text("Text"),
  Code("Code"),
  Image("Image"),
  Note("Note"),
  File("File"),
}

@Composable
private fun HistoryStableTopBar(
  state: ClipDockUiState,
  onOpenSettings: () -> Unit,
  onSyncNow: () -> Unit,
) {
  val c = ClipTheme.colors
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(9.dp),
  ) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        "剪贴板",
        color = c.ink,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      HistorySyncChip(state)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      HistoryRoundIconButton(icon = ClipDockIconKind.Search, contentDescription = "搜索", onClick = {})
      HistoryRoundIconButton(icon = ClipDockIconKind.Plus, contentDescription = "添加", onClick = onOpenSettings)
    }
  }
}

@Composable
private fun HistorySyncChip(state: ClipDockUiState) {
  val c = ClipTheme.colors
  Surface(
    shape = CircleShape,
    color = c.surface,
    border = BorderStroke(1.dp, c.hairline),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      Box(Modifier.size(8.dp).clip(CircleShape).background(if (state.tokenPresent) c.online else c.ink3))
      Text(
        historyStableSyncText(state),
        color = c.ink2,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun HistoryRoundIconButton(
  icon: ClipDockIconKind,
  contentDescription: String,
  onClick: () -> Unit,
) {
  val c = ClipTheme.colors
  Surface(
    shape = CircleShape,
    color = c.surface,
    border = BorderStroke(1.dp, c.hairline),
    shadowElevation = 0.dp,
    modifier = Modifier
      .size(42.dp)
      .clip(CircleShape)
      .clickable(onClick = onClick)
      .semantics { this.contentDescription = contentDescription },
  ) {
    Box(contentAlignment = Alignment.Center) {
      ClipDockSymbol(icon, Modifier.size(22.dp), color = c.ink2)
    }
  }
}

@Composable
private fun HistorySearchPill() {
  val c = ClipTheme.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(50.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(c.sunken)
      .semantics { contentDescription = "搜索文本、链接、文件名" }
      .padding(horizontal = 15.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    ClipDockSymbol(ClipDockIconKind.Search, Modifier.size(20.dp), color = c.ink3)
    Text(
      "搜索文本、链接、文件名",
      color = c.ink3,
      fontSize = 14.sp,
      lineHeight = 18.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun HistoryVisualFilterRow(
  selected: HistoryVisualFilter,
  onSelected: (HistoryVisualFilter) -> Unit,
  allItems: List<ClipHistoryItem> = emptyList(),
) {
  val counts = remember(allItems) {
    mapOf(
      HistoryVisualFilter.All to allItems.size,
      HistoryVisualFilter.Text to allItems.count { historyCardVariant(it) in setOf(HistoryCardVariant.Text, HistoryCardVariant.Code, HistoryCardVariant.Note) },
      HistoryVisualFilter.Link to allItems.count { historyCardVariant(it) == HistoryCardVariant.Link },
      HistoryVisualFilter.Image to allItems.count { historyCardVariant(it) == HistoryCardVariant.Image },
      HistoryVisualFilter.File to allItems.count { historyCardVariant(it) == HistoryCardVariant.File },
    )
  }
  LazyRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(7.dp),
    contentPadding = PaddingValues(horizontal = 0.dp),
  ) {
    items(HistoryVisualFilter.entries) { filter ->
      HistoryVisualFilterChip(
        filter = filter,
        count = counts[filter] ?: 0,
        selected = filter == selected,
        onClick = { onSelected(filter) },
      )
    }
  }
}

@Composable
private fun HistoryVisualFilterChip(
  filter: HistoryVisualFilter,
  count: Int,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val c = ClipTheme.colors
  val bgColor = if (selected) c.coral else c.surface
  val contentColor = if (selected) Color.White else c.ink2
  val cntBg = if (selected) Color.White.copy(alpha = 0.22f) else c.sunken
  val cntColor = if (selected) Color.White else c.ink3
  Row(
    modifier = Modifier
      .height(36.dp)
      .clip(RoundedCornerShape(999.dp))
      .background(bgColor)
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Text(
      filter.label,
      color = contentColor,
      fontSize = 13.sp,
      lineHeight = 16.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
    )
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(999.dp))
        .background(cntBg)
        .padding(horizontal = 6.dp, vertical = 1.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        count.toString(),
        color = cntColor,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun HistoryHealthStrip(state: ClipDockUiState, onClick: () -> Unit) {
  val c = ClipTheme.colors
  val isConnected = state.tokenPresent
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = c.surface,
    shadowElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .height(68.dp)
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick),
  ) {
    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
      HistoryHealthCell(
        icon = ClipDockIconKind.Cloud,
        title = if (isConnected) "同步正常" else "等待连接",
        subtitle = if (isConnected) "连接稳定" else "打开设置",
        iconTint = if (isConnected) c.online else c.ink3,
        modifier = Modifier.weight(1.1f),
        showDivider = true,
      )
      HistoryHealthCell(
        icon = ClipDockIconKind.Devices,
        title = "在线设备",
        subtitle = "${state.p2pDevices.size + if (state.tokenPresent) 1 else 0} 台",
        iconTint = c.coral,
        modifier = Modifier.weight(0.85f),
        showDivider = true,
      )
      HistoryHealthCell(
        icon = ClipDockIconKind.Cloud,
        title = "待上传",
        subtitle = "0 项",
        iconTint = c.ink3,
        modifier = Modifier.weight(0.85f),
        showDivider = false,
      )
    }
  }
}

@Composable
private fun HistoryHealthCell(
  icon: ClipDockIconKind,
  title: String,
  subtitle: String,
  iconTint: Color,
  modifier: Modifier = Modifier,
  showDivider: Boolean,
) {
  val c = ClipTheme.colors
  Row(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 13.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        ClipDockSymbol(icon, Modifier.size(17.dp), color = iconTint)
        Text(title, color = c.ink, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Text(subtitle, color = c.ink2, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (showDivider) {
      Box(Modifier.width(1.dp).fillMaxSize().padding(vertical = 10.dp).background(c.hairline))
    }
  }
}

@Composable
private fun HistoryStableCard(
  item: ClipHistoryItem,
  selected: Boolean,
  onOpenDetail: () -> Unit,
  onDownload: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val source = item.sourceName?.takeIf(String::isNotBlank) ?: "未知来源"
  val timeLabel = historyStableClockLabel(item)
  val fullText = listOf(item.title, item.body).filter { it.isNotBlank() }.distinct().joinToString("\n").ifBlank { item.detail }

  when {
    item.type == ClipItemType.Image -> ImageClipCard(
      name = item.title.ifBlank { "图片" },
      dimensions = if (item.thumbnailWidth != null && item.thumbnailHeight != null) "${item.thumbnailWidth}×${item.thumbnailHeight}" else "",
      sizeLabel = byteCountLabel(item.thumbnailByteCount),
      downloaded = !item.localUri.isNullOrBlank(),
      onClick = onOpenDetail,
      onDownload = onDownload,
      modifier = modifier.testTag(historyCardTestTag(item.stableId)),
    )
    item.type == ClipItemType.File -> FileClipCard(
      name = item.title.ifBlank { "文件" },
      ext = item.title.substringAfterLast('.', "").uppercase().take(4).ifBlank { "FILE" },
      sizeLabel = byteCountLabel(item.thumbnailByteCount).ifBlank { item.body.take(20) },
      downloaded = !item.localUri.isNullOrBlank(),
      onClick = onOpenDetail,
      onDownload = onDownload,
      modifier = modifier.testTag(historyCardTestTag(item.stableId)),
    )
    item.type == ClipItemType.Link -> LinkClipCard(
      title = item.title.ifBlank { item.body },
      description = item.body.ifBlank { item.detail },
      domain = item.linkSiteName?.takeIf(String::isNotBlank) ?: (item.body.removePrefix("https://").removePrefix("http://").substringBefore("/").ifBlank { source }) + " · $timeLabel",
      onClick = onOpenDetail,
      modifier = modifier.testTag(historyCardTestTag(item.stableId)),
    )
    item.type == ClipItemType.Color -> {
      val hexStr = item.title.ifBlank { "#000000" }
      val colorVal = runCatching {
        val stripped = hexStr.removePrefix("#")
        Color(("FF$stripped").toLong(16))
      }.getOrElse { Color.Black }
      ColorClipCard(
        color = colorVal,
        hex = hexStr,
        rgb = item.body.ifBlank { hexStr },
        source = source,
        onClick = onOpenDetail,
        modifier = modifier.testTag(historyCardTestTag(item.stableId)),
      )
    }
    isCodeVisual(item) -> CodeClipCard(
      code = fullText,
      source = "$source · $timeLabel",
      pinned = false,
      onClick = onOpenDetail,
      modifier = modifier.testTag(historyCardTestTag(item.stableId)),
    )
    else -> TextClipCard(
      text = fullText,
      source = source,
      time = timeLabel,
      onClick = onOpenDetail,
      modifier = modifier.testTag(historyCardTestTag(item.stableId)),
    )
  }
}

@Composable
private fun HistoryTypePill(label: String, tone: ClipDockTone) {
  val colors = historyToneColors(tone)
  Surface(shape = RoundedCornerShape(7.dp), color = colors.second, contentColor = colors.first) {
    Text(label, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
  }
}

@Composable
private fun HistoryActionPill(label: String, icon: ClipDockIconKind, tone: ClipDockTone) {
  val colors = historyToneColors(tone)
  Row(
    modifier =
      Modifier
        .height(26.dp)
        .clip(CircleShape)
        .background(colors.second)
        .padding(horizontal = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    ClipDockSymbol(icon, Modifier.size(14.dp), color = colors.first)
    Text(label, color = colors.first, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
  }
}

@Composable
private fun historyToneColors(tone: ClipDockTone): Pair<Color, Color> {
  val c = ClipTheme.colors
  return when (tone) {
    ClipDockTone.Green -> c.online to c.onlineSoft
    ClipDockTone.Blue -> c.accentLink.fg to c.accentLink.bg
    ClipDockTone.Amber -> Color(0xFFD97706) to Color(0xFFFEF3C7)
    ClipDockTone.Red -> Color(0xFFDC2626) to Color(0xFFFEE2E2)
    ClipDockTone.Neutral -> c.ink2 to c.sunken
  }
}

@Composable
private fun HistoryMediaFallback(variant: HistoryCardVariant, modifier: Modifier = Modifier) {
  if (variant == HistoryCardVariant.File) {
    Box(
      modifier =
        modifier.background(
          Brush.verticalGradient(
            listOf(Color(0xFFFFF7ED), Color(0xFFFFFFFF), Color(0xFF64748B)),
          ),
        ),
    ) {
      Box(
        Modifier
          .align(Alignment.Center)
          .fillMaxWidth(0.74f)
          .aspectRatio(1.45f)
          .clip(RoundedCornerShape(18.dp))
          .background(Color.White.copy(alpha = 0.78f)),
      )
      Box(Modifier.align(Alignment.CenterStart).padding(start = 24.dp).size(62.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFB923C)))
      Box(Modifier.align(Alignment.TopEnd).padding(top = 42.dp, end = 30.dp).width(78.dp).height(12.dp).clip(CircleShape).background(Color(0xFFFDAD64)))
      Box(Modifier.align(Alignment.TopEnd).padding(top = 68.dp, end = 56.dp).width(48.dp).height(12.dp).clip(CircleShape).background(Color(0xFFFDD6A5)))
      Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 50.dp).fillMaxWidth(0.70f).height(10.dp).clip(CircleShape).background(Color(0xFFD7CEC2)))
    }
  } else {
    Box(
      modifier =
        modifier.background(
          Brush.verticalGradient(
            listOf(Color(0xFFA7F3D0), Color(0xFF67E8F9), Color(0xFF2563EB)),
          ),
        ),
    ) {
      Box(Modifier.align(Alignment.TopEnd).padding(24.dp).size(54.dp).clip(CircleShape).background(Color(0xFFFEF3C7)))
      Box(
        Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .height(118.dp)
          .background(Brush.linearGradient(listOf(Color(0xFF18A67A), Color(0xFFFACC15)))),
      )
      Box(
        Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .height(82.dp)
          .background(Color(0xB80F766E)),
      )
    }
  }
}

@Composable
private fun HistoryMiniChip(label: String, tone: ClipDockTone) {
  val c = ClipTheme.colors
  val colors =
    when (tone) {
      ClipDockTone.Green -> c.online to c.onlineSoft
      ClipDockTone.Blue -> c.accentLink.fg to c.accentLink.bg
      ClipDockTone.Amber -> Color(0xFFD97706) to Color(0xFFFEF3C7)
      ClipDockTone.Red -> Color(0xFFDC2626) to Color(0xFFFEE2E2)
      ClipDockTone.Neutral -> c.ink2 to c.sunken
    }
  Box(
    modifier =
      Modifier
        .height(22.dp)
        .clip(CircleShape)
        .background(colors.second)
        .padding(horizontal = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(label, color = colors.first, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
  }
}

private val HistoryDesignInk = Color(0xFF101827)
private val HistoryCardBorder = Color(0x3D94A3B8)
private val HistoryCodeMarkerRegex =
  Regex("(@main|\\bfun\\s|\\bclass\\s|\\bstruct\\s|\\bimport\\s|\\bpackage\\s|\\bWindowGroup\\b)")

private fun filteredHistoryItems(items: List<ClipHistoryItem>, filter: HistoryVisualFilter): List<ClipHistoryItem> =
  items.filter { item ->
    when (filter) {
      HistoryVisualFilter.All -> true
      HistoryVisualFilter.Text -> historyCardVariant(item) in setOf(HistoryCardVariant.Text, HistoryCardVariant.Code, HistoryCardVariant.Note)
      HistoryVisualFilter.Link -> historyCardVariant(item) == HistoryCardVariant.Link
      HistoryVisualFilter.Image -> historyCardVariant(item) == HistoryCardVariant.Image
      HistoryVisualFilter.File -> historyCardVariant(item) == HistoryCardVariant.File
    }
  }

private fun isImportantVisual(item: ClipHistoryItem): Boolean = item.copyCount > 1

private fun historyCardVariant(item: ClipHistoryItem): HistoryCardVariant =
  when {
    item.type == ClipItemType.Link -> HistoryCardVariant.Link
    item.type == ClipItemType.Image -> HistoryCardVariant.Image
    item.type == ClipItemType.File -> HistoryCardVariant.File
    isCodeVisual(item) -> HistoryCardVariant.Code
    (item.type == ClipItemType.Text || item.type == ClipItemType.RichText) && isImportantVisual(item) -> HistoryCardVariant.Note
    else -> HistoryCardVariant.Text
  }

private fun isCodeVisual(item: ClipHistoryItem): Boolean {
  if (item.type != ClipItemType.Text && item.type != ClipItemType.RichText) return false
  val text = historyFullText(item)
  val nonBlankLines = text.lineSequence().count { it.isNotBlank() }
  return nonBlankLines >= 3 && HistoryCodeMarkerRegex.containsMatchIn(text)
}

private fun historyFullText(item: ClipHistoryItem): String =
  listOf(item.title, item.body)
    .filter { it.isNotBlank() }
    .distinct()
    .joinToString("\n")
    .ifBlank { item.detail }

private fun historyStableSyncText(state: ClipDockUiState): String =
  when {
    state.syncId == "clipdock-home" -> "刚刚同步 12 条 · 3 台设备在线"
    !state.tokenPresent -> "未加入 · 打开设置"
    state.isSyncing -> "正在同步 · ${relativeTimeLabel(state.diagnostics.lastSyncAtMillis)}"
    state.isSyncSetupInFlight -> "正在连接 · 请稍候"
    state.diagnostics.lastSyncAtMillis > 0 -> "已同步 · ${relativeTimeLabel(state.diagnostics.lastSyncAtMillis)}"
    else -> "已加入 · 未同步"
  }

private data class HistoryFooterChip(val label: String, val tone: ClipDockTone)

private fun historyCardIcon(variant: HistoryCardVariant): ClipDockIconKind =
  when (variant) {
    HistoryCardVariant.Image -> ClipDockIconKind.Image
    HistoryCardVariant.Link -> ClipDockIconKind.Link
    HistoryCardVariant.File -> ClipDockIconKind.File
    else -> ClipDockIconKind.Text
  }

private fun historyCardLabel(variant: HistoryCardVariant): String =
  when (variant) {
    HistoryCardVariant.Image -> "图片"
    HistoryCardVariant.Link -> "链接"
    HistoryCardVariant.File -> "文件"
    else -> "文本"
  }

private fun historyCardFooter(item: ClipHistoryItem, variant: HistoryCardVariant): Pair<HistoryFooterChip, HistoryFooterChip> =
  when (variant) {
    HistoryCardVariant.Image ->
      HistoryFooterChip("取回", ClipDockTone.Blue) to HistoryFooterChip(item.detail.ifBlank { "1.2 MB" }, ClipDockTone.Neutral)
    HistoryCardVariant.Link ->
      HistoryFooterChip("复制", ClipDockTone.Green) to HistoryFooterChip("分享", ClipDockTone.Neutral)
    HistoryCardVariant.File ->
      HistoryFooterChip("下载", ClipDockTone.Amber) to HistoryFooterChip(historyFileBadge(item), ClipDockTone.Neutral)
    else ->
      HistoryFooterChip("复制", ClipDockTone.Green) to HistoryFooterChip("固定", ClipDockTone.Neutral)
  }

private fun historyStableClockLabel(item: ClipHistoryItem): String {
  val timeMillis = item.copiedAtMillis
  if (timeMillis <= 0L) return "--:--"
  val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
  return formatter.format(java.util.Date(timeMillis))
}

@Composable
private fun historyHeaderColor(variant: HistoryCardVariant): Color =
  when (variant) {
    HistoryCardVariant.Link -> Color(0xFF22C55E)
    HistoryCardVariant.Text -> Color(0xFF2563EB)
    HistoryCardVariant.File -> Color(0xFFF59E0B)
    else -> ClipTheme.colors.ink2
  }

private fun historyStableTimeLabel(timeMillis: Long): String {
  if (timeMillis <= 0L) return "--"
  val elapsed = System.currentTimeMillis() - timeMillis
  if (elapsed < 0) return "now"
  val minutes = elapsed / 60_000
  return when {
    minutes < 1 -> "now"
    minutes < 60 -> "${minutes}m"
    minutes < 24 * 60 -> "${minutes / 60}h"
    else -> "${minutes / (24 * 60)}d"
  }
}

private fun historyFileBadge(item: ClipHistoryItem): String =
  item.metadataLabel
    .takeIf { it.length in 2..5 }
    ?: item.displayTitle.substringAfterLast('.', missingDelimiterValue = "").uppercase().takeIf { it.length in 2..5 }
    ?: "FILE"

@Composable
private fun DevicesPage(state: ClipDockUiState, onCreateInvite: () -> Unit, onRefresh: () -> Unit) {
  val onlineDevices = state.p2pDevices.filterNot { it.deviceId == state.deviceId }
  val deviceCards =
    listOf(
      DeviceUiCard(
        name = state.deviceName,
        kind = "Android · 本机",
        status = if (state.tokenPresent) "前台运行 · 悬浮球${if (state.overlayEnabled) "开启" else "关闭"}" else "未加入同步空间",
        icon = ClipDockIconKind.Devices,
        tone = ClipDockTone.Blue,
        online = state.tokenPresent,
        caps = listOf("复制", "下载"),
      ),
    ) +
      onlineDevices.map { device ->
        DeviceUiCard(
          name = device.deviceName,
          kind = "在线设备",
          status = "P2P endpoint · ${timeLabel(device.endpoint.updatedAtMillis)}",
          icon = ClipDockIconKind.Devices,
          tone = ClipDockTone.Green,
          online = true,
          caps = listOf("文本", "图片", "文件"),
        )
      }
  LazyVerticalGrid(
    columns = GridCells.Adaptive(164.dp),
    contentPadding = PaddingValues(start = 14.dp, top = 15.dp, end = 14.dp, bottom = 18.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxSize(),
  ) {
    item(span = { GridItemSpan(maxLineSpan) }) {
      ClipDockScreenHeader(
        title = "设备",
        subtitle = "当前账号下的同步设备和连接状态",
        actions = {
          ClipDockIconButton(ClipDockIconKind.More, "刷新设备", onClick = onRefresh, enabled = state.tokenPresent && !state.isSyncSetupInFlight)
          ClipDockIconButton(ClipDockIconKind.Plus, "生成配对码", onClick = onCreateInvite, enabled = state.tokenPresent && !state.isSyncSetupInFlight)
        },
      )
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
      DeviceStatusPanel(
        state = state,
        onlineCount = deviceCards.count { it.online },
        onCreateInvite = onCreateInvite,
      )
    }
    items(deviceCards, key = { it.name }) { card ->
      DeviceCard(card)
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
      Text("最近传输", color = ClipTheme.colors.ink2, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp, start = 2.dp))
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
      ClipDockCard {
        TimelineRow(ClipDockIconKind.Image, "图片缩略图已同步", if (state.tokenPresent) "${state.deviceName} · ${relativeTimeLabel(state.diagnostics.lastSyncAtMillis)}" else "加入同步空间后显示最近传输", "刚刚")
        TimelineRow(ClipDockIconKind.Link, "邀请新设备", state.pairingCode?.let { "$it · ${pairingExpiryText(state)}" } ?: "生成 5 位配对码给新设备加入", if (state.pairingCode == null) "生成" else "刷新")
      }
    }
  }
}

private data class DeviceUiCard(
  val name: String,
  val kind: String,
  val status: String,
  val icon: ClipDockIconKind,
  val tone: ClipDockTone,
  val online: Boolean,
  val caps: List<String>,
)

@Composable
private fun DeviceStatusPanel(state: ClipDockUiState, onlineCount: Int, onCreateInvite: () -> Unit) {
  ClipDockCard(contentPadding = PaddingValues(14.dp), modifier = Modifier.background(Color.Transparent)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ClipDockSymbol(ClipDockIconKind.Cloud, Modifier.size(18.dp), color = ClipTheme.colors.online)
      Text(if (state.tokenPresent) "同步通道正常" else "等待连接服务端", color = ClipTheme.colors.ink, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
    Text(
      if (state.tokenPresent) "${state.deviceName} 在线，最近一次同步${relativeTimeLabel(state.diagnostics.lastSyncAtMillis)}完成。" else "邀请新设备前需要先加入同步空间。",
      color = ClipTheme.colors.ink2,
      fontSize = 13.sp,
      lineHeight = 18.sp,
      fontWeight = FontWeight.Bold,
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
      TimelineInlineMetric(ClipDockIconKind.Devices, "$onlineCount 台在线", "设备可见")
      StatusPill(if (state.tokenPresent) "稳定" else "未连接", if (state.tokenPresent) ClipDockTone.Green else ClipDockTone.Neutral)
    }
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(40.dp)
          .clip(RoundedCornerShape(13.dp))
          .background(ClipTheme.colors.coralSoft)
          .clickable(enabled = state.tokenPresent && !state.isSyncSetupInFlight, onClick = onCreateInvite)
          .padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text("邀请新设备", color = ClipTheme.colors.coral, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold)
      ClipDockSymbol(ClipDockIconKind.Chevron, Modifier.size(16.dp), color = ClipTheme.colors.coral)
    }
  }
}

@Composable
private fun DeviceCard(card: DeviceUiCard) {
  val c = ClipTheme.colors
  ClipDockCard(contentPadding = PaddingValues(13.dp), modifier = Modifier.height(170.dp)) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(card.name, color = c.ink, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(card.kind, color = c.ink2, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      IconTile(card.icon, tone = card.tone)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      Box(Modifier.size(7.dp).clip(CircleShape).background(if (card.online) c.online else c.ink3))
      Text(card.status, color = c.ink2, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
    Spacer(Modifier.weight(1f))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
      card.caps.take(3).forEach { cap -> HistoryMiniChip(cap, ClipDockTone.Neutral) }
    }
  }
}

@Composable
private fun TimelineInlineMetric(icon: ClipDockIconKind, title: String, subtitle: String) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
    IconTile(icon, tone = ClipDockTone.Green, modifier = Modifier.size(34.dp))
    Column {
      Text(title, color = ClipTheme.colors.ink, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold)
      Text(subtitle, color = ClipTheme.colors.ink2, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun TimelineRow(icon: ClipDockIconKind, title: String, subtitle: String, trailing: String) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
    IconTile(icon, tone = ClipDockTone.Green, modifier = Modifier.size(34.dp))
    Column(Modifier.weight(1f)) {
      Text(title, color = ClipTheme.colors.ink, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(subtitle, color = ClipTheme.colors.ink2, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Text(trailing, color = ClipTheme.colors.inkFaint, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
  }
}

@Composable
private fun FilesPage(
  state: ClipDockUiState,
  wifiOnlyBlocked: Boolean,
  onUseItem: (ClipHistoryItem) -> Unit,
  onOpenItemDetail: (String) -> Unit,
) {
  val context = LocalContext.current
  var selectedSegment by remember { mutableStateOf("全部") }
  val fileItems =
    state.items
      .filter { it.type == ClipItemType.Image || it.type == ClipItemType.File }
      .filter {
        when (selectedSegment) {
          "图片" -> it.type == ClipItemType.Image
          "文档" -> it.type == ClipItemType.File
          "下载" -> it.payloadState == PayloadState.Ready && !it.localUri.isNullOrBlank()
          else -> true
        }
      }
  LazyVerticalGrid(
    columns = GridCells.Adaptive(154.dp),
    contentPadding = PaddingValues(start = 14.dp, top = 15.dp, end = 14.dp, bottom = 18.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxSize(),
  ) {
    item(span = { GridItemSpan(maxLineSpan) }) {
      ClipDockScreenHeader(
        title = "文件",
        subtitle = "图片、视频、文档和远程资产",
        actions = {
          ClipDockIconButton(ClipDockIconKind.Search, "搜索文件", onClick = {}, enabled = false)
          ClipDockIconButton(ClipDockIconKind.Download, "下载队列", onClick = {}, enabled = false)
        },
      )
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
      SegmentedControl(listOf("全部", "图片", "视频", "文档", "下载"), selectedSegment, { selectedSegment = it })
    }
    if (fileItems.isEmpty()) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        EmptyState("暂无文件", "图片和文件会从同步历史中自动聚合。", null, null)
      }
    } else {
      items(fileItems, key = { it.stableId }) { item ->
        FileAssetCard(
          item = item,
          p2pEnabled = state.p2pEnabled,
          wifiOnlyBlocked = wifiOnlyBlocked,
          onUseItem = onUseItem,
          onOpenDetail = { onOpenItemDetail(item.stableId) },
          onOpenItem = { openLocalUri(context, item) },
        )
      }
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
      Text("远程资产状态", color = ClipTheme.colors.ink2, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp, start = 2.dp))
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
      ClipDockCard {
        TimelineRow(ClipDockIconKind.Check, "缩略图可直接预览", "原始文件按需下载，避免占用本机空间", "设计态")
        TimelineRow(ClipDockIconKind.Download, "按需下载", if (wifiOnlyBlocked) "仅 Wi-Fi 下载已开启，当前网络不可取回" else "下载队列为空", if (state.wifiOnly) "仅 Wi-Fi" else "空闲")
      }
    }
  }
}

@Composable
private fun FileAssetCard(
  item: ClipHistoryItem,
  p2pEnabled: Boolean,
  wifiOnlyBlocked: Boolean,
  onUseItem: (ClipHistoryItem) -> Unit,
  onOpenDetail: () -> Unit,
  onOpenItem: () -> Unit,
) {
  val c = ClipTheme.colors
  val state = fileActionState(item, p2pEnabled, wifiOnlyBlocked)
  val variant = if (item.type == ClipItemType.File) HistoryCardVariant.File else HistoryCardVariant.Image
  val bitmap by rememberImageBitmap(item.thumbnailUri ?: item.localUri)
  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .height(if (item.type == ClipItemType.File) 226.dp else 190.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(c.sunken)
        .clickable(onClick = { if (state.opensLocalUri) onOpenItem() else onOpenDetail() }),
  ) {
    if (bitmap != null) {
      Image(bitmap = bitmap!!, contentDescription = item.displayTitle, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    } else {
      HistoryMediaFallback(variant = variant, modifier = Modifier.fillMaxSize())
    }
    Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color(0x730F172A), Color.Transparent, Color(0xB80F172A)))))
    Row(
      modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ClipDockSymbol(if (item.type == ClipItemType.Image) ClipDockIconKind.Image else ClipDockIconKind.File, Modifier.size(15.dp), color = Color.White)
        HistoryTypePill(if (item.type == ClipItemType.Image) "图片" else "文档", typeTone(item.type))
      }
      Text(historyStableClockLabel(item), color = Color.White.copy(alpha = 0.86f), fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
    Row(
      modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
      horizontalArrangement = Arrangement.spacedBy(7.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      HistoryActionPill(if (state.primaryLabel == "打开") "打开" else "复制", if (state.primaryLabel == "打开") ClipDockIconKind.Folder else ClipDockIconKind.Copy, ClipDockTone.Green)
      HistoryActionPill("下载", ClipDockIconKind.Download, ClipDockTone.Blue)
    }
  }
}

@Composable
private fun FileRow(
  item: ClipHistoryItem,
  p2pEnabled: Boolean,
  wifiOnlyBlocked: Boolean,
  onUseItem: (ClipHistoryItem) -> Unit,
  onOpenDetail: () -> Unit,
  onOpenItem: () -> Unit,
) {
  val state = fileActionState(item, p2pEnabled, wifiOnlyBlocked)
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    RowCard(
      icon = if (item.type == ClipItemType.Image) ClipDockIconKind.Image else ClipDockIconKind.File,
      title = item.displayTitle,
      subtitle = state.message,
      tone = typeTone(item.type),
      onClick = { if (state.opensLocalUri) onOpenItem() else onOpenDetail() },
    ) {
      ActionChip(
        label = state.primaryLabel,
        enabled = state.primaryEnabled,
        tone = state.tone,
        onClick = {
          when {
            state.opensLocalUri -> onOpenItem()
            state.primaryLabel == "复制" -> onUseItem(item)
            else -> onOpenDetail()
          }
        },
      )
    }
    if (item.transferState == TransferState.Downloading) {
      LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 2.dp), color = ClipTheme.colors.coral)
    }
  }
}

@Composable
private fun SettingsOverviewPage(
  state: ClipDockUiState,
  onSyncNow: () -> Unit,
  onP2pEnabledChange: (Boolean) -> Unit,
  onWifiOnlyChange: (Boolean) -> Unit,
  onOverlayEnabledChange: (Boolean) -> Unit,
  onEncryptionEnabledChange: (Boolean) -> Unit,
  onServerUrlChange: (String) -> Unit,
  onCheckHealth: () -> Unit,
  onCreateSyncSpace: () -> Unit,
  onJoinSyncSpace: (String) -> Unit,
  onOpenSettingsDetail: (SettingsDetailDestination) -> Unit,
) {
  val context = LocalContext.current
  val overlayGranted = Settings.canDrawOverlays(context)
  val notificationGranted =
    Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
  val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
  val batteryIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)
  val keepAliveMissingCount = listOf(overlayGranted, notificationGranted, batteryIgnored).count { !it }
  var syncConfigExpanded by remember { mutableStateOf(false) }
  LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
    item {
      ClipDockScreenHeader(
        title = "设置",
        subtitle = "同步、设备、悬浮球与隐私",
        actions = { ClipDockIconButton(ClipDockIconKind.Search, "设置搜索", onClick = {}, enabled = false) },
      )
    }
    item { SyncStatusHero(state, onSyncNow) }

    item {
      val c = ClipTheme.colors
      SettingsGroup("同步") {
        SettingsToggleRow(
          icon = ClipDockIconKind.Cloud,
          accent = c.accentImage,
          label = "自动同步",
          checked = state.p2pEnabled && state.tokenPresent,
          divider = true,
          onCheckedChange = { enabled ->
            if (enabled) {
              if (state.tokenPresent) onP2pEnabledChange(true) else syncConfigExpanded = true
            } else {
              onP2pEnabledChange(false)
            }
          },
        )
        SettingsToggleRow(
          icon = ClipDockIconKind.Wifi,
          accent = c.accentLink,
          label = "仅 Wi-Fi 下载原文件",
          checked = state.wifiOnly,
          divider = true,
          onCheckedChange = onWifiOnlyChange,
        )
        SettingsValueRow(
          icon = ClipDockIconKind.Download,
          accent = c.accentFile,
          label = "远程文件下载",
          value = "按需",
          divider = false,
        )
      }
    }
    if (!state.tokenPresent && syncConfigExpanded) {
      item {
        SyncServerConfigCard(
          state = state,
          onServerUrlChange = onServerUrlChange,
          onCheckHealth = onCheckHealth,
          onCreateSyncSpace = onCreateSyncSpace,
          onJoinSyncSpace = onJoinSyncSpace,
        )
      }
    }

    item {
      val c = ClipTheme.colors
      SettingsGroup("设备与配对") {
        SettingsValueRow(
          icon = ClipDockIconKind.Plus,
          accent = c.accentImage,
          label = "配对新设备",
          value = "",
          divider = true,
          onClick = { onOpenSettingsDetail(SettingsDetailDestination.Pairing) },
        )
        SettingsValueRow(
          icon = ClipDockIconKind.Shield,
          accent = c.accentFile,
          label = "保活权限",
          value = if (keepAliveMissingCount == 0) "已完成" else "${keepAliveMissingCount} 项",
          divider = false,
          onClick = { onOpenSettingsDetail(SettingsDetailDestination.KeepAlive) },
        )
      }
    }

    item {
      val c = ClipTheme.colors
      SettingsGroup("悬浮球") {
        SettingsToggleRow(
          icon = ClipDockIconKind.Window,
          accent = c.accentLink,
          label = "启用悬浮球",
          checked = state.overlayEnabled && overlayGranted,
          divider = true,
          onCheckedChange = { enabled ->
            if (enabled) {
              if (Settings.canDrawOverlays(context)) {
                onOverlayEnabledChange(true)
                startFloatingOverlay(context)
              } else {
                openOverlayPermission(context)
              }
            } else {
              onOverlayEnabledChange(false)
              stopFloatingOverlay(context)
            }
          },
        )
        SettingsValueRow(
          icon = ClipDockIconKind.More,
          accent = c.accentText,
          label = "外观与位置",
          value = if (state.overlayEnabled && overlayGranted) "已启用" else "关闭",
          divider = false,
          onClick = { onOpenSettingsDetail(SettingsDetailDestination.FloatingBall) },
        )
      }
    }

    item {
      val c = ClipTheme.colors
      SettingsGroup("隐私与安全") {
        SettingsToggleRow(
          icon = ClipDockIconKind.Lock,
          accent = c.accentImage,
          label = "敏感内容保护",
          checked = state.encryptionEnabled,
          divider = false,
          onCheckedChange = onEncryptionEnabledChange,
        )
      }
    }

    item {
      val c = ClipTheme.colors
      SettingsGroup("存储与高级") {
        SettingsValueRow(
          icon = ClipDockIconKind.Trash,
          accent = c.accentFile,
          label = "清理历史与缓存",
          value = "",
          divider = true,
        )
        SettingsValueRow(
          icon = ClipDockIconKind.Server,
          accent = c.accentText,
          label = "连接维护",
          value = "",
          divider = false,
          onClick = { onOpenSettingsDetail(SettingsDetailDestination.ServerAdvanced) },
        )
      }
    }
  }
}

@Composable
private fun SyncServerConfigCard(
  state: ClipDockUiState,
  onServerUrlChange: (String) -> Unit,
  onCheckHealth: () -> Unit,
  onCreateSyncSpace: () -> Unit,
  onJoinSyncSpace: (String) -> Unit,
) {
  val c = ClipTheme.colors
  var pairingCode by remember { mutableStateOf("") }
  val canRunSetup = !state.isSyncSetupInFlight
  val hasSyncRegistration = state.tokenPresent || !state.syncId.isNullOrBlank() || !state.deviceId.isNullOrBlank()
  ClipDockCard {
    Text("连接同步服务器", style = MaterialTheme.typography.titleSmall)
    Text("配置服务端并加入同步空间后，自动同步才会开启", style = MaterialTheme.typography.bodySmall, color = c.ink2)
    OutlinedTextField(value = state.serverUrl, onValueChange = onServerUrlChange, label = { Text("服务端地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedButton(onClick = onCheckHealth, enabled = canRunSetup, modifier = Modifier.fillMaxWidth()) {
      Text(if (state.connectionStatus == "可连接") "连接正常 · 重新检查" else "检查连接")
    }
    SettingDivider()
    OutlinedTextField(value = pairingCode, onValueChange = { pairingCode = it.take(5).uppercase() }, label = { Text("5 位配对码（加入已有空间）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Button(onClick = { onJoinSyncSpace(pairingCode) }, enabled = pairingCode.length == 5 && canRunSetup, modifier = Modifier.fillMaxWidth()) { Text("加入空间") }
    Button(onClick = onCreateSyncSpace, enabled = !hasSyncRegistration && canRunSetup, modifier = Modifier.fillMaxWidth()) { Text("在本机创建新空间") }
    state.diagnostics.lastError?.let { error ->
      Text(error, style = MaterialTheme.typography.bodySmall, color = Color(0xFFDC2626))
    }
  }
}

@Composable
private fun SettingsSectionTitle(text: String) {
  Text(
    text,
    color = ClipTheme.colors.inkFaint,
    fontSize = 12.sp,
    lineHeight = 15.sp,
    fontWeight = FontWeight.ExtraBold,
    modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 2.dp),
  )
}

@Composable
private fun SyncStatusHero(state: ClipDockUiState, onSyncNow: () -> Unit) {
  val c = ClipTheme.colors
  val connected = state.tokenPresent
  val onlineCount = state.p2pDevices.size + if (connected) 1 else 0
  Column(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(c.surface)
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      IconTile(ClipDockIconKind.Cloud, tone = ClipDockTone.Neutral)
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          if (connected) "同步正常运行" else "未连接同步空间",
          color = c.ink,
          fontSize = 15.sp,
          lineHeight = 20.sp,
          fontWeight = FontWeight.ExtraBold,
        )
        Text(
          if (connected) "${state.deviceName} · 最近同步 ${relativeTimeLabel(state.diagnostics.lastSyncAtMillis)}" else "前往设备与配对加入空间",
          color = c.ink2,
          fontSize = 12.sp,
          lineHeight = 16.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      StatusPill(if (connected) "已连接" else "未设置", if (connected) ClipDockTone.Green else ClipDockTone.Neutral)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      HeroMetric("$onlineCount 台", "在线设备", Modifier.weight(1f))
      HeroMetric("0 项", "待上传", Modifier.weight(1f))
      Box(
        Modifier
          .weight(1.15f)
          .height(46.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(if (connected && !state.isSyncing) c.coral else c.surface)
          .clickable(enabled = connected && !state.isSyncing, onClick = onSyncNow),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          if (state.isSyncing) "同步中…" else "立即同步",
          color = if (connected && !state.isSyncing) Color.White else c.ink2,
          fontSize = 13.sp,
          fontWeight = FontWeight.ExtraBold,
        )
      }
    }
  }
}

@Composable
private fun HeroMetric(value: String, label: String, modifier: Modifier = Modifier) {
  val c = ClipTheme.colors
  Column(
    modifier
      .clip(RoundedCornerShape(12.dp))
      .background(c.surface)
      .padding(horizontal = 11.dp, vertical = 7.dp),
    verticalArrangement = Arrangement.spacedBy(1.dp),
  ) {
    Text(value, color = c.ink, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold)
    Text(label, color = c.ink2, fontSize = 10.sp, lineHeight = 13.sp)
  }
}

@Composable
private fun PairingPage(
  state: ClipDockUiState,
  onBack: () -> Unit,
  onDeviceNameChange: (String) -> Unit,
  onCreateSyncSpace: () -> Unit,
  onJoinSyncSpace: (String) -> Unit,
  onCreateInvite: () -> Unit,
) {
  var pairingCode by remember { mutableStateOf("") }
  val hasSyncRegistration = state.tokenPresent || !state.syncId.isNullOrBlank() || !state.deviceId.isNullOrBlank()
  val canRunSetup = !state.isSyncSetupInFlight
  LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
    item {
      ClipDockScreenHeader(
        title = "配对新设备",
        subtitle = "创建或加入同步空间",
        actions = { ClipDockIconButton(ClipDockIconKind.Check, "返回设置", onClick = onBack) },
      )
    }
    item {
      ClipDockCard {
        Text("设备名称", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(value = state.deviceName, onValueChange = onDeviceNameChange, label = { Text("本机显示名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
      }
    }
    item {
      ClipDockCard {
        Text("加入已有空间", style = MaterialTheme.typography.titleSmall)
        Text("输入另一台设备生成的 5 位配对码", style = MaterialTheme.typography.bodySmall, color = ClipTheme.colors.ink2)
        PairingCodeInput(code = pairingCode, onCodeChange = { pairingCode = it }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onJoinSyncSpace(pairingCode) }, enabled = pairingCode.length == 5 && canRunSetup, modifier = Modifier.fillMaxWidth()) { Text("加入空间") }
      }
    }
    item {
      ClipDockCard {
        Text("创建新空间", style = MaterialTheme.typography.titleSmall)
        Text(if (hasSyncRegistration) "本机已在同步空间中" else "在本机创建一个新的同步空间", style = MaterialTheme.typography.bodySmall, color = ClipTheme.colors.ink2)
        Button(onClick = onCreateSyncSpace, enabled = !hasSyncRegistration && canRunSetup, modifier = Modifier.fillMaxWidth()) { Text("创建空间") }
      }
    }
    item {
      ClipDockCard {
        Text("邀请其他设备", style = MaterialTheme.typography.titleSmall)
        Text(state.pairingCode?.let { "当前配对码：$it" } ?: "生成一个 5 位配对码给新设备使用", style = MaterialTheme.typography.bodySmall, color = ClipTheme.colors.ink2)
        OutlinedButton(onClick = onCreateInvite, enabled = state.tokenPresent && canRunSetup, modifier = Modifier.fillMaxWidth()) { Text(if (state.pairingCode == null) "生成配对码" else "刷新配对码") }
      }
    }
  }
}

@Composable
private fun ServerAdvancedPage(
  state: ClipDockUiState,
  onBack: () -> Unit,
  onServerUrlChange: (String) -> Unit,
  onCheckHealth: () -> Unit,
  onRefreshInfo: () -> Unit,
  onSyncNow: () -> Unit,
) {
  val canRunSetup = !state.isSyncSetupInFlight
  LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
    item {
      ClipDockScreenHeader(
        title = "服务器与高级",
        subtitle = "服务端地址与连接维护",
        actions = { ClipDockIconButton(ClipDockIconKind.Check, "返回设置", onClick = onBack) },
      )
    }
    item {
      ClipDockCard {
        Text("服务端地址", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(value = state.serverUrl, onValueChange = onServerUrlChange, label = { Text("服务端地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = onCheckHealth, enabled = canRunSetup, modifier = Modifier.fillMaxWidth()) { Text("检查连接") }
      }
    }
    item {
      ClipDockCard {
        Text("连接维护", style = MaterialTheme.typography.titleSmall)
        Text("同步空间：${state.syncId ?: "未加入"}", style = MaterialTheme.typography.bodySmall, color = ClipTheme.colors.ink2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = onRefreshInfo, enabled = state.tokenPresent && canRunSetup, modifier = Modifier.weight(1f)) { Text("刷新能力") }
          Button(onClick = onSyncNow, enabled = state.tokenPresent && !state.isSyncing && canRunSetup, modifier = Modifier.weight(1f)) { Text("立即同步") }
        }
      }
    }
  }
}

@Composable
private fun KeepAlivePage(state: ClipDockUiState, onBack: () -> Unit) {
  val context = LocalContext.current
  val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
  val overlayGranted = Settings.canDrawOverlays(context)
  val notificationGranted =
    Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
  val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
  val batteryIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)
  val missingPermissionCount = listOf(overlayGranted, notificationGranted, batteryIgnored).count { !it }
  LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
    item {
      ClipDockScreenHeader(
        title = "保活权限",
        subtitle = "确保悬浮球和实时同步可靠运行",
        actions = { ClipDockIconButton(ClipDockIconKind.Check, "返回设置", onClick = onBack) },
      )
    }
    item {
      ClipDockHeroBanner(
        icon = ClipDockIconKind.Shield,
        title = if (missingPermissionCount == 0) "关键权限已处理" else "还差 ${missingPermissionCount} 项建议权限",
        subtitle = "不影响基础使用，但会影响后台实时同步时间",
        actionLabel = if (missingPermissionCount == 0) "完成" else "检查",
        actionTone = if (missingPermissionCount == 0) ClipDockTone.Green else ClipDockTone.Amber,
      )
    }
    item {
      RowCard(ClipDockIconKind.Window, "全局悬浮窗", "允许桌面显示悬浮球", tone = ClipDockTone.Green, onClick = { openOverlayPermission(context) }) {
        StatusPill(if (overlayGranted) "已授权" else "去开启", if (overlayGranted) ClipDockTone.Green else ClipDockTone.Amber)
      }
    }
    item {
      RowCard(ClipDockIconKind.Bell, "通知权限", if (Build.VERSION.SDK_INT >= 33) "用于前台同步状态提示" else "Android 13 以下内置可用", tone = ClipDockTone.Blue, onClick = {
        if (Build.VERSION.SDK_INT >= 33) {
          notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
          openNotificationSettings(context)
        }
      }) {
        StatusPill(if (notificationGranted) "已开启" else "去开启", if (notificationGranted) ClipDockTone.Green else ClipDockTone.Blue)
      }
    }
    item {
      RowCard(ClipDockIconKind.Battery, "忽略电池优化", "降低 Doze/App Standby 中断概率", tone = ClipDockTone.Amber, onClick = { openBatteryOptimizationSettings(context) }) {
        StatusPill(if (batteryIgnored) "已忽略" else "建议", if (batteryIgnored) ClipDockTone.Green else ClipDockTone.Amber)
      }
    }
    item {
      RowCard(ClipDockIconKind.Play, "厂商后台保护", "MIUI/ColorOS 等需要手动允许自启动", tone = ClipDockTone.Amber, onClick = { openVendorBackgroundSettings(context) }) {
        StatusPill("去设置", ClipDockTone.Amber)
      }
    }
    item {
      RowCard(ClipDockIconKind.Lock, "剪贴板隐私", "敏感内容写入使用系统内置标记", tone = ClipDockTone.Green) {
        StatusPill("内置", ClipDockTone.Green)
      }
    }
  }
}

@Composable
private fun FloatingBallSettingsPage(
  state: ClipDockUiState,
  onBack: () -> Unit,
  onOverlayEnabledChange: (Boolean) -> Unit,
  onOverlayClickActionChange: (OverlayClickAction) -> Unit,
  onOverlaySnapEdgeChange: (OverlaySnapEdge) -> Unit,
  onOverlaySizeChange: (Int) -> Unit,
  onOverlayIdleOpacityChange: (Int) -> Unit,
  onOverlayVerticalFractionChange: (Float) -> Unit,
) {
  val context = LocalContext.current
  LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
    item {
      ClipDockScreenHeader(
        title = "悬浮球",
        subtitle = "点击复制最新，内滑展开侧栏",
        actions = { ClipDockIconButton(ClipDockIconKind.More, "返回设置", onClick = onBack) },
      )
    }
    item { FloatingPreview(state) }
    item {
      SettingGroup {
        SwitchSettingRow(
          ClipDockIconKind.Window,
          "启用悬浮球",
          "显示在系统桌面边缘",
          state.overlayEnabled,
          onCheckedChange = { enabled ->
            if (enabled) {
              if (Settings.canDrawOverlays(context)) {
                onOverlayEnabledChange(true)
                startFloatingOverlay(context)
              } else {
                openOverlayPermission(context)
              }
            } else {
              onOverlayEnabledChange(false)
              stopFloatingOverlay(context)
            }
          },
        )
        SettingDivider()
        SettingRow(ClipDockIconKind.Copy, "点击把手", "复制最新内容") {
          StatusPill("点击", ClipDockTone.Green)
        }
        LaunchedEffect(Unit) {
          onOverlayClickActionChange(OverlayClickAction.QuickSyncCopy)
        }
        SettingDivider()
        SettingRow(ClipDockIconKind.Window, "内滑把手", "展开侧边栏，选条目或同步") {
          StatusPill("展开", ClipDockTone.Blue)
        }
      }
    }
    item {
      ClipDockCard {
        Text("停靠方向", style = MaterialTheme.typography.titleSmall)
        Text("当前吸附到${if (state.overlaySnapEdge == OverlaySnapEdge.Right) "右侧" else "左侧"}边缘", style = MaterialTheme.typography.bodySmall, color = ClipTheme.colors.ink2)
        SegmentedControl(
          options = listOf("左侧", "右侧"),
          selected = if (state.overlaySnapEdge == OverlaySnapEdge.Left) "左侧" else "右侧",
          onSelected = { label -> onOverlaySnapEdgeChange(if (label == "左侧") OverlaySnapEdge.Left else OverlaySnapEdge.Right) },
        )
      }
    }
    item {
      SliderSettingCard(
        title = "尺寸",
        subtitle = "${state.overlaySizeDp} dp · 兼顾可点按和遮挡范围",
        value = state.overlaySizeDp.toFloat(),
        onValueChange = { onOverlaySizeChange(it.toInt()) },
        valueRange = 52f..72f,
        steps = 19,
      )
    }
    item {
      SliderSettingCard(
        title = "闲置透明度",
        subtitle = "${state.overlayIdleOpacityPercent}% · 不完全隐藏，保持可发现",
        value = state.overlayIdleOpacityPercent.toFloat(),
        onValueChange = { onOverlayIdleOpacityChange(it.toInt()) },
        valueRange = 45f..100f,
        steps = 54,
      )
    }
    item {
      SliderSettingCard(
        title = "垂直位置",
        subtitle = "${(state.overlayVerticalFraction * 100).toInt()}% · 拖动后自动保存",
        value = state.overlayVerticalFraction,
        onValueChange = onOverlayVerticalFractionChange,
        valueRange = 0f..1f,
        steps = 19,
      )
    }
  }
}

@Composable
private fun FloatingPreview(state: ClipDockUiState) {
  val c = ClipTheme.colors
  val latestItem = state.items.firstOrNull()
  Box(
    Modifier
      .fillMaxWidth()
      .height(112.dp)
      .clip(RoundedCornerShape(20.dp))
      .background(c.surface)
      .padding(16.dp),
  ) {
    ClipDockCard(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 66.dp).width(190.dp)) {
      Text(if (latestItem == null) "暂无可复制内容" else "最近同步内容", style = MaterialTheme.typography.labelMedium)
      Text(
        latestItem?.let { it.displayTitle.ifBlank { it.displayBody } } ?: "同步后会显示真实最近记录",
        style = MaterialTheme.typography.labelSmall,
        color = c.ink2,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
    val right = state.overlaySnapEdge == OverlaySnapEdge.Right
    Box(
      Modifier
        .align(if (right) Alignment.CenterEnd else Alignment.CenterStart)
        .width(8.dp)
        .height(state.overlaySizeDp.dp)
        .clip(
          if (right) RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp) else RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp),
        )
        .background(c.coral.copy(alpha = state.overlayIdleOpacityPercent / 100f)),
    )
  }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
  ClipDockCard(modifier = modifier.height(58.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp)) {
    Text(value, style = MaterialTheme.typography.titleMedium, color = ClipTheme.colors.ink)
    Text(label, style = MaterialTheme.typography.labelSmall, color = ClipTheme.colors.ink2)
  }
}

@Composable
private fun EmptyState(title: String, subtitle: String, actionLabel: String?, onAction: (() -> Unit)?) {
  val c = ClipTheme.colors
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = c.surface,
    shadowElevation = 2.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(title, style = MaterialTheme.typography.titleMedium, color = c.ink)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.ink2)
      if (actionLabel != null && onAction != null) {
        Spacer(Modifier.height(4.dp))
        Surface(
          onClick = onAction,
          shape = RoundedCornerShape(999.dp),
          color = c.coral,
        ) {
          Text(actionLabel, color = Color.White, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), style = MaterialTheme.typography.labelLarge)
        }
      }
    }
  }
}

@Composable
private fun FeedbackBanner(message: String, isError: Boolean) {
  Surface(color = if (isError) Color(0xFFFEE2E2) else ClipTheme.colors.accentLink.bg, contentColor = if (isError) Color(0xFFDC2626) else ClipTheme.colors.accentLink.fg) {
    Text(message, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun rememberImageBitmap(uri: String?): androidx.compose.runtime.State<ImageBitmap?> {
  val context = LocalContext.current
  return produceState<ImageBitmap?>(initialValue = null, uri) {
    value =
      if (uri.isNullOrBlank()) {
        null
      } else {
        withContext(Dispatchers.IO) {
          runCatching {
              context.contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
              }
            }
            .getOrNull()
        }
      }
  }
}

internal fun historyCardTestTag(stableId: String): String = "history-card-$stableId"

internal data class HistoryDetailDisplay(
  val previewIcon: ClipDockIconKind,
  val previewUri: String?,
  val title: String,
  val subtitle: String,
  val status: String,
  val source: String,
  val contentType: String,
  val timeLabel: String,
  val metaRows: List<HistoryDetailMetaRow>,
)

internal data class HistoryDetailMetaRow(
  val label: String,
  val value: String,
)

internal fun historyDetailDisplay(item: ClipHistoryItem): HistoryDetailDisplay {
  val source = item.sourceName?.takeIf { it.isNotBlank() } ?: "未知来源"
  val contentType = historyDetailContentType(item)
  val timeLabel = relativeTimeLabel(item.copiedAtMillis)
  val status = historyDetailStatus(item)
  val previewIcon =
    when (item.type) {
      ClipItemType.Image -> ClipDockIconKind.Image
      ClipItemType.File -> ClipDockIconKind.File
      ClipItemType.Link -> ClipDockIconKind.Link
      ClipItemType.Color -> ClipDockIconKind.Text
      ClipItemType.RichText,
      ClipItemType.Text -> ClipDockIconKind.Text
      ClipItemType.Unknown -> ClipDockIconKind.Alert
    }
  val previewUri =
    when (item.type) {
      ClipItemType.Link -> item.linkPreviewUri
      ClipItemType.Image -> item.thumbnailUri ?: item.localUri
      ClipItemType.File -> item.thumbnailUri
      else -> null
    }
  return HistoryDetailDisplay(
    previewIcon = previewIcon,
    previewUri = previewUri,
    title = item.displayTitle.ifBlank { item.type.label },
    subtitle = listOf(source, contentType, timeLabel).filter(String::isNotBlank).joinToString(" · "),
    status = status,
    source = source,
    contentType = contentType,
    timeLabel = timeLabel,
    metaRows =
      listOf(
        HistoryDetailMetaRow("状态", status),
        HistoryDetailMetaRow("来源设备", source),
        HistoryDetailMetaRow("内容类型", contentType),
        HistoryDetailMetaRow("时间", timeLabel),
      ),
  )
}

internal data class FileActionState(
  val primaryLabel: String,
  val message: String,
  val primaryEnabled: Boolean,
  val opensLocalUri: Boolean,
  val tone: ClipDockTone,
)

internal fun fileActionState(item: ClipHistoryItem, p2pEnabled: Boolean, wifiOnlyBlocked: Boolean): FileActionState =
  when {
    item.payloadState == PayloadState.Ready && !item.localUri.isNullOrBlank() ->
      FileActionState("打开", "已缓存 · ${item.body.ifBlank { item.detail.ifBlank { item.type.label } }}", primaryEnabled = true, opensLocalUri = true, tone = ClipDockTone.Green)
    item.assetId.isNullOrBlank() ->
      FileActionState("不可用", "缺少 assetId，无法取回", primaryEnabled = false, opensLocalUri = false, tone = ClipDockTone.Neutral)
    !p2pEnabled ->
      FileActionState("不可用", "P2P 未开启，无法取回远端内容", primaryEnabled = false, opensLocalUri = false, tone = ClipDockTone.Neutral)
    wifiOnlyBlocked ->
      FileActionState("等待 Wi-Fi", "仅 Wi-Fi 下载已开启，当前网络不可取回", primaryEnabled = false, opensLocalUri = false, tone = ClipDockTone.Amber)
    item.transferState == TransferState.DiscoveringPeer ->
      FileActionState("查找", "正在查找可用设备", primaryEnabled = false, opensLocalUri = false, tone = ClipDockTone.Blue)
    item.transferState == TransferState.Downloading ->
      FileActionState("下载中", "正在下载，进度由 P2P 传输完成后更新", primaryEnabled = false, opensLocalUri = false, tone = ClipDockTone.Blue)
    item.transferState == TransferState.Failed ->
      FileActionState("重试", "没有可用提供方或上次下载失败", primaryEnabled = true, opensLocalUri = false, tone = ClipDockTone.Amber)
    else ->
      FileActionState("取回", "远端内容尚未下载到本机", primaryEnabled = true, opensLocalUri = false, tone = ClipDockTone.Blue)
  }

private fun imageCacheSummary(item: ClipHistoryItem): String =
  when {
    item.payloadState == PayloadState.Ready && !item.localUri.isNullOrBlank() -> "原图已下载 · 自动使用清晰图片"
    item.transferState == TransferState.Downloading -> "正在下载原图 · 完成后自动切换清晰图"
    item.transferState == TransferState.DiscoveringPeer -> "正在查找来源设备 · 当前显示缩略图"
    item.thumbnailUri != null -> "原图保留在远端 · 当前显示模糊缩略图"
    else -> "远端原图未下载"
  }

private fun imageDrawerSubtitle(item: ClipHistoryItem): String {
  val source = item.sourceName?.takeIf(String::isNotBlank) ?: "未知来源"
  val time = relativeTimeLabel(item.copiedAtMillis)
  val sizeOrState =
    when {
      item.payloadState == PayloadState.Ready && !item.localUri.isNullOrBlank() -> item.localUri
      item.detail.isNotBlank() && !item.detail.startsWith("content://") -> imageDetailValueLabel(item.detail)
      item.thumbnailByteCount != null -> byteCountLabel(item.thumbnailByteCount).ifBlank { historyDetailStatus(item) }
      else -> historyDetailStatus(item)
    }
  return listOf(source, time, sizeOrState).filter(String::isNotBlank).joinToString(" · ")
}

private fun imageOriginalDimensionsLabel(item: ClipHistoryItem): String =
  if (item.thumbnailWidth != null && item.thumbnailHeight != null) {
    "${item.thumbnailWidth} x ${item.thumbnailHeight}"
  } else {
    "原图尺寸待取回"
  }

private fun imageOriginalSizeLabel(item: ClipHistoryItem): String =
  when {
    item.detail.isNotBlank() && !item.detail.startsWith("content://") -> imageDetailValueLabel(item.detail)
    item.thumbnailByteCount != null -> byteCountLabel(item.thumbnailByteCount).ifBlank { "原图大小待取回" }
    else -> "原图大小待取回"
  }

private fun imageDetailValueLabel(value: String): String {
  val trimmed = value.trim()
  val bytes = trimmed.toLongOrNull()
  return if (bytes != null) byteCountLabel(bytes).ifBlank { trimmed } else trimmed
}

private fun imageLocalCacheLabel(item: ClipHistoryItem): String =
  when {
    item.payloadState == PayloadState.Ready && !item.localUri.isNullOrBlank() -> "已下载到 app-owned cache"
    item.transferState == TransferState.Downloading -> "正在写入本机缓存"
    item.transferState == TransferState.DiscoveringPeer -> "等待来源设备"
    else -> "未下载"
  }

private fun imageOriginalStateLabel(item: ClipHistoryItem): String =
  when {
    item.payloadState == PayloadState.Ready && !item.localUri.isNullOrBlank() -> "本机可复制"
    item.transferState == TransferState.Downloading -> "下载中"
    item.transferState == TransferState.DiscoveringPeer -> "查找来源"
    item.transferState == TransferState.Failed || item.payloadState == PayloadState.Failed -> "取回失败"
    item.assetId.isNullOrBlank() -> "远端不可用"
    else -> "远端"
  }

private fun imageTransferTitle(item: ClipHistoryItem): String =
  when {
    item.payloadState == PayloadState.Ready && !item.localUri.isNullOrBlank() -> "清晰原图已就绪"
    item.transferState == TransferState.DiscoveringPeer -> "正在查找 P2P 来源"
    item.transferState == TransferState.Downloading -> "正在取回原图"
    item.transferState == TransferState.Failed || item.payloadState == PayloadState.Failed -> "取回失败"
    item.assetId.isNullOrBlank() -> "远端资产不可用"
    else -> "原图在远端设备"
  }

private fun imageTransferMessage(item: ClipHistoryItem, actions: MobileV4DetailActions): String =
  when {
    item.payloadState == PayloadState.Ready && !item.localUri.isNullOrBlank() -> "复制时会直接使用本机原图，也可以只清理本机缓存保留同步记录。"
    item.transferState == TransferState.Downloading -> "P2P 下载完成后，本页会从模糊缩略图自动切换为本机清晰原图。"
    item.transferState == TransferState.DiscoveringPeer -> "正在根据 asset 查询可提供原图的设备。"
    item.thumbnailUri != null && actions.copyThumbnail.enabled -> "可先复制缩略图；选择取回或仅下载后会写入本机缓存。"
    else -> actions.primary.message
  }

private fun imageThumbnailDescription(item: ClipHistoryItem): String {
  val dimensions =
    if (item.thumbnailWidth != null && item.thumbnailHeight != null) {
      "${item.thumbnailWidth} x ${item.thumbnailHeight}"
    } else {
      "尺寸未知"
    }
  val bytes = byteCountLabel(item.thumbnailByteCount)
  val mime = item.thumbnailMimeType?.takeIf(String::isNotBlank) ?: "缩略图 MIME 未知"
  return listOf(dimensions, bytes, mime).filter(String::isNotBlank).joinToString(" · ")
}

private fun byteCountLabel(bytes: Long?): String =
  when {
    bytes == null || bytes <= 0L -> ""
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    else -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
  }

private fun shortIdentifier(value: String): String =
  value
    .removePrefix("blake3:")
    .takeIf(String::isNotBlank)
    ?.let { if (it.length <= 16) it else "${it.take(10)}...${it.takeLast(6)}" }
    ?: "无"

private fun historyDetailStatus(item: ClipHistoryItem): String =
  when {
    item.transferState == TransferState.DiscoveringPeer -> "正在查找来源"
    item.transferState == TransferState.Downloading -> "正在下载"
    item.transferState == TransferState.Failed || item.payloadState == PayloadState.Failed -> "取回失败"
    mobileV4HasLocalCopySemantics(item) -> "已可复制"
    item.type == ClipItemType.Image || item.type == ClipItemType.File ->
      if (item.assetId.isNullOrBlank()) "远端不可用" else "远端可取回"
    item.type == ClipItemType.Unknown -> "暂不可用"
    else -> "暂不可用"
  }

private fun historyDetailContentType(item: ClipHistoryItem): String =
  when (item.type) {
    ClipItemType.Image -> historyMimeLabel(item.body, "图片")
    ClipItemType.File -> historyMimeLabel(item.body, "文件")
    ClipItemType.Link -> "链接"
    ClipItemType.Color -> "颜色"
    ClipItemType.RichText -> "富文本"
    ClipItemType.Text -> "文字"
    ClipItemType.Unknown -> "未知类型"
  }

private fun historyMimeLabel(value: String, fallback: String): String {
  val mime = value.trim().takeIf { it.isNotBlank() } ?: return fallback
  if (!mime.contains('/')) return mime
  val subtype = mime.substringAfter('/').substringBefore(';').substringAfterLast('.').uppercase()
  return subtype.takeIf { it.isNotBlank() }?.let { "$it $fallback" } ?: fallback
}

internal fun historyActionLabel(item: ClipHistoryItem): String =
  when (item.transferState) {
    TransferState.DiscoveringPeer -> "查找"
    TransferState.Downloading -> "下载中"
    TransferState.Failed -> "重试"
    TransferState.Ready -> "复制"
    TransferState.Idle ->
      when {
        item.needsRemotePayload && item.type == ClipItemType.Image -> "取回"
        item.needsRemotePayload -> "下载"
        else -> "复制"
      }
  }

private val ClipHistoryItem.displayTitle: String
  get() =
    when (type) {
      ClipItemType.Image -> title.takeUnless { it == "[图片]" } ?: body.takeIf { it.isNotBlank() && !it.startsWith("image/") } ?: "图片内容"
      else -> title.ifBlank { compactText }
    }

private val ClipHistoryItem.displayBody: String
  get() =
    when (type) {
      ClipItemType.Image -> body.takeIf { it.isNotBlank() } ?: detail
      else -> body
    }

private val ClipHistoryItem.metadataLabel: String
  get() =
    detail
      .takeUnless { it.isBlank() }
      ?.takeUnless { sourceName != null && it == sourceName }
      ?.let { if (it.length <= 8) it else it.substringAfterLast('.').takeIf { ext -> ext.length in 2..5 }?.uppercase() ?: it.take(7) + "..." }
      ?: ""

private fun relativeTimeLabel(timeMillis: Long): String {
  if (timeMillis <= 0L) return "未同步"
  val elapsed = System.currentTimeMillis() - timeMillis
  if (elapsed < 0) return "刚刚"
  val minutes = elapsed / 60_000
  return when {
    minutes < 1 -> "刚刚"
    minutes < 60 -> "${minutes} 分钟前"
    minutes < 24 * 60 -> "${minutes / 60} 小时前"
    else -> "${minutes / (24 * 60)} 天前"
  }
}

private fun pairingExpiryText(state: ClipDockUiState): String {
  val expiresAt = state.pairingExpiresAtMillis ?: return "未获取"
  val remainingSeconds = (expiresAt - System.currentTimeMillis()) / 1_000
  return if (remainingSeconds <= 0) "已过期" else "约 ${maxOf(1, (remainingSeconds + 59) / 60)} 分钟后过期"
}

private fun timeLabel(timeMillis: Long): String {
  if (timeMillis <= 0L) return "--:--"
  val elapsed = System.currentTimeMillis() - timeMillis
  val minutes = elapsed / 60_000
  return when {
    elapsed < 0 -> "刚刚"
    minutes < 1 -> "刚刚"
    minutes < 60 -> "${minutes} 分钟前"
    minutes < 24 * 60 -> "${minutes / 60} 小时前"
    else -> "${minutes / (24 * 60)} 天前"
  }
}

private fun typeTone(type: ClipItemType): ClipDockTone =
  when (type) {
    ClipItemType.Image -> ClipDockTone.Blue
    ClipItemType.File -> ClipDockTone.Amber
    ClipItemType.Link -> ClipDockTone.Blue
    ClipItemType.Color -> ClipDockTone.Amber
    ClipItemType.RichText -> ClipDockTone.Green
    ClipItemType.Text -> ClipDockTone.Neutral
    ClipItemType.Unknown -> ClipDockTone.Neutral
  }

private fun isWifiConnected(context: Context): Boolean {
  val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  val network = manager.activeNetwork ?: return false
  val capabilities = manager.getNetworkCapabilities(network) ?: return false
  return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}

private fun openLocalUri(context: Context, item: ClipHistoryItem) {
  val uri = item.localUri?.let(Uri::parse) ?: return
  val intent =
    Intent(Intent.ACTION_VIEW)
      .setDataAndType(uri, item.body.takeIf { it.contains("/") } ?: "*/*")
      .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
  runCatching { context.startActivity(intent) }.recoverCatching { openAppSettings(context) }
}

private fun openOverlayPermission(context: Context) {
  val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
  startActivityOrAppSettings(context, intent)
}

private fun openNotificationSettings(context: Context) {
  val intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
      .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
  startActivityOrAppSettings(context, intent)
}

private fun openBatteryOptimizationSettings(context: Context) {
  startActivityOrAppSettings(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
}

private fun openVendorBackgroundSettings(context: Context) {
  val candidates =
    listOf(
      Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
      Intent().setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
      Intent().setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
      Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
    )
  val resolvable = candidates.firstOrNull { it.resolveActivity(context.packageManager) != null }
  startActivityOrAppSettings(context, resolvable ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
}

private fun openAppSettings(context: Context) {
  startActivityOrAppSettings(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
}

private fun startActivityOrAppSettings(context: Context, intent: Intent) {
  try {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  } catch (_: ActivityNotFoundException) {
    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }
}

private fun startFloatingOverlay(context: Context) {
  context.startService(Intent(context, FloatingOverlayService::class.java))
}

private fun stopFloatingOverlay(context: Context) {
  context.stopService(Intent(context, FloatingOverlayService::class.java))
}
