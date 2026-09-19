package com.kwb130212.macrov1

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import java.util.Locale

data class TapPoint(val x: Int, val y: Int, val delayMs: Long)

class MainActivity : Activity() {
    private lateinit var pointsBox: LinearLayout
    private lateinit var status: TextView
    private lateinit var packageInput: EditText
    private lateinit var intervalInput: EditText
    private lateinit var webhookInput: EditText
    private val points = mutableListOf<TapPoint>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        loadConfig()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            setBackgroundColor(Color.rgb(15, 15, 20))
        }
        fun text(value: String, size: Float = 16f) = TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(0, 8, 0, 8)
        }
        root.addView(text("매크로v1.0", 28f))
        root.addView(text("백지헌의 캣히어로 매크로", 18f))
        status = text("● 대기", 14f)
        root.addView(status)

        packageInput = EditText(this).apply {
            hint = "대상 게임 패키지 (예: com.example.game)"
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); singleLine = true
        }
        root.addView(packageInput)

        intervalInput = EditText(this).apply {
            hint = "기본 간격(ms) — 최소 20"; inputType = 2
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); singleLine = true
        }
        root.addView(intervalInput)

        webhookInput = EditText(this).apply {
            hint = "웹훅 URL (선택, HTTPS만)"
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); singleLine = true
        }
        root.addView(webhookInput)

        root.addView(Button(this).apply {
            text = "입력 지점 추가"; setOnClickListener { addPointDialog() }
        })
        pointsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(pointsBox)

        root.addView(Button(this).apply {
            text = "접근성 권한 열기"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = "설정 저장"; setOnClickListener { saveConfig(); toast("저장됨") }
        })
        root.addView(Button(this).apply {
            text = "매크로 시작"
            setOnClickListener {
                saveConfig()
                if (points.isEmpty()) { toast("입력 지점을 1개 이상 추가하세요."); return@setOnClickListener }
                MacroAccessibilityService.instance?.let {
                    it.startMacro(); updateStatus()
                } ?: run {
                    toast("접근성 서비스를 먼저 켜세요.")
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        })
        root.addView(Button(this).apply {
            text = "매크로 정지"
            setOnClickListener { MacroAccessibilityService.instance?.stopMacro(); updateStatus() }
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun addPointDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(32, 8, 32, 8)
        }
        val x = EditText(this).apply { hint = "X"; inputType = 2 }
        val y = EditText(this).apply { hint = "Y"; inputType = 2 }
        val d = EditText(this).apply { hint = "이후 대기(ms), 비우면 기본값"; inputType = 2 }
        box.addView(x); box.addView(y); box.addView(d)
        AlertDialog.Builder(this).setTitle("입력 지점").setView(box)
            .setPositiveButton("추가") { _, _ ->
                val px = x.text.toString().toIntOrNull()
                val py = y.text.toString().toIntOrNull()
                if (px == null || py == null || px < 0 || py < 0) {
                    toast("좌표가 올바르지 않습니다."); return@setPositiveButton
                }
                val base = intervalInput.text.toString().toLongOrNull()?.coerceAtLeast(20L) ?: 100L
                val delay = d.text.toString().toLongOrNull()?.coerceAtLeast(20L) ?: base
                points.add(TapPoint(px, py, delay)); refreshPoints()
            }.setNegativeButton("취소", null).show()
    }

    private fun refreshPoints() {
        pointsBox.removeAllViews()
        points.forEachIndexed { index, p ->
            pointsBox.addView(TextView(this).apply {
                text = String.format(Locale.US, "%02d. (%d,%d) → %dms", index + 1, p.x, p.y, p.delayMs)
                textSize = 15f; setTextColor(Color.WHITE); setPadding(0, 10, 0, 10)
                setOnLongClickListener { points.removeAt(index); refreshPoints(); true }
            })
        }
    }

    private fun saveConfig() {
        getSharedPreferences("macro", MODE_PRIVATE).edit()
            .putString("targetPackage", packageInput.text.toString().trim())
            .putLong("interval", intervalInput.text.toString().toLongOrNull()?.coerceAtLeast(20L) ?: 100L)
            .putString("webhook", webhookInput.text.toString().trim())
            .putString("points", points.joinToString(";") { "${it.x},${it.y},${it.delayMs}" })
            .apply()
    }

    private fun loadConfig() {
        val p = getSharedPreferences("macro", MODE_PRIVATE)
        packageInput.setText(p.getString("targetPackage", ""))
        intervalInput.setText(p.getLong("interval", 100L).toString())
        webhookInput.setText(p.getString("webhook", ""))
        points.clear()
        p.getString("points", "")?.split(";")?.forEach { raw ->
            val a = raw.split(",")
            if (a.size == 3) {
                val x = a[0].toIntOrNull(); val y = a[1].toIntOrNull(); val d = a[2].toLongOrNull()
                if (x != null && y != null && d != null && x >= 0 && y >= 0 && d >= 20) points.add(TapPoint(x, y, d))
            }
        }
        refreshPoints()
    }

    private fun updateStatus() {
        status.text = if (MacroAccessibilityService.instance?.running == true) "● 실행 중" else "● 대기"
    }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
