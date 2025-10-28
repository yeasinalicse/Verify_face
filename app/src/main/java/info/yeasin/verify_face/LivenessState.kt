package info.yeasin.verify_face

data class LivenessState(
    val step: Step = Step.BLINK,
    val status: String = "Looking for a face...",
    val instruction: String = "Instruction: Blink your eyes",
    val passed: Boolean = false
)