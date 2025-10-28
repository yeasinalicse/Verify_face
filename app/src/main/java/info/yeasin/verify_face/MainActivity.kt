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

    Column(Modifier.fillMaxSize()) {
        // Camera preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                analyzer = analyzer
            )
        }

        // Instructions + Status
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(instruction, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(status, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            if (!done) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                Text("✅ Verification complete", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}