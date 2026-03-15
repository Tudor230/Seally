package com.example.seally.camera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.seally.ui.theme.SeallyTheme

class PushupGuideActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SeallyTheme {
                ExerciseGuideScreen(
                    mTitle = "Push-up",
                    mImageAssetPath = "img/pushup.png",
                    mOnConfirm = { finish() },
                )
            }
        }
    }
}
