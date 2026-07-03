package com.autoclicker.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class AutoClickAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isServiceRunning = false

    private val clickRunnable = object : Runnable {
        override fun run() {
            if (FloatingClickService.isClicking && isServiceRunning) {
                performClick(FloatingClickService.targetX, FloatingClickService.targetY)
                handler.postDelayed(this, FloatingClickService.clickInterval)
            } else {
                // 持續輪詢，等待 isClicking 變為 true
                handler.postDelayed(this, 200)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        handler.post(clickRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不需要處理事件，僅用於 dispatchGesture
    }

    override fun onInterrupt() {
        // 服務被中斷
    }

    override fun onDestroy() {
        isServiceRunning = false
        handler.removeCallbacks(clickRunnable)
        super.onDestroy()
    }

    private fun performClick(x: Float, y: Float) {
        if (x <= 0f && y <= 0f) return

        val path = Path().apply {
            moveTo(x, y)
        }

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(
            GestureDescription.StrokeDescription(
                path,
                0,       // startTime: 立即開始
                50       // duration: 50ms 模擬點擊
            )
        )

        dispatchGesture(
            gestureBuilder.build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    // 點擊成功
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    // 點擊被取消（可能被其他手勢搶佔）
                }
            },
            null
        )
    }
}
