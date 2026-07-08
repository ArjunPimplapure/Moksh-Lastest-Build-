package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        androidx.compose.material3.Surface(
          color = com.example.ui.DarkBackground,
          modifier = androidx.compose.ui.Modifier.fillMaxSize()
        ) {
          androidx.compose.foundation.layout.Box(
            contentAlignment = androidx.compose.ui.Alignment.Center,
            modifier = androidx.compose.ui.Modifier.fillMaxSize()
          ) {
            androidx.compose.material3.Card(
              colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = com.example.ui.SurfaceCard),
              shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
              modifier = androidx.compose.ui.Modifier.padding(24.dp).fillMaxWidth()
            ) {
              androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier.padding(16.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
              ) {
                androidx.compose.material3.Text(
                  text = "JewelLedger",
                  color = com.example.ui.LuxuryGold,
                  fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                  fontSize = 24.sp,
                  fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                )
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                androidx.compose.material3.Text(
                  text = "Admin Mode Active",
                  color = com.example.ui.AccentGreen,
                  fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                  fontSize = 14.sp
                )
              }
            }
          }
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
