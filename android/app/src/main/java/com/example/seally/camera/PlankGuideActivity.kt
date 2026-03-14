package com.example.seally.camera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.seally.ui.theme.SeallyTheme

class PlankGuideActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SeallyTheme {
                ExerciseGuideScreen(
                    mTitle = "Plank",
                    mImageAssetPath = "img/plank.png",
                    mOnConfirm = { finish() },
                )
            }
        }
    }
}
