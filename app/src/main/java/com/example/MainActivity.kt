package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Natural Tones styling definitions
val NaturalBackground = Color(0xFFFBFDF7)
val TextPrimary = Color(0xFF1A1C19)
val NaturalGreenSolid = Color(0xFF4B6350)
val NaturalGreenPale = Color(0xFFD7E8CD)
val NaturalGreenSecondary = Color(0xFFF1F3E9)
val BorderNatural = Color(0xFFE1E3DA)
val TextMuted = Color(0xFF43493E)

val ConnectedGreen = Color(0xFF4B6350)
val DisconnectedRed = Color(0xFFBA1A1A)
val ConnectingOrange = Color(0xFFA17A00)

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

data class LogEntry(
    val timestamp: String,
    val level: String, // "INFO", "SUCCESS", "WARN"
    val message: String
)

class ConnectionViewModel : ViewModel() {
    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(listOf(
        LogEntry(currentTime(), "INFO", "Openvine central inicializado. Esperando comando...")
    ))
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _nodeAddress = MutableStateFlow("https://open.app/connect")
    val nodeAddress: StateFlow<String> = _nodeAddress.asStateFlow()

    private val _encryptionEnabled = MutableStateFlow(true)
    val encryptionEnabled: StateFlow<Boolean> = _encryptionEnabled.asStateFlow()

    private val _compressionEnabled = MutableStateFlow(true)
    val compressionEnabled: StateFlow<Boolean> = _compressionEnabled.asStateFlow()

    fun updateNodeAddress(address: String) {
        _nodeAddress.value = address
    }

    fun toggleEncryption(enabled: Boolean) {
        _encryptionEnabled.value = enabled
        addLog("INFO", "Cifrado SSL de extremo a extremo " + (if (enabled) "habilitado" else "deshabilitado") + ".")
    }

    fun toggleCompression(enabled: Boolean) {
        _compressionEnabled.value = enabled
        addLog("INFO", "Compresión GZIP en tránsito " + (if (enabled) "habilitada" else "deshabilitada") + ".")
    }

    private fun currentTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    fun addLog(level: String, message: String) {
        val newEntry = LogEntry(currentTime(), level, message)
        _logs.value = _logs.value + newEntry
    }

    fun clearLogs() {
        _logs.value = listOf(LogEntry(currentTime(), "INFO", "Registro borrado por el operador."))
    }

    fun startConnecting(scope: kotlinx.coroutines.CoroutineScope) {
        if (_status.value != ConnectionStatus.DISCONNECTED) return
        
        _status.value = ConnectionStatus.CONNECTING
        _progress.value = 0f
        
        scope.launch {
            addLog("INFO", "Abriendo puerto local de Openvine...")
            delay(500)
            _progress.value = 0.20f
            addLog("INFO", "Localizando pasarela de destino: ${_nodeAddress.value}")
            
            delay(600)
            _progress.value = 0.50f
            addLog("INFO", "Handshake enviado. Validando certificado criptográfico del servicio 'Open'...")
            
            delay(700)
            _progress.value = 0.80f
            if (_encryptionEnabled.value) {
                addLog("SUCCESS", "Canal de transporte TLS v1.3 asegurado con AES-256GCM.")
            } else {
                addLog("WARN", "Canal establecido sin cifrado - Tránsito inseguro.")
            }
            if (_compressionEnabled.value) {
                addLog("INFO", "Compresión de paquetes activa para transporte liviano.")
            }
            
            delay(500)
            _progress.value = 1.0f
            _status.value = ConnectionStatus.CONNECTED
            addLog("SUCCESS", "¡Puente de enlace Openvine activo! Conectado satisfactoriamente con la interfaz 'Open'.")
        }
    }

    fun disconnect() {
        if (_status.value == ConnectionStatus.DISCONNECTED) return
        _status.value = ConnectionStatus.DISCONNECTED
        _progress.value = 0f
        addLog("WARN", "Terminando puerto de enlace con Openvine...")
        addLog("INFO", "Canal finalizado de manera segura. Desconectado de la aplicación principal 'Open'.")
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = NaturalBackground
                ) { innerPadding ->
                    OpenvineConnectDashboard(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun OpenvineConnectDashboard(
    modifier: Modifier = Modifier,
    viewModel: ConnectionViewModel = viewModel()
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val nodeAddress by viewModel.nodeAddress.collectAsStateWithLifecycle()
    val encryptionEnabled by viewModel.encryptionEnabled.collectAsStateWithLifecycle()
    val compressionEnabled by viewModel.compressionEnabled.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Auto-scroll logs terminal whenever logs size increases
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .background(NaturalBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header containing modern icon styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(NaturalGreenSolid, Color(0xFF6E8C75)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Openvine",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Adaptador de Conexión a 'Open'",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Concentric Breathing Pulse Action Panel
        PulseConnectionCard(
            status = status,
            progress = progress,
            onConnectClick = { viewModel.startConnecting(coroutineScope) },
            onDisconnectClick = { viewModel.disconnect() }
        )

        // Custom flowing connection schema visualizer diagram
        ConnectionSchemaDiagram(status = status)

        // Explanation Module: Tells what the app is about as requested by the user
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderNatural)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NaturalGreenSolid,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.info_section_title),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = stringResource(id = R.string.info_section_desc),
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )

                Divider(color = BorderNatural, thickness = 1.dp)

                // Feature points
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureRow(
                        icon = Icons.Default.Check,
                        title = "Enlace Simplificado",
                        desc = "Se encarga del handshake entre tu nodo local y la app Open."
                    )
                    FeatureRow(
                        icon = Icons.Default.Settings,
                        title = "Cifrado Completo",
                        desc = "Protección nativa e interoperabilidad bajo llave privada robusta."
                    )
                    FeatureRow(
                        icon = Icons.Default.Build,
                        title = "Aplicación Principal 'Open'",
                        desc = "Tu entorno local interactúa automáticamente con el panel global de Open."
                    )
                }
            }
        }

        // Connection Settings Adjustmen Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderNatural)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = NaturalGreenSolid,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.settings_title),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = nodeAddress,
                    onValueChange = { viewModel.updateNodeAddress(it) },
                    label = { Text(text = stringResource(id = R.string.mock_ip_label)) },
                    singleLine = true,
                    enabled = status == ConnectionStatus.DISCONNECTED,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NaturalGreenSolid,
                        unfocusedBorderColor = BorderNatural,
                        focusedLabelColor = NaturalGreenSolid,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        disabledBorderColor = BorderNatural.copy(alpha = 0.5f),
                        disabledTextColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Habilitar Cifrado AES-256",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cifra paquetes de canal con capa segura SSL.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = encryptionEnabled,
                        onCheckedChange = { viewModel.toggleEncryption(it) },
                        enabled = status == ConnectionStatus.DISCONNECTED,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NaturalGreenSolid,
                            checkedTrackColor = NaturalGreenPale,
                            uncheckedBorderColor = BorderNatural
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Habilitar Compresión",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Optimiza banda ancha comprimiendo tramas GZIP.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = compressionEnabled,
                        onCheckedChange = { viewModel.toggleCompression(it) },
                        enabled = status == ConnectionStatus.DISCONNECTED,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NaturalGreenSolid,
                            checkedTrackColor = NaturalGreenPale,
                            uncheckedBorderColor = BorderNatural
                        )
                    )
                }
            }
        }

        // Terminal Log Console Module
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderNatural)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (status == ConnectionStatus.CONNECTED) ConnectedGreen else if (status == ConnectionStatus.CONNECTING) ConnectingOrange else TextMuted)
                        )
                        Text(
                            text = "CONEXIÓN LOG CONSOLE",
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.clearLogs() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Limpiar LOGS",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(Color(0xFFF1F3E9), RoundedCornerShape(8.dp))
                        .border(1.dp, BorderNatural, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            text = "No hay entradas de log disponibles.",
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(logs) { log ->
                                LogLine(log)
                            }
                        }
                    }
                }
            }
        }
        
        Text(
            text = "Openvine Connect • Sincronía adaptada para 'Open'",
            color = TextMuted,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
fun PulseConnectionCard(
    status: ConnectionStatus,
    progress: Float,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_pulse"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha"
    )

    val themeColor = when (status) {
        ConnectionStatus.DISCONNECTED -> DisconnectedRed
        ConnectionStatus.CONNECTING -> ConnectingOrange
        ConnectionStatus.CONNECTED -> ConnectedGreen
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderNatural)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.connection_status).uppercase(),
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
            
            // Pulsing Animation Bubble
            Box(
                modifier = Modifier
                    .size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                if (status != ConnectionStatus.DISCONNECTED) {
                    Box(
                        modifier = Modifier
                            .size((100f + (50f * pulseScale)).dp)
                            .clip(CircleShape)
                            .drawBehind {
                                drawCircle(
                                    color = themeColor,
                                    alpha = pulseAlpha,
                                    style = Stroke(width = 4f)
                                )
                            }
                    )
                }

                CircularProgressIndicator(
                    progress = if (status == ConnectionStatus.CONNECTING) progress else if (status == ConnectionStatus.CONNECTED) 1f else 0f,
                    modifier = Modifier.size(104.dp),
                    color = themeColor,
                    strokeWidth = 4.dp,
                    trackColor = BorderNatural
                )

                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    themeColor.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(1.5.dp, themeColor.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (status) {
                            ConnectionStatus.DISCONNECTED -> Icons.Default.Warning
                            ConnectionStatus.CONNECTING -> Icons.Default.Refresh
                            ConnectionStatus.CONNECTED -> Icons.Default.Check
                        },
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = when (status) {
                    ConnectionStatus.DISCONNECTED -> stringResource(id = R.string.disconnected_status)
                    ConnectionStatus.CONNECTING -> stringResource(id = R.string.connecting_status)
                    ConnectionStatus.CONNECTED -> stringResource(id = R.string.connected_status)
                },
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (status == ConnectionStatus.CONNECTING) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = ConnectingOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedContent(
                targetState = status,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "connect_btns"
            ) { currentStatus ->
                if (currentStatus == ConnectionStatus.DISCONNECTED) {
                    Button(
                        onClick = onConnectClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(NaturalGreenSolid, Color(0xFF5A7560)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.connect_btn),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onDisconnectClick,
                        enabled = currentStatus == ConnectionStatus.CONNECTED,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("disconnect_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.2.dp, if (currentStatus == ConnectionStatus.CONNECTED) DisconnectedRed else BorderNatural),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (currentStatus == ConnectionStatus.CONNECTED) DisconnectedRed else TextMuted
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentStatus == ConnectionStatus.CONNECTING) "Resolviendo..." else stringResource(id = R.string.disconnect_btn),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionSchemaDiagram(
    status: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "schema_pulse")
    
    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "energy_flow"
    )
    
    val pulseSizeMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_size"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NodeElement(
            title = "Dispositivo",
            subtitle = "Openvine",
            icon = Icons.Default.List,
            isActive = true,
            activeColor = NaturalGreenSolid,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .weight(1.3f)
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val startX = 0f
                val endX = size.width
                val centerY = size.height / 2f
                
                // Base grid line
                drawLine(
                    color = BorderNatural,
                    start = Offset(startX, centerY),
                    end = Offset(endX, centerY),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
                
                if (status == ConnectionStatus.CONNECTING) {
                    drawLine(
                        color = ConnectingOrange,
                        start = Offset(startX, centerY),
                        end = Offset(endX, centerY),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(15f, 15f),
                            phase = animatedPhase
                        )
                    )
                } else if (status == ConnectionStatus.CONNECTED) {
                    drawLine(
                        brush = Brush.horizontalGradient(listOf(NaturalGreenSolid, Color(0xFF8FA895))),
                        start = Offset(startX, centerY),
                        end = Offset(endX, centerY),
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
                    )
                    
                    // Moving energy packet particle dot
                    drawCircle(
                        color = NaturalGreenSolid,
                        radius = 12f * pulseSizeMultiplier,
                        center = Offset(startX + (endX - startX) * ((animatedPhase % 60f) / 60f), centerY)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        when (status) {
                            ConnectionStatus.DISCONNECTED -> NaturalGreenSecondary
                            ConnectionStatus.CONNECTING -> ConnectingOrange.copy(alpha = 0.2f)
                            ConnectionStatus.CONNECTED -> NaturalGreenSolid.copy(alpha = 0.2f)
                        }
                    )
                    .border(
                        1.dp,
                        when (status) {
                            ConnectionStatus.DISCONNECTED -> BorderNatural
                            ConnectionStatus.CONNECTING -> ConnectingOrange
                            ConnectionStatus.CONNECTED -> NaturalGreenSolid
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (status) {
                        ConnectionStatus.DISCONNECTED -> Icons.Default.Warning
                        ConnectionStatus.CONNECTING -> Icons.Default.Refresh
                        ConnectionStatus.CONNECTED -> Icons.Default.Check
                    },
                    contentDescription = null,
                    tint = when (status) {
                        ConnectionStatus.DISCONNECTED -> TextMuted
                        ConnectionStatus.CONNECTING -> ConnectingOrange
                        ConnectionStatus.CONNECTED -> NaturalGreenSolid
                    },
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        NodeElement(
            title = "App Destino",
            subtitle = "Open",
            icon = Icons.Default.Build,
            isActive = status == ConnectionStatus.CONNECTED,
            activeColor = Color(0xFF8FA895),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun NodeElement(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isActive: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isActive) activeColor.copy(alpha = 0.12f) else NaturalGreenSecondary)
                .border(
                    width = if (isActive) 1.5.dp else 1.dp,
                    color = if (isActive) activeColor else BorderNatural,
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) activeColor else TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = subtitle,
            color = if (isActive) TextPrimary else TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = title,
            color = TextMuted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FeatureRow(
    icon: ImageVector,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(NaturalGreenSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NaturalGreenSolid,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun LogLine(log: LogEntry) {
    val levelColor = when (log.level) {
        "SUCCESS" -> ConnectedGreen
        "WARN" -> ConnectingOrange
        else -> TextMuted
    }

    val annotatedString = buildAnnotatedString {
        // Timestamp in natural accent green
        withStyle(style = SpanStyle(color = NaturalGreenSolid, fontWeight = FontWeight.SemiBold)) {
            append("[${log.timestamp}] ")
        }
        // Logger level tag
        withStyle(style = SpanStyle(color = levelColor, fontWeight = FontWeight.Bold)) {
            append("${log.level.padEnd(7)} ")
        }
        // Message body
        withStyle(style = SpanStyle(color = TextPrimary)) {
            append(log.message)
        }
    }

    Text(
        text = annotatedString,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    MyApplicationTheme {
        OpenvineConnectDashboard()
    }
}
