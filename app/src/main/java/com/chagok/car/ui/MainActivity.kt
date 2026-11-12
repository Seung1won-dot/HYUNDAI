package com.chagok.car.ui

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val viewModel: ChagokViewModel by viewModels()
    private val FUSED_LOCATION = "pleos.car.permission.FUSED_LOCATION"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 주차 좌표에 쓰는 Pleos 위치 권한은 사용자가 허용해야 한다.
        if (checkSelfPermission(FUSED_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(FUSED_LOCATION), 1)
        }
        setContent { ChagokApp(viewModel) }
    }
}
