package com.kun.miplaymanager

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // 服务器地址（可修改，保存在 SharedPreferences）
    private val PREFS_NAME = "MiPlayPrefs"
    private val KEY_HOST = "server_host"
    private val KEY_PORT = "server_port"
    private val DEFAULT_HOST = "192.168.1.10"
    private val DEFAULT_PORT = 8092
    private val PRESET_LAN_HOST = "192.168.1.10"
    private val PRESET_TAILSCALE_HOST = "100.88.1.6"

    private var serverHost = DEFAULT_HOST
    private var serverPort = DEFAULT_PORT
    private val baseUrl get() = "http://$serverHost:$serverPort"
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    // UI 元素
    private lateinit var tvStatus: TextView
    private lateinit var tvActiveSource: TextView
    private lateinit var tvDevice: TextView
    private lateinit var tvMiplayState: TextView
    private lateinit var tvDlnaState: TextView
    private lateinit var tvSoundCard: TextView
    private lateinit var tvVolume: TextView
    private lateinit var tvPlayback: TextView
    private lateinit var seekVolume: SeekBar
    private lateinit var switchMute: Switch
    private lateinit var btnRefresh: Button
    private lateinit var btnPlayPause: Button
    private lateinit var btnStop: Button
    private lateinit var btnNext: Button
    private lateinit var btnPrev: Button
    private lateinit var btnStopAll: Button
    private lateinit var btnClearMiplay: Button
    private lateinit var btnClearDlna: Button
    private lateinit var btnTestSound: Button
    private lateinit var tvLog: TextView
    private lateinit var scrollLog: ScrollView
    private lateinit var tvServerAddr: TextView
    private lateinit var etServerHost: EditText
    private lateinit var etServerPort: EditText
    private lateinit var btnPresetLan: Button
    private lateinit var btnPresetTailscale: Button
    private lateinit var btnSaveServer: Button

    private var currentVolumeRaw = 10
    private var isMuted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadServerSettings()
        setContentView(R.layout.activity_main)
        initViews()
        setupListeners()
        updateServerDisplay()
        refreshAll()
    }

    private fun loadServerSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        serverHost = prefs.getString(KEY_HOST, DEFAULT_HOST) ?: DEFAULT_HOST
        serverPort = prefs.getInt(KEY_PORT, DEFAULT_PORT)
    }

    private fun saveServerSettings(host: String, port: Int) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putString(KEY_HOST, host)
            .putInt(KEY_PORT, port)
            .apply()
        serverHost = host
        serverPort = port
    }

    private fun updateServerDisplay() {
        tvServerAddr.text = "服务器：$serverHost:$serverPort"
        etServerHost.setText(serverHost)
        etServerPort.setText(serverPort.toString())
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvActiveSource = findViewById(R.id.tvActiveSource)
        tvDevice = findViewById(R.id.tvDevice)
        tvMiplayState = findViewById(R.id.tvMiplayState)
        tvDlnaState = findViewById(R.id.tvDlnaState)
        tvSoundCard = findViewById(R.id.tvSoundCard)
        tvVolume = findViewById(R.id.tvVolume)
        tvPlayback = findViewById(R.id.tvPlayback)
        seekVolume = findViewById(R.id.seekVolume)
        switchMute = findViewById(R.id.switchMute)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnStop = findViewById(R.id.btnStop)
        btnNext = findViewById(R.id.btnNext)
        btnPrev = findViewById(R.id.btnPrev)
        btnStopAll = findViewById(R.id.btnStopAll)
        btnClearMiplay = findViewById(R.id.btnClearMiplay)
        btnClearDlna = findViewById(R.id.btnClearDlna)
        btnTestSound = findViewById(R.id.btnTestSound)
        tvLog = findViewById(R.id.tvLog)
        scrollLog = findViewById(R.id.scrollLog)
        tvServerAddr = findViewById(R.id.tvServerAddr)
        etServerHost = findViewById(R.id.etServerHost)
        etServerPort = findViewById(R.id.etServerPort)
        btnPresetLan = findViewById(R.id.btnPresetLan)
        btnPresetTailscale = findViewById(R.id.btnPresetTailscale)
        btnSaveServer = findViewById(R.id.btnSaveServer)
    }

    private fun setupListeners() {
        btnRefresh.setOnClickListener { refreshAll() }

        // 服务器地址设置
        btnPresetLan.setOnClickListener {
            etServerHost.setText(PRESET_LAN_HOST)
            etServerPort.setText(DEFAULT_PORT.toString())
            log("已填入家里 Wi-Fi 地址：$PRESET_LAN_HOST:$DEFAULT_PORT")
        }
        btnPresetTailscale.setOnClickListener {
            etServerHost.setText(PRESET_TAILSCALE_HOST)
            etServerPort.setText(DEFAULT_PORT.toString())
            log("已填入 Tailscale 地址：$PRESET_TAILSCALE_HOST:$DEFAULT_PORT（需手机开 Tailscale）")
        }
        btnSaveServer.setOnClickListener {
            val host = etServerHost.text.toString().trim()
            val portStr = etServerPort.text.toString().trim()
            if (host.isEmpty()) {
                Toast.makeText(this, "请输入主机地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val port = portStr.toIntOrNull() ?: DEFAULT_PORT
            saveServerSettings(host, port)
            updateServerDisplay()
            log("✓ 服务器地址已保存：$host:$port")
            Toast.makeText(this, "已保存，正在连接 $host:$port", Toast.LENGTH_SHORT).show()
            refreshAll()
        }

        // 音量滑块：0-15 原始值
        seekVolume.max = 15
        seekVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) {
                    val pct = p * 100 / 15
                    tvVolume.text = "音量：$p / 15（$pct%）"
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                setVolume(sb?.progress ?: 10)
            }
        })

        // 静音开关
        switchMute.setOnCheckedChangeListener { _, checked ->
            setMute(checked)
        }

        // 播放控制
        btnPlayPause.setOnClickListener { control("toggle") }
        btnStop.setOnClickListener { control("stop") }
        btnNext.setOnClickListener { control("next") }
        btnPrev.setOnClickListener { control("prev") }

        // 断开连接
        btnStopAll.setOnClickListener { control("stop_all") }
        btnClearMiplay.setOnClickListener { disconnect("miplay") }
        btnClearDlna.setOnClickListener { disconnect("dlna") }

        // 测试音
        btnTestSound.setOnClickListener { testPlay() }
    }

    // ===== 状态刷新 =====
    private fun refreshAll() {
        tvStatus.text = "检查中..."
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val statusJson = getJson("$baseUrl/api/status")
                val mixerJson = getJson("$baseUrl/api/mixer")
                withContext(Dispatchers.Main) { updateUI(statusJson, mixerJson) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvStatus.text = "● 无法连接"
                    tvStatus.setTextColor(0xFFFF6B6B.toInt())
                    tvActiveSource.text = "当前来源：--"
                    tvDevice.text = "设备：--"
                    log("✗ 连接失败：${e.message}")
                    log("当前服务器：$serverHost:$serverPort")
                    log("家里用 192.168.1.10，外出用 100.88.1.6（需开 Tailscale）")
                }
            }
        }
    }

    private fun updateUI(status: JSONObject, mixer: JSONObject) {
        try {
            // 活跃来源
            val active = status.optString("active_source", "idle")
            tvActiveSource.text = "当前来源：${when (active) {
                "miplay" -> "妙播（AirPlay）"
                "dlna" -> "DLNA"
                "mpd" -> "MPD 音乐"
                else -> "空闲"
            }}"

            // MiPlay 状态
            val miplay = status.optJSONObject("miplay")
            val mpConnected = miplay?.optBoolean("connected", false) ?: false
            val mpVol = miplay?.optInt("volume", 67) ?: 67
            val mpState = miplay?.optString("state", "idle") ?: "idle"
            tvMiplayState.text = "妙播：${if (mpConnected) "● 已连接" else "○ 未连接"}（$mpState，音量 $mpVol%）"

            // DLNA 状态
            val dlna = status.optJSONObject("dlna")
            val dlnaName = dlna?.optString("friendly_name", "飞牛音箱") ?: "飞牛音箱"
            val dlnaState = dlna?.optString("transport_state", "NO_MEDIA_PRESENT") ?: ""
            val dlnaSubs = dlna?.optInt("subscribers", 0) ?: 0
            tvDlnaState.text = "DLNA：$dlnaName（$dlnaState，订阅 $dlnaSubs）"
            tvDevice.text = "设备：$dlnaName"

            // 声卡
            val mixerObj = mixer.optJSONObject("mixer")
            val card = mixerObj?.optJSONObject("card")
            val cardId = card?.optString("id", "?") ?: "?"
            val cardNum = card?.optInt("card", 0) ?: 0
            tvSoundCard.text = "声卡：$cardId（Card $cardNum）"

            // 音量（PCM）
            val controls = mixerObj?.optJSONArray("controls")
            for (i in 0 until (controls?.length() ?: 0)) {
                val c = controls!!.getJSONObject(i)
                if (c.optString("name") == "PCM") {
                    val ch = c.optJSONArray("channels")?.getJSONObject(0)
                    val pb = ch?.optJSONObject("playback")
                    val raw = pb?.optInt("raw", 10) ?: 10
                    val pct = pb?.optInt("percent", 67) ?: 67
                    val on = pb?.optBoolean("on", true) ?: true
                    currentVolumeRaw = raw
                    isMuted = !on
                    seekVolume.progress = raw
                    tvVolume.text = "音量：$raw / 15（$pct%）"
                    switchMute.isChecked = !on
                    break
                }
            }

            // 播放状态
            val playback = status.optJSONObject("playback")
            val pbState = playback?.optString("state", "stop") ?: "stop"
            tvPlayback.text = "播放：$pbState"

            tvStatus.text = "● 在线"
            tvStatus.setTextColor(0xFF3DDC84.toInt())
            log("✓ 状态已刷新（来源：$active）")
        } catch (e: Exception) {
            log("✗ 解析状态失败：${e.message}")
        }
    }

    // ===== 音量控制 =====
    private fun setVolume(raw: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("name", "PCM")
                    put("stream", "playback")
                    put("volume", org.json.JSONArray().apply { put(raw) })
                }.toString()
                val result = postJson("$baseUrl/api/mixer", body)
                withContext(Dispatchers.Main) {
                    if (result.optBoolean("success", false)) {
                        val pct = raw * 100 / 15
                        tvVolume.text = "音量：$raw / 15（$pct%）"
                        currentVolumeRaw = raw
                        log("✓ 音量设为 $raw / 15（$pct%）")
                    } else {
                        log("✗ 音量设置失败：${result.optString("error")}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { log("✗ 音量设置失败：${e.message}") }
            }
        }
    }

    private fun setMute(mute: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("name", "PCM")
                    put("stream", "playback")
                    put("switch", if (mute) "off" else "on")
                }.toString()
                val result = postJson("$baseUrl/api/mixer", body)
                withContext(Dispatchers.Main) {
                    if (result.optBoolean("success", false)) {
                        isMuted = mute
                        log(if (mute) "✓ 已静音" else "✓ 已取消静音")
                    } else {
                        log("✗ 静音设置失败：${result.optString("error")}")
                        switchMute.isChecked = isMuted
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    log("✗ 静音设置失败：${e.message}")
                    switchMute.isChecked = isMuted
                }
            }
        }
    }

    // ===== 播放控制 =====
    private fun control(action: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val body = JSONObject().apply { put("action", action) }.toString()
                val result = postJson("$baseUrl/api/control", body)
                withContext(Dispatchers.Main) {
                    if (result.optBoolean("success", false)) {
                        log("✓ 已执行：$action")
                        refreshAll()
                    } else {
                        log("✗ 控制失败：${result.optString("error")}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { log("✗ 控制失败：${e.message}") }
            }
        }
    }

    // ===== 断开连接 =====
    private fun disconnect(type: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = "$baseUrl/api/$type/disconnect"
                val result = postJson(url, "{}")
                withContext(Dispatchers.Main) {
                    if (result.optBoolean("success", false)) {
                        log("✓ 已清除${if (type == "miplay") "妙播" else "DLNA"}连接")
                        refreshAll()
                    } else {
                        log("✗ 清除失败：${result.optString("error")}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { log("✗ 清除失败：${e.message}") }
            }
        }
    }

    // ===== 测试音 =====
    private fun testPlay() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = postJson("$baseUrl/api/test-play", "{}")
                withContext(Dispatchers.Main) {
                    if (result.optBoolean("success", false)) {
                        val freq = result.optInt("frequency_hz", 440)
                        val secs = result.optInt("seconds", 2)
                        log("✓ 测试音播放中：${freq}Hz，${secs}秒（注意听音箱）")
                        Toast.makeText(this@MainActivity, "测试音播放中，注意听音箱", Toast.LENGTH_SHORT).show()
                    } else {
                        log("✗ 测试音失败：${result.optString("error")}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { log("✗ 测试音失败：${e.message}") }
            }
        }
    }

    // ===== HTTP 工具 =====
    private fun getJson(url: String): JSONObject {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: throw Exception("空响应")
            if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
            return JSONObject(body)
        }
    }

    private fun postJson(url: String, json: String): JSONObject {
        val req = Request.Builder().url(url)
            .post(json.toRequestBody(jsonType)).build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: "{}"
            return JSONObject(body)
        }
    }

    private fun log(msg: String) {
        runOnUiThread {
            val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            tvLog.append("[$time] $msg\n")
            scrollLog.post { scrollLog.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }
}
