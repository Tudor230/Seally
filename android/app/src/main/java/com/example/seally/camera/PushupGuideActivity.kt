package com.example.seally.camera

import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.example.seally.ui.theme.SeallyTheme

class PushupGuideActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure the content flows under the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Set transparent colors for system bars
        window.navigationBarColor = AndroidColor.TRANSPARENT
        window.statusBarColor = AndroidColor.TRANSPARENT
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        
        // Enable Edge-to-Edge with transparent system bars
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        
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
