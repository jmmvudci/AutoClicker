package com.autoclicker.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnToggle: Button
    private lateinit var btnOverlayPerm: Button
    private lateinit var btnAccessPerm: Button
    private lateinit var seekInterval: SeekBar
    private lateinit var tvInterval: TextView
    private lateinit var tvOverlayStatus: TextView
    private lateinit var tvAccessStatus: TextView

    // 點擊間隔（毫秒）
    private var clickInterval: Long = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnToggle = findViewById(R.id.btn_toggle)
        btnOverlayPerm = findViewById(R.id.btn_overlay_perm)
        btnAccessPerm = findViewById(R.id.btn_access_perm)
        seekInterval = findViewById(R.id.seek_interval)
        tvInterval = findViewById(R.id.tv_interval)
        tvOverlayStatus = findViewById(R.id.tv_overlay_status)
        tvAccessStatus = findViewById(R.id.tv_access_status)

        // 讀取儲存的間隔設定
        val prefs = getSharedPreferences("autoclicker", MODE_PRIVATE)
        clickInterval = prefs.getLong("interval", 500L)
        seekInterval.progress = (clickInterval / 50).toInt().coerceIn(1, 100)
        tvInterval.text = "${clickInterval} ms"

        // 間隔調整 (50ms ~ 5000ms)
        seekInterval.max = 100
        seekInterval.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                clickInterval = (progress.coerceAtLeast(1) * 50).toLong()
                tvInterval.text = "${clickInterval} ms"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                prefs.edit().putLong("interval", clickInterval).apply()
            }
        })

        // 懸浮視窗權限按鈕
        btnOverlayPerm.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "懸浮視窗權限已開啟 ✓", Toast.LENGTH_SHORT).show()
            }
        }

        // 無障礙權限按鈕
        btnAccessPerm.setOnClickListener {
            if (!isAccessibilityEnabled()) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, "請找到「自動連點器」並開啟", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "無障礙服務已開啟 ✓", Toast.LENGTH_SHORT).show()
            }
        }

        // 啟動/停止按鈕
        btnToggle.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "請先開啟懸浮視窗權限", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "請先開啟無障礙服務", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (FloatingClickService.isRunning) {
                stopService(Intent(this, FloatingClickService::class.java))
                btnToggle.text = "啟動連點器"
            } else {
                val intent = Intent(this, FloatingClickService::class.java)
                intent.putExtra("interval", clickInterval)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                btnToggle.text = "停止連點器"
                // 最小化 App，讓使用者切換到目標 App
                moveTaskToBack(true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        btnToggle.text = if (FloatingClickService.isRunning) "停止連點器" else "啟動連點器"
    }

    private fun updatePermissionStatus() {
        val overlayOk = Settings.canDrawOverlays(this)
        val accessOk = isAccessibilityEnabled()

        tvOverlayStatus.text = if (overlayOk) "✓ 已開啟" else "✗ 未開啟"
        tvOverlayStatus.setTextColor(
            if (overlayOk) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
        )

        tvAccessStatus.text = if (accessOk) "✓ 已開啟" else "✗ 未開啟"
        tvAccessStatus.setTextColor(
            if (accessOk) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
        )

        btnToggle.isEnabled = overlayOk && accessOk
        btnToggle.alpha = if (overlayOk && accessOk) 1.0f else 0.5f
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == packageName
        }
    }
}
