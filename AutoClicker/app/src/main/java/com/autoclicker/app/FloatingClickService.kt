package com.autoclicker.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView

class FloatingClickService : Service() {

    companion object {
        var isRunning = false
        // 給 AccessibilityService 讀取的目標座標
        var targetX: Float = 0f
        var targetY: Float = 0f
        var isClicking: Boolean = false
        var clickInterval: Long = 500L
    }

    private lateinit var windowManager: WindowManager

    // 懸浮氣泡（小圓點，可拖曳）
    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    // 控制面板（播放/暫停/關閉）
    private var panelView: View? = null
    private var panelParams: WindowManager.LayoutParams? = null

    // 十字準星（可拖曳定位點擊位置）
    private var crosshairView: View? = null
    private var crosshairParams: WindowManager.LayoutParams? = null

    private var isPanelVisible = false
    private val handler = Handler(Looper.getMainLooper())

    // ===== 生命週期 =====

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        clickInterval = intent?.getLongExtra("interval", 500L) ?: 500L

        createNotificationChannel()
        startForeground(1, buildNotification())

        showBubble()
        showCrosshair()

        return START_STICKY
    }

    override fun onDestroy() {
        isClicking = false
        isRunning = false
        removeBubble()
        removePanel()
        removeCrosshair()
        super.onDestroy()
    }

    // ===== 通知（前景服務必備）=====

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "autoclicker_channel",
                "自動連點器",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "連點器執行中"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "autoclicker_channel")
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("自動連點器執行中")
            .setContentText("點擊通知返回設定")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    // ===== 懸浮氣泡 =====

    private fun showBubble() {
        val inflater = LayoutInflater.from(this)
        bubbleView = inflater.inflate(R.layout.floating_bubble, null)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        // 拖曳 + 點擊切換面板
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        bubbleView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams!!.x
                    initialY = bubbleParams!!.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (dx * dx + dy * dy > 25) moved = true
                    bubbleParams!!.x = initialX + dx.toInt()
                    bubbleParams!!.y = initialY + dy.toInt()
                    windowManager.updateViewLayout(bubbleView, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) togglePanel()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(bubbleView, bubbleParams)
    }

    private fun removeBubble() {
        bubbleView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        bubbleView = null
    }

    // ===== 控制面板 =====

    private fun togglePanel() {
        if (isPanelVisible) {
            removePanel()
        } else {
            showPanel()
        }
    }

    private fun showPanel() {
        val inflater = LayoutInflater.from(this)
        panelView = inflater.inflate(R.layout.floating_control, null)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (bubbleParams?.x ?: 50) + 80
            y = (bubbleParams?.y ?: 200)
        }

        val btnPlay = panelView!!.findViewById<ImageButton>(R.id.btn_play)
        val btnStop = panelView!!.findViewById<ImageButton>(R.id.btn_stop)
        val btnClose = panelView!!.findViewById<ImageButton>(R.id.btn_close)
        val tvStatus = panelView!!.findViewById<TextView>(R.id.tv_status)

        fun updateUI() {
            tvStatus.text = if (isClicking) "連點中..." else "已暫停"
            btnPlay.setImageResource(
                if (isClicking) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
        }

        updateUI()

        // 播放 / 暫停
        btnPlay.setOnClickListener {
            isClicking = !isClicking
            if (isClicking) {
                // 記錄十字準星的螢幕座標
                updateTargetFromCrosshair()
            }
            updateUI()
        }

        // 停止並關閉服務
        btnStop.setOnClickListener {
            stopSelf()
        }

        // 收起面板
        btnClose.setOnClickListener {
            removePanel()
        }

        windowManager.addView(panelView, panelParams)
        isPanelVisible = true
    }

    private fun removePanel() {
        panelView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        panelView = null
        isPanelVisible = false
    }

    // ===== 十字準星 =====

    private fun showCrosshair() {
        val inflater = LayoutInflater.from(this)
        crosshairView = inflater.inflate(R.layout.floating_crosshair, null)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        val size = 80
        crosshairParams = WindowManager.LayoutParams(
            size, size,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 300
            y = 600
        }

        // 拖曳十字準星
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        crosshairView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = crosshairParams!!.x
                    initialY = crosshairParams!!.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    crosshairParams!!.x = initialX + (event.rawX - initialTouchX).toInt()
                    crosshairParams!!.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(crosshairView, crosshairParams)
                    // 即時更新目標座標
                    if (isClicking) updateTargetFromCrosshair()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(crosshairView, crosshairParams)
    }

    private fun removeCrosshair() {
        crosshairView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        crosshairView = null
    }

    private fun updateTargetFromCrosshair() {
        crosshairParams?.let { params ->
            // 十字準星中心的螢幕座標
            targetX = params.x + 40f  // 40 = size/2
            targetY = params.y + 40f
        }
    }
}
