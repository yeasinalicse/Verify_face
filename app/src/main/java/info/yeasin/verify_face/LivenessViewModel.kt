package info.yeasin.verify_face

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LivenessViewModel : ViewModel() {
    private val _ui = MutableStateFlow(LivenessState())
    val ui: StateFlow<LivenessState> = _ui

    private var lastStepTime = System.currentTimeMillis()
    private val stepTimeoutMs = 15_000L

    private fun set(step: Step, status: String, instruction: String, passed: Boolean = false) {
        _ui.value = LivenessState(step, status, instruction, passed)
    }

    fun onNoFace() {
        if (_ui.value.passed) return
        _ui.value = _ui.value.copy(status = "No face detected.")
    }

    fun onFace(leftProb: Float?, rightProb: Float?, smileProb: Float?, yawDeg: Float?) {
        if (_ui.value.passed) return
        val now = System.currentTimeMillis()
        if (now - lastStepTime > stepTimeoutMs && _ui.value.step != Step.DONE) {
            // timeout → reset
            set(Step.BLINK, "Timed out. Restarting.", "Instruction: Blink your eyes")
            lastStepTime = now
            return
        }

        when (_ui.value.step) {
            Step.BLINK -> {
                val l = leftProb ?: -1f
                val r = rightProb ?: -1f
                if (l in 0f..0.35f && r in 0f..0.35f) {
                    advance("Blink detected ✔")
                } else {
                    progress("Face ok. Blink your eyes…")
                }
            }
            Step.TURN_LEFT -> {
                val yaw = yawDeg ?: 0f
                if (yaw < -15f) {
                    advance("Turned left ✔")
                } else {
                    progress("Face ok. Turn your head LEFT…")
                }
            }
            Step.SMILE -> {
                val s = smileProb ?: -1f
                if (s >= 0.70f) {
                    // Done!
                    _ui.value = _ui.value.copy(
                        step = Step.DONE,
                        status = "Smile detected ✔",
                        instruction = "Liveness passed ✅",
                        passed = true
                    )
                } else {
                    progress("Face ok. Give a big SMILE…")
                }
            }
            Step.DONE -> Unit
        }
    }

    private fun progress(text: String) {
        _ui.value = _ui.value.copy(status = text)
    }

    private fun advance(msg: String) {
        val next = when (_ui.value.step) {
            Step.BLINK -> Step.TURN_LEFT
            Step.TURN_LEFT -> Step.SMILE
            Step.SMILE -> Step.DONE
            Step.DONE -> Step.DONE
            else -> {}
        }
        lastStepTime = System.currentTimeMillis()
        val nextInstruction = when (next) {
            Step.TURN_LEFT -> "Instruction: Turn your head LEFT"
            Step.SMILE -> "Instruction: Smile"
            Step.DONE -> "Liveness passed ✅"
            Step.BLINK -> "Instruction: Blink your eyes"
            else -> {}
        }
        _ui.value = _ui.value.copy(step = next as Step, status = msg, instruction = nextInstruction as String)
    }
}