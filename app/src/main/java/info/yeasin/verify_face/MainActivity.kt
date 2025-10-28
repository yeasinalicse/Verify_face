package info.yeasin.verify_face

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val analyzer = remember {
        FaceAnalyzer(
            onFace = { f -> onFaceData(f) },
            onNoFace = { onNoFace() }
        )
    }

    LaunchedEffect(done) {
        if (done) analyzer.close()
    }

    // current step index map
    val stepIndex = remember(instruction) {
        when {
            instruction.contains("Blink", ignoreCase = true) -> 0
            instruction.contains("LEFT", ignoreCase = true) -> 1
            instruction.contains("Smile", ignoreCase = true) -> 2
            instruction.contains("passed", ignoreCase = true) -> 2
            else -> 0
        }
    }

    Box(Modifier.fillMaxSize()) {
        // 1) Camera preview
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            analyzer = analyzer
        )

        // 2) HUD Overlay
        // Top: circular step ring + instruction pill
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                CircularStepProgress(
                    modifier = Modifier.fillMaxSize(),
                    totalSteps = 3,
                    currentStepIndex = stepIndex
                )
                // center title
                Text(
                    text = "${stepIndex + 1}/3",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))
            StatusPill(
                text = instruction,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.weight(1f))

            // Bottom: status text + animated arrow (only for TURN_LEFT)
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(status, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))

                if (!done && instruction.contains("LEFT", ignoreCase = true)) {
                    // ভিডিওর মতো বামদিকের অ্যারো
                    BouncingArrowLeft(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))
                if (!done) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .align(Alignment.CenterHorizontally)
                    )
                } else {
                    Text("✅ Verification complete", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
