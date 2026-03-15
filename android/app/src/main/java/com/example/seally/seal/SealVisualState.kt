package com.example.seally.seal

enum class SealVisualState(val mAssetPath: String) {
    VeryLow("seals/seal1.png"),
    Low("seals/seal2.png"),
    Neutral("seals/seal3.png"),
    High("seals/seal4.png"),
    VeryHigh("seals/seal5.png");

    companion object {
        fun fromHiddenPoints(hiddenPoints: Int): SealVisualState {
            return when {
                hiddenPoints <= -4 -> VeryLow
                hiddenPoints <= -2 -> Low
                hiddenPoints < 2 -> Neutral
                hiddenPoints < 4 -> High
                else -> VeryHigh
            }
        }
    }
}
