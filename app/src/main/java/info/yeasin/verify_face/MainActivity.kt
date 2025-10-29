package info.yeasin.verify_face

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.face.Face

class MainActivity : ComponentActivity() {

    private var onPermissionResult: ((Boolean) -> Unit)? = null
    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            onPermissionResult?.invoke(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val vm: LivenessViewModel = viewModel()
                val ui by vm.ui.collectAsState()

                var hasCamPermission by remember { mutableStateOf(false) }
                val context = LocalContext.current

                // Ask permission if needed
                LaunchedEffect(Unit) {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        hasCamPermission = true
                    } else {
                        onPermissionResult = { ok -> hasCamPermission = ok }
                        permLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                Surface(Modifier.fillMaxSize()) {
                    if (!hasCamPermission) {
                        PermissionScreen()
                    } else {
                        LivenessScreen(
                            status = ui.status,
                            instruction = ui.instruction,
                            done = ui.passed,
                            onFaceData = { face ->
                                vm.onFace(
                                    leftProb = face.leftEyeOpenProbability,
                                    rightProb = face.rightEyeOpenProbability,
                                    smileProb = face.smilingProbability,
                                    yawDeg = face.headEulerAngleY
                                )
                            },
                            onNoFace = { vm.onNoFace() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Waiting for camera permission…")
    }
}
@Composable
fun LivenessScreen(
    status: String,
    instruction: String,
    done: Boolean,
    onFaceData: (Face) -> Unit,
    onNoFace: () -> Unit
) {
    val analyzer = remember { FaceAnalyzer(onFace = onFaceData, onNoFace = onNoFace) }
    LaunchedEffect(done) { if (done) analyzer.close() }

    // স্টেপ অনুযায়ী 0..1 প্রগ্রেস
    val progress = remember(instruction, done) {
        when {
            done -> 1f
            instruction.contains("Blink", ignoreCase = true) -> 0.15f
            instruction.contains("LEFT", ignoreCase = true)  -> 0.55f
            instruction.contains("Smile", ignoreCase = true) -> 0.90f
            else -> 0f
        }
    }

    Box(Modifier.fillMaxSize()) {
        // 1) সাদা ব্যাকগ্রাউন্ড (রিং/গোল প্রিভিউকে হাইলাইট করবে)
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}

        // 2) গোল ক্যামেরা প্রিভিউ (স্ক্রিনশট স্টাইল)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val circleSize = 320.dp

            // গোল করে ক্লিপ করা ক্যামেরা ভিউ
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .clip(CircleShape)
            ) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    analyzer = analyzer
                )
            }

            // উপরেই ডটেড সার্কুলার প্রগ্রেস রিং
            LivenessDottedRingOverlay(
                progress = progress,
                ringSize = circleSize,
                tickWidth = 6.dp,
                tickLength = 18.dp,
                tickGapDegrees = 6f
            )
        }

        // 3) নিচে ইনস্ট্রাকশন/স্ট্যাটাস + (TURN_LEFT হলে) বাউন্সিং অ্যারো
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Text(status, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(10.dp))
            if (!done && instruction.contains("LEFT", ignoreCase = true)) {
                BouncingArrowLeft(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            if (!done) {
                LinearProgressIndicator(Modifier.fillMaxWidth(0.9f))
            } else {
                val scale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(250),
                    label = ""
                )
                val alpha by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(250),
                    label = ""
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.scaleX = scale
                            this.scaleY = scale
                            this.alpha = alpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color(0xFF2E7D32), // success green
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Verification complete",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

            }
        }
    }
}
@Composable
fun BouncingArrowLeft(
    modifier: Modifier = Modifier,
    color: Color = Color.Black
) {
    val infiniteTransition = rememberInfiniteTransition()
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Icon(
        imageVector = Icons.Default.ArrowForward,
        contentDescription = "Bouncing Arrow Left",
        tint = color,
        modifier = modifier
            .offset(y = offsetY.dp)
            .size(48.dp)
    )
}


