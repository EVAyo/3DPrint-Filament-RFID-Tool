package com.m0h31h31.bamburfidreader.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m0h31h31.bamburfidreader.R
import com.m0h31h31.bamburfidreader.cloud.BambuCloudFilament
import com.m0h31h31.bamburfidreader.cloud.BambuCloudFilamentCatalog
import com.m0h31h31.bamburfidreader.cloud.BambuCloudFilamentCatalogInfo
import com.m0h31h31.bamburfidreader.cloud.BambuCloudFilamentResult
import com.m0h31h31.bamburfidreader.cloud.BambuCloudPrinter
import com.m0h31h31.bamburfidreader.cloud.BambuCloudPrinterResult
import com.m0h31h31.bamburfidreader.cloud.BambuAmsTray
import com.m0h31h31.bamburfidreader.cloud.BambuAmsUnit
import com.m0h31h31.bamburfidreader.cloud.BambuMqttConnectionState
import com.m0h31h31.bamburfidreader.cloud.BambuMqttRealtimeClient
import com.m0h31h31.bamburfidreader.cloud.BambuPrinterImageMatcher
import com.m0h31h31.bamburfidreader.cloud.BambuPrinterRealtimeStatus
import com.m0h31h31.bamburfidreader.cloud.BambuCloudRepository
import com.m0h31h31.bamburfidreader.cloud.BambuCloudRepositoryResult
import com.m0h31h31.bamburfidreader.cloud.BambuCloudSession
import com.m0h31h31.bamburfidreader.cloud.BambuCloudRemainingSyncer
import com.m0h31h31.bamburfidreader.cloud.SensitiveValueMasker
import com.m0h31h31.bamburfidreader.ui.components.AppCircularProgressIndicator
import com.m0h31h31.bamburfidreader.ui.components.AppLinearProgressIndicator
import com.m0h31h31.bamburfidreader.ui.components.NeuButton
import com.m0h31h31.bamburfidreader.ui.components.NeuPanel
import com.m0h31h31.bamburfidreader.ui.components.neuBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun CloudConnectScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember(context) { BambuCloudRepository(context.applicationContext) }
    val coroutineScope = rememberCoroutineScope()
    var session by remember { mutableStateOf<BambuCloudSession?>(null) }
    var statusMessage by remember { mutableStateOf("") }
    var showLoginDialog by remember { mutableStateOf(false) }
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var verificationRequired by remember { mutableStateOf(false) }
    var loginInProgress by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var printers by remember { mutableStateOf<List<BambuCloudPrinter>>(emptyList()) }
    var printersLoading by remember { mutableStateOf(false) }
    var printerStatusMessage by remember { mutableStateOf("") }
    var realtimeStatuses by remember { mutableStateOf<Map<String, BambuPrinterRealtimeStatus>>(emptyMap()) }
    var mqttStatusMessage by remember { mutableStateOf("") }
    var filaments by remember { mutableStateOf<List<BambuCloudFilament>>(emptyList()) }
    var filamentCatalogInfo by remember { mutableStateOf<Map<Long, BambuCloudFilamentCatalogInfo>>(emptyMap()) }
    var filamentsLoading by remember { mutableStateOf(false) }
    var filamentsSyncing by remember { mutableStateOf(false) }
    var filamentStatusMessage by remember { mutableStateOf("") }

    suspend fun refreshPrinters() {
        if (repository.loadSession() == null) {
            printers = emptyList()
            printerStatusMessage = ""
            realtimeStatuses = emptyMap()
            mqttStatusMessage = ""
            printersLoading = false
            return
        }
        printersLoading = true
        printerStatusMessage = context.getString(R.string.cloud_printers_loading)
        when (val result = repository.fetchPrinters()) {
            is BambuCloudPrinterResult.Success -> {
                printers = result.printers
                printerStatusMessage = if (result.printers.isEmpty()) {
                    context.getString(R.string.cloud_printers_empty)
                } else {
                    ""
                }
            }
            is BambuCloudPrinterResult.Failure -> {
                printerStatusMessage = context.getString(
                    R.string.cloud_printers_refresh_failed,
                    result.message
                )
            }
        }
        printersLoading = false
    }

    suspend fun refreshFilaments() {
        if (repository.loadSession() == null) {
            filaments = emptyList()
            filamentCatalogInfo = emptyMap()
            filamentStatusMessage = ""
            filamentsLoading = false
            filamentsSyncing = false
            return
        }
        filamentsLoading = true
        filamentStatusMessage = context.getString(R.string.cloud_filaments_loading)
        when (val result = repository.fetchFilaments(offset = 0, limit = 20)) {
            is BambuCloudFilamentResult.Success -> {
                filaments = result.filaments
                filamentStatusMessage = if (result.filaments.isEmpty()) {
                    context.getString(R.string.cloud_filaments_empty)
                } else {
                    ""
                }
            }
            is BambuCloudFilamentResult.Failure -> {
                filamentStatusMessage = context.getString(
                    R.string.cloud_filaments_refresh_failed,
                    result.message
                )
            }
        }
        filamentsLoading = false
    }

    LaunchedEffect(filaments) {
        filamentCatalogInfo = if (filaments.isEmpty()) {
            emptyMap()
        } else {
            withContext(Dispatchers.IO) {
                filaments.mapNotNull { filament ->
                    BambuCloudFilamentCatalog.lookup(context, filament)?.let { info ->
                        filament.id to info
                    }
                }.toMap()
            }
        }
    }

    suspend fun refreshCloudLists() {
        refreshPrinters()
        refreshFilaments()
    }

    fun syncFilamentRemainingToLocal() {
        if (filaments.isEmpty() || filamentsSyncing) return
        filamentsSyncing = true
        filamentStatusMessage = context.getString(R.string.cloud_filaments_syncing_remaining)
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    BambuCloudRemainingSyncer.syncToLocalInventory(context, filaments)
                }
            }
            result.onSuccess { syncResult ->
                filamentStatusMessage = if (syncResult.updatedCount > 0) {
                    context.getString(
                        R.string.cloud_filaments_sync_remaining_success,
                        syncResult.updatedCount
                    )
                } else {
                    context.getString(R.string.cloud_filaments_sync_remaining_no_match)
                }
            }.onFailure { error ->
                filamentStatusMessage = context.getString(
                    R.string.cloud_filaments_sync_remaining_failed,
                    error.message.orEmpty().ifBlank { context.getString(R.string.label_unknown) }
                )
            }
            filamentsSyncing = false
        }
    }

    val deviceIdsKey = remember(printers) {
        printers.map { it.deviceId }.filter { it.isNotBlank() }.joinToString("|")
    }
    DisposableEffect(session?.account?.uid, session?.tokens?.accessToken, deviceIdsKey) {
        val activeSession = session
        if (activeSession == null || deviceIdsKey.isBlank()) {
            onDispose { }
        } else {
            val client = BambuMqttRealtimeClient(
                session = activeSession,
                deviceIds = printers.map { it.deviceId },
                onStatus = { status ->
                    coroutineScope.launch {
                        realtimeStatuses = realtimeStatuses + (
                            status.deviceId to mergeRealtimeStatus(
                                realtimeStatuses[status.deviceId],
                                status
                            )
                        )
                    }
                },
                onConnectionState = { state, error ->
                    coroutineScope.launch {
                        mqttStatusMessage = when (state) {
                            BambuMqttConnectionState.DISCONNECTED -> ""
                            BambuMqttConnectionState.CONNECTING -> {
                                context.getString(R.string.cloud_mqtt_connecting)
                            }
                            BambuMqttConnectionState.CONNECTED -> {
                                context.getString(R.string.cloud_mqtt_connected)
                            }
                            BambuMqttConnectionState.FAILED -> {
                                context.getString(
                                    R.string.cloud_mqtt_failed,
                                    error.ifBlank { context.getString(R.string.label_unknown) }
                                )
                            }
                        }
                    }
                }
            )
            client.start()
            onDispose {
                client.stop()
            }
        }
    }

    LaunchedEffect(Unit) {
        session = repository.loadSession()
        statusMessage = if (session == null) {
            context.getString(R.string.cloud_status_not_logged_in)
        } else {
            context.getString(R.string.cloud_status_logged_in)
        }
        if (session != null) {
            when (val result = repository.refreshAccount()) {
                is BambuCloudRepositoryResult.Success -> {
                    session = result.session
                }
                else -> Unit
            }
            refreshCloudLists()
        }
    }

    fun handleLoginResult(result: BambuCloudRepositoryResult) {
        loginInProgress = false
        when (result) {
            is BambuCloudRepositoryResult.Success -> {
                session = result.session
                showLoginDialog = false
                password = ""
                verificationCode = ""
                verificationRequired = false
                statusMessage = context.getString(R.string.cloud_status_login_success)
                coroutineScope.launch {
                    refreshCloudLists()
                }
            }
            BambuCloudRepositoryResult.VerificationCodeRequired -> {
                verificationRequired = true
                statusMessage = context.getString(R.string.cloud_status_verify_required)
            }
            is BambuCloudRepositoryResult.Failure -> {
                statusMessage = context.getString(R.string.cloud_status_login_failed, result.message)
            }
        }
    }

    fun startPasswordLogin() {
        if (account.isBlank() || password.isBlank()) {
            statusMessage = context.getString(R.string.cloud_status_missing_credentials)
            return
        }
        loginInProgress = true
        coroutineScope.launch {
            handleLoginResult(repository.loginWithPassword(account, password))
        }
    }

    fun startCodeLogin() {
        if (account.isBlank() || verificationCode.isBlank()) {
            statusMessage = context.getString(R.string.cloud_status_missing_code)
            return
        }
        loginInProgress = true
        coroutineScope.launch {
            handleLoginResult(repository.loginWithCode(account, password, verificationCode))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .neuBackground()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NeuPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val activeSession = session
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.cloud_connect_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    CloudStatusPill(
                        text = statusMessage,
                        connected = activeSession != null
                    )
                    if (activeSession != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { showLogoutConfirm = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_logout),
                                contentDescription = stringResource(R.string.cloud_logout),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (activeSession == null) {
                    NeuButton(
                        text = stringResource(R.string.cloud_login),
                        onClick = {
                            verificationRequired = false
                            showLoginDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    CloudAccountHeader(activeSession)
                }
            }
        }
        if (session != null) {
            CloudPrinterCarousel(
                printers = printers,
                loading = printersLoading,
                statusMessage = printerStatusMessage,
                mqttStatusMessage = mqttStatusMessage,
                realtimeStatuses = realtimeStatuses
            )
            CloudFilamentLibraryCard(
                filaments = filaments,
                catalogInfo = filamentCatalogInfo,
                loading = filamentsLoading,
                syncing = filamentsSyncing,
                statusMessage = filamentStatusMessage,
                onSyncRemaining = ::syncFilamentRemainingToLocal
            )
        }
    }

    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!loginInProgress) showLoginDialog = false
            },
            title = { Text(stringResource(R.string.cloud_login_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = account,
                        onValueChange = { account = it },
                        enabled = !loginInProgress,
                        singleLine = true,
                        label = { Text(stringResource(R.string.cloud_account)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        enabled = !loginInProgress,
                        singleLine = true,
                        label = { Text(stringResource(R.string.cloud_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (verificationRequired) {
                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = { verificationCode = it },
                            enabled = !loginInProgress,
                            singleLine = true,
                            label = { Text(stringResource(R.string.cloud_verification_code)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (loginInProgress) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppCircularProgressIndicator()
                            Text(
                                text = stringResource(R.string.cloud_status_logging_in),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (verificationRequired) startCodeLogin() else startPasswordLogin()
                    },
                    enabled = !loginInProgress
                ) {
                    Text(
                        if (verificationRequired) {
                            stringResource(R.string.cloud_login_code)
                        } else {
                            stringResource(R.string.cloud_login_password)
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLoginDialog = false },
                    enabled = !loginInProgress
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(R.string.cloud_logout_confirm_title)) },
            text = { Text(stringResource(R.string.cloud_logout_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.logout()
                        session = null
                        printers = emptyList()
                        filaments = emptyList()
                        filamentCatalogInfo = emptyMap()
                        printerStatusMessage = ""
                        filamentStatusMessage = ""
                        filamentsSyncing = false
                        realtimeStatuses = emptyMap()
                        mqttStatusMessage = ""
                        statusMessage = context.getString(R.string.cloud_status_logged_out)
                        showLogoutConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun CloudPrinterCarousel(
    printers: List<BambuCloudPrinter>,
    loading: Boolean,
    statusMessage: String,
    mqttStatusMessage: String,
    realtimeStatuses: Map<String, BambuPrinterRealtimeStatus>
) {
    val context = LocalContext.current
    var printerAssetNames by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        printerAssetNames = withContext(Dispatchers.IO) {
            context.assets.list(BambuPrinterImageMatcher.ASSET_DIR)?.toList().orEmpty()
        }
    }
    NeuPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cloud_printers_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (statusMessage.isNotBlank() && printers.isNotEmpty()) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (mqttStatusMessage.isNotBlank()) {
                        Text(
                            text = mqttStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (loading) {
                    AppCircularProgressIndicator(modifier = Modifier.size(22.dp))
                }
            }
            if (printers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 14.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (statusMessage.isBlank()) {
                            stringResource(R.string.cloud_printers_empty)
                        } else {
                            statusMessage
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val pagerState = rememberPagerState(pageCount = { printers.size })
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val cardWidth = if (maxWidth < 330.dp) {
                        maxWidth - 24.dp
                    } else {
                        286.dp
                    }
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = (maxWidth - cardWidth) / 2),
                        pageSpacing = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        CloudPrinterCard(
                            printer = printers[page],
                            realtimeStatus = realtimeStatuses[printers[page].deviceId],
                            assetNames = printerAssetNames,
                            modifier = Modifier.width(cardWidth)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudFilamentLibraryCard(
    filaments: List<BambuCloudFilament>,
    catalogInfo: Map<Long, BambuCloudFilamentCatalogInfo>,
    loading: Boolean,
    syncing: Boolean,
    statusMessage: String,
    onSyncRemaining: () -> Unit
) {
    NeuPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cloud_filaments_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.cloud_filaments_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(
                    onClick = onSyncRemaining,
                    enabled = filaments.isNotEmpty() && !loading && !syncing
                ) {
                    Text(
                        text = stringResource(R.string.cloud_filaments_sync_remaining),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (loading || syncing) {
                    AppCircularProgressIndicator(modifier = Modifier.size(22.dp))
                }
            }
            if (statusMessage.isNotBlank() && filaments.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (filaments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 14.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (statusMessage.isBlank()) {
                            stringResource(R.string.cloud_filaments_empty)
                        } else {
                            statusMessage
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    filaments.forEachIndexed { index, filament ->
                        CloudFilamentRow(
                            filament = filament,
                            catalogInfo = catalogInfo[filament.id]
                        )
                        if (index < filaments.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudFilamentRow(
    filament: BambuCloudFilament,
    catalogInfo: BambuCloudFilamentCatalogInfo?
) {
    val unknown = stringResource(R.string.label_unknown)
    val title = buildList {
        if (filament.vendor.isNotBlank()) add(filament.vendor)
        add(filament.name.ifBlank { filament.type.ifBlank { unknown } })
    }.joinToString(" ")
    val meta = buildList {
        val colorName = catalogInfo?.colorName.orEmpty()
        val filamentNumber = catalogInfo?.filamentNumber.orEmpty()
        if (colorName.isNotBlank()) add(colorName)
        if (filamentNumber.isNotBlank()) add(filamentNumber)
    }.joinToString("  |  ")
    val total = filament.totalNetWeightGrams.coerceAtLeast(0)
    val net = filament.netWeightGrams.coerceAtLeast(0)
    val progress = if (total > 0) {
        (net.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CloudFilamentColorPreview(filament)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (filament.inPrinter) {
                val location = buildList {
                    if (filament.deviceName.isNotBlank()) add(filament.deviceName)
                    filament.amsId?.let { add(stringResource(R.string.cloud_filament_ams, it)) }
                    if (filament.slotId.isNotBlank()) {
                        add(stringResource(R.string.cloud_filament_slot, filament.slotId))
                    }
                }.joinToString("  |  ")
                if (location.isNotBlank()) {
                    Text(
                        text = location,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Column(
            modifier = Modifier.width(92.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = if (total > 0) {
                    stringResource(R.string.cloud_filament_weight, net, total)
                } else {
                    stringResource(R.string.cloud_filament_weight_unknown)
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AppLinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun CloudFilamentColorPreview(filament: BambuCloudFilament) {
    val colors = filament.colors.ifEmpty {
        listOf(filament.color).filter { it.isNotBlank() }
    }.take(4)
    Row(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                shape = RoundedCornerShape(6.dp)
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (colors.isEmpty()) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        } else {
            colors.forEach { colorHex ->
                Box(
                    modifier = Modifier
                        .width((24 / colors.size.coerceAtLeast(1)).dp)
                        .height(24.dp)
                        .background(parseRgbaColor(colorHex) ?: MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun CloudPrinterCard(
    printer: BambuCloudPrinter,
    realtimeStatus: BambuPrinterRealtimeStatus?,
    assetNames: List<String>,
    modifier: Modifier = Modifier
) {
    val unknown = stringResource(R.string.label_unknown)
    val productName = printer.productName.ifBlank { unknown }
    val deviceId = printer.deviceId.ifBlank { unknown }
    val progress = realtimeStatus?.progress ?: printer.progress?.coerceIn(0, 100)
    val taskName = realtimeStatus?.taskName?.ifBlank { realtimeStatus.taskId }?.ifBlank { null }
        ?: printer.taskName.ifBlank { printer.taskId.ifBlank { unknown } }
    val taskStatus = realtimeStatus?.gcodeState?.ifBlank { null }
        ?: printer.taskStatus.ifBlank { unknown }
    val hasTask = taskName != unknown ||
        printer.taskId.isNotBlank() ||
        taskStatus != unknown ||
        progress != null
    var showAmsDialog by remember(printer.deviceId) { mutableStateOf(false) }
    var showDeviceIdPlain by remember(printer.deviceId) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            CloudPrinterProductImage(
                printer = printer,
                assetNames = assetNames,
                productName = productName,
                modifier = Modifier.size(82.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    CloudOnlinePill(online = printer.online)
                }
                MaskedInfoText(
                    value = deviceId,
                    plain = showDeviceIdPlain,
                    onToggle = { showDeviceIdPlain = !showDeviceIdPlain }
                )
            }
        }
        CloudPrinterTaskBlock(
            hasTask = hasTask,
            taskName = taskName,
            taskStatus = taskStatus,
            progress = progress,
            realtimeStatus = realtimeStatus
        )
        CloudAmsBlock(
            status = realtimeStatus,
            onClick = { showAmsDialog = true }
        )
    }
    if (showAmsDialog && realtimeStatus != null) {
        CloudAmsDialog(
            status = realtimeStatus,
            onDismiss = { showAmsDialog = false }
        )
    }
}

@Composable
private fun MaskedInfoText(
    value: String,
    plain: Boolean,
    onToggle: () -> Unit
) {
    if (value.isBlank()) return
    Text(
        text = if (plain) value else SensitiveValueMasker.maskMiddle(value),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clickable(onClick = onToggle)
    )
}

@Composable
private fun CloudPrinterProductImage(
    printer: BambuCloudPrinter,
    assetNames: List<String>,
    productName: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val assetName = remember(printer, assetNames) {
        BambuPrinterImageMatcher.matchAssetName(printer, assetNames)
    }
    val bitmap = rememberPrinterAssetBitmap(assetName)
    val shape = RoundedCornerShape(8.dp)
    val imageModifier = Modifier
        .then(modifier)
        .aspectRatio(1f)
        .clip(shape)
        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
        .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            shape = shape
        )
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = stringResource(R.string.cloud_printer_product_image),
            contentScale = ContentScale.Fit,
            modifier = imageModifier
        )
    } else {
        Box(
            modifier = imageModifier.padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = productName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CloudOnlinePill(online: Boolean) {
    val containerColor = if (online) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
    }
    val contentColor = if (online) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = stringResource(
                if (online) {
                    R.string.cloud_printer_online
                } else {
                    R.string.cloud_printer_offline
                }
            ),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
private fun CloudPrinterDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CloudPrinterTaskBlock(
    hasTask: Boolean,
    taskName: String,
    taskStatus: String,
    progress: Int?,
    realtimeStatus: BambuPrinterRealtimeStatus?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (!hasTask) {
            Text(
                text = stringResource(R.string.cloud_printer_task_idle),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }
        Text(
            text = taskName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.basicMarquee()
        )
        Text(
            text = taskStatus,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (progress != null) {
            AppLinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
            )
            Text(
                text = stringResource(R.string.cloud_printer_progress, progress),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        if (realtimeStatus != null) {
            CloudRealtimeMetricLine(realtimeStatus)
        }
    }
}

@Composable
private fun CloudRealtimeMetricLine(status: BambuPrinterRealtimeStatus) {
    val metrics = buildList {
        status.remainingMinutes?.let {
            add(stringResource(R.string.cloud_printer_remaining_time, it))
        }
        if (status.nozzleTemperature != null || status.nozzleTargetTemperature != null) {
            add(
                stringResource(
                    R.string.cloud_printer_nozzle_temp,
                    formatTemperature(status.nozzleTemperature),
                    formatTemperature(status.nozzleTargetTemperature)
                )
            )
        }
        if (status.bedTemperature != null || status.bedTargetTemperature != null) {
            add(
                stringResource(
                    R.string.cloud_printer_bed_temp,
                    formatTemperature(status.bedTemperature),
                    formatTemperature(status.bedTargetTemperature)
                )
            )
        }
        status.chamberTemperature?.let {
            add(stringResource(R.string.cloud_printer_chamber_temp, formatTemperature(it)))
        }
        if (status.wifiSignal.isNotBlank()) {
            add(stringResource(R.string.cloud_printer_wifi_signal, status.wifiSignal))
        }
    }
    if (metrics.isEmpty()) return
    Text(
        text = metrics.joinToString("  |  "),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun CloudAmsBlock(
    status: BambuPrinterRealtimeStatus?,
    onClick: () -> Unit
) {
    val units = status?.amsUnits.orEmpty()
    if (units.isEmpty()) return
    val trays = units.flatMap { unit ->
        unit.trays.map { tray -> unit to tray }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.cloud_ams_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.cloud_ams_expand),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            trays.forEach { (_, tray) ->
                CloudAmsColorSwatch(
                    colorHex = tray.color,
                    active = status?.currentTray == tray.id,
                    empty = !tray.hasFilament
                )
            }
        }
    }
}

@Composable
private fun CloudAmsDialog(
    status: BambuPrinterRealtimeStatus,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cloud_ams_details_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                status.amsUnits.forEach { unit ->
                    CloudAmsUnitDetails(unit)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_confirm))
            }
        }
    )
}

@Composable
private fun CloudAmsUnitDetails(unit: BambuAmsUnit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val meta = buildList {
            if (unit.temperature.isNotBlank()) {
                add(stringResource(R.string.cloud_ams_temp, unit.temperature))
            }
            if (unit.humidity.isNotBlank()) {
                add(stringResource(R.string.cloud_ams_humidity, unit.humidity))
            }
        }.joinToString("  |  ")
        Text(
            text = if (meta.isBlank()) {
                stringResource(R.string.cloud_ams_unit, unit.id)
            } else {
                "${stringResource(R.string.cloud_ams_unit, unit.id)}  $meta"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        unit.trays.forEach { tray ->
            CloudAmsTrayRow(tray)
        }
    }
}

@Composable
private fun CloudAmsTrayRow(tray: BambuAmsTray) {
    val hasFilament = tray.hasFilament
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CloudAmsColorSwatch(colorHex = tray.color, active = false, empty = !hasFilament)
        Column(modifier = Modifier.weight(1f)) {
            val title = buildList {
                add(stringResource(R.string.cloud_ams_slot, tray.id))
                if (hasFilament) {
                    if (tray.filamentType.isNotBlank()) add(tray.filamentType)
                    if (tray.name.isNotBlank()) add(tray.name)
                } else {
                    add(stringResource(R.string.cloud_ams_slot_empty))
                }
            }.joinToString("  ")
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (hasFilament) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val detail = if (hasFilament) buildList {
                tray.remain?.let { add(stringResource(R.string.cloud_ams_remain, it)) }
                if (tray.nozzleTempMin.isNotBlank() || tray.nozzleTempMax.isNotBlank()) {
                    add(
                        stringResource(
                            R.string.cloud_ams_nozzle_range,
                            tray.nozzleTempMin.ifBlank { "-" },
                            tray.nozzleTempMax.ifBlank { "-" }
                        )
                    )
                }
                if (tray.trayInfoIndex.isNotBlank()) add(tray.trayInfoIndex)
                if (tray.tagUid.isNotBlank() && tray.tagUid != "0000000000000000") {
                    add(tray.tagUid)
                }
            }.joinToString("  |  ") else stringResource(R.string.cloud_ams_slot_empty_hint)
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CloudAmsColorSwatch(
    colorHex: String,
    active: Boolean,
    empty: Boolean = false
) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 16.dp)
            .clip(shape)
            .background(
                if (empty) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
                } else {
                    parseRgbaColor(colorHex) ?: MaterialTheme.colorScheme.surface
                }
            )
            .border(
                width = if (active) 2.dp else 1.dp,
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
                },
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (empty) {
            Text(
                text = "?",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun CloudStatusPill(
    text: String,
    connected: Boolean
) {
    val containerColor = if (connected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    }
    val contentColor = if (connected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CloudAccountHeader(session: BambuCloudSession) {
    val account = session.account
    val displayName = account.name.ifBlank { stringResource(R.string.label_unknown) }
    val handleText = account.handle.ifBlank { stringResource(R.string.label_unknown) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CloudAvatar(
            name = displayName,
            avatarUrl = account.avatarUrl,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@$handleText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CloudAvatar(
    name: String,
    avatarUrl: String,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberRemoteBitmap(avatarUrl)
    val shape = CircleShape
    val avatarModifier = modifier
        .clip(shape)
        .background(MaterialTheme.colorScheme.primaryContainer)
        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), shape)
    val loadedBitmap = bitmap
    if (loadedBitmap != null) {
        Image(
            bitmap = loadedBitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = avatarModifier
        )
    } else {
        Box(
            modifier = avatarModifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercaseChar()?.toString().orEmpty().ifBlank { "B" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun rememberRemoteBitmap(imageUrl: String): ImageBitmap? {
    var bitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(imageUrl) {
        bitmap = if (imageUrl.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val connection = URL(imageUrl).openConnection() as HttpURLConnection
                    connection.connectTimeout = 5_000
                    connection.readTimeout = 5_000
                    try {
                        connection.inputStream.use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                    } finally {
                        connection.disconnect()
                    }
                }.getOrNull()
            }
        }
    }
    return bitmap
}

@Composable
private fun rememberPrinterAssetBitmap(assetName: String?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(assetName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(assetName) {
        bitmap = if (assetName.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.assets
                        .open("${BambuPrinterImageMatcher.ASSET_DIR}/$assetName")
                        .use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                }.getOrNull()
            }
        }
    }
    return bitmap
}

private fun mergeRealtimeStatus(
    previous: BambuPrinterRealtimeStatus?,
    next: BambuPrinterRealtimeStatus
): BambuPrinterRealtimeStatus {
    if (previous == null) return next
    return next.copy(
        gcodeState = next.gcodeState.ifBlank { previous.gcodeState },
        taskName = next.taskName.ifBlank { previous.taskName },
        taskId = next.taskId.ifBlank { previous.taskId },
        progress = next.progress ?: previous.progress,
        remainingMinutes = next.remainingMinutes ?: previous.remainingMinutes,
        nozzleTemperature = next.nozzleTemperature ?: previous.nozzleTemperature,
        nozzleTargetTemperature = next.nozzleTargetTemperature ?: previous.nozzleTargetTemperature,
        bedTemperature = next.bedTemperature ?: previous.bedTemperature,
        bedTargetTemperature = next.bedTargetTemperature ?: previous.bedTargetTemperature,
        chamberTemperature = next.chamberTemperature ?: previous.chamberTemperature,
        wifiSignal = next.wifiSignal.ifBlank { previous.wifiSignal },
        currentTray = next.currentTray.ifBlank { previous.currentTray },
        amsUnits = next.amsUnits.ifEmpty { previous.amsUnits }
    )
}

private fun formatTemperature(value: Double?): String {
    return value?.let {
        if (it % 1.0 == 0.0) {
            it.toInt().toString()
        } else {
            String.format("%.1f", it)
        }
    } ?: "-"
}

private fun parseRgbaColor(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    if (clean.length < 6) return null
    return runCatching {
        val red = clean.substring(0, 2).toInt(16)
        val green = clean.substring(2, 4).toInt(16)
        val blue = clean.substring(4, 6).toInt(16)
        val alpha = clean.takeIf { it.length >= 8 }?.substring(6, 8)?.toInt(16) ?: 255
        Color(red = red, green = green, blue = blue, alpha = alpha)
    }.getOrNull()
}
