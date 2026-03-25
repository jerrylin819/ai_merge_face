package com.example.rokidglasses_project.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.rokidglasses_project.R
import com.example.rokidglasses_project.audio.AudioRecorder
import com.example.rokidglasses_project.audio.AudioPlayer
import com.example.rokidglasses_project.network.ApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import android.Manifest
import android.content.pm.PackageManager
import android.util.Base64
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import okhttp3.ResponseBody
import android.util.Log

class ChatFragment : Fragment() {

    private lateinit var tvReply: TextView
    private lateinit var btnStartRecord: Button
    private lateinit var btnStopRecord: Button

    private lateinit var audioRecorder: AudioRecorder
    private lateinit var audioPlayer: AudioPlayer

    private var recordingFile: File? = null
    private var isRecording = false

    // 【關鍵】回調給 MainActivity - 當偵測到「人臉辨識」關鍵字時
    var onFaceRecognitionRequested: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化 UI
        tvReply = view.findViewById(R.id.tvReply)
        btnStartRecord = view.findViewById(R.id.btnMic)
        btnStopRecord = view.findViewById(R.id.btnStopRecord)

        // 初始化錄音和播放組件
        audioRecorder = AudioRecorder(requireContext())
        audioPlayer = AudioPlayer(requireContext())

        setupButtons()
        updateUI("等待輸入或語音...")
    }

    // ===== 按鈕設置（簡化版） =====

    private fun setupButtons() {
        btnStartRecord.setup("開始錄音") { handleStartRecord() }
        btnStopRecord.setup("停止錄音並上傳") { handleStopRecord() }
    }

    // 擴展函數：簡化按鈕設置
    private fun Button.setup(text: String, listener: () -> Unit) {
        this.text = text
        setOnClickListener { listener() }
    }

    // ===== 開始錄音 =====

    private fun handleStartRecord() {
        if (!canStartRecording()) return

        try {
            recordingFile = audioRecorder.startRecording()
            isRecording = true
            updateUI("🎤 錄音中...\n請對著手機說話")
            updateButtonState(true)
        } catch (e: Exception) {
            updateUI("❌ 開始錄音失敗: ${e.message}")
            isRecording = false
        }
    }

    // ===== 停止錄音 =====

    private fun handleStopRecord() {
        if (!isRecording) {
            updateUI("沒有在錄音")
            return
        }

        try {
            val file = audioRecorder.stopRecording()?: run {
                updateUI("❌ 錄音失敗或檔案為空")
                isRecording = false
                updateButtonState(false)
                return
            }
            isRecording = false
            updateButtonState(false)

            if (!isRecordingValid(file)) return

            updateUI("⏳ 錄音完成，上傳中...\n檔案大小: ${file.length() / 1024}KB")
            uploadAudioToFastApi(file)
        } catch (e: Exception) {
            updateUI("❌ 停止錄音失敗: ${e.message}")
            isRecording = false
            updateButtonState(false)
        }
    }

    // ===== 檢查是否可以開始錄音 =====

    private fun canStartRecording(): Boolean {
        if (!ensureAudioPermission()) {
            updateUI("❌ 需要麥克風權限")
            return false
        }
        if (isRecording) {
            updateUI("⚠️ 已經在錄音中")
            return false
        }
        return true
    }

    // ===== 檢查錄音檔案是否有效 =====

    private fun isRecordingValid(file: File?): Boolean {
        if (file == null || !file.exists()) {
            updateUI("❌ 錄音檔不存在")
            return false
        }
        if (file.length() == 0L) {
            updateUI("❌ 錄音檔為空（可能沒有收到聲音）")
            return false
        }
        return true
    }

    // ===== 上傳音頻到 FastAPI =====

    private fun uploadAudioToFastApi(file: File) {
        lifecycleScope.launch {
            try {
                // 1️⃣ 包裝成 multipart
                val part = MultipartBody.Part.createFormData(
                    "file", file.name,
                    file.asRequestBody("audio/*".toMediaTypeOrNull())
                )

                // 2️⃣ 呼叫 API
                val response = ApiClient.api.chatAudio(part)

                // 3️⃣ 檢查錯誤
                if (!response.isSuccessful) {
                    updateUI("❌ 後端錯誤:\n${response.code()}")
                    return@launch
                }

                // 4️⃣ 提取音頻數據和文字
                val audioData = extractAudioResponse(response) ?: return@launch

                // 【🔑 關鍵邏輯】檢查 AI 回覆是否包含「人臉辨識」關鍵字
                if (shouldSwitchToFaceRecognition(audioData.replyText, audioData.userText)) {
                    updateUI("🔄 正在啟動人臉辨識模式...")
                    // 延遲 1.5 秒讓用戶看到過渡
                    delay(1500)

                    Log.d("ChatFragment", "onFaceRecognitionRequested 回调是否存在: ${onFaceRecognitionRequested != null}")
                    onFaceRecognitionRequested?.invoke()
                    Log.d("ChatFragment", "✅ 已调用回调")
                    return@launch
                }

                // 否則，播放 AI 的回覆
                playAudioResponse(audioData)

            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    // ===== 決定是否切換到人臉辨識 =====

    private fun shouldSwitchToFaceRecognition(replyText: String, userText: String): Boolean {
        // 🎯 檢查 AI 的回覆或用戶的語音中是否有相關關鍵字
        val keywords = listOf(
            "人臉辨識",
            "臉部識別",
            "啟用人臉",
            "開啟人臉",
            "face recognition",
            "recognize"
        )

        val combinedText = (replyText + " " + userText).lowercase()

        return keywords.any { keyword ->
            combinedText.contains(keyword.lowercase())
        }
    }

    // ===== 數據模型 =====

    private data class AudioResponse(
        val userText: String,
        val replyText: String,
        val audioBytes: ByteArray
    )

    // ===== 提取 API 響應 =====

    private fun extractAudioResponse(response: retrofit2.Response<ResponseBody>): AudioResponse? {
        val userText = decodeHeader(response.headers()["X-User-Text"])
        val replyText = decodeHeader(response.headers()["X-Reply-Text"])
        val audioBytes = response.body()?.bytes()

        if (audioBytes == null || audioBytes.isEmpty()) {
            updateUI("❌ 未收到音檔\n\n請重試")
            return null
        }

        return AudioResponse(userText, replyText, audioBytes)
    }

    // ===== Base64 解碼 =====

    private fun decodeHeader(encoded: String?): String {
        return encoded?.let {
            String(Base64.decode(it, Base64.DEFAULT), Charsets.UTF_8)
        } ?: "無法取得"
    }

    // ===== 播放音頻響應 =====

    private fun playAudioResponse(data: AudioResponse) {
        val resultText = """
            🎤 你說：${data.userText}
            ━━━━━━━━━━━━━━━━
            💬 AI 回覆：${data.replyText}
            ━━━━━━━━━━━━━━━━
            🔊 正在播放語音...
        """.trimIndent()

        updateUI(resultText)

        audioPlayer.play(
            audioData = data.audioBytes,
            onComplete = {
                updateUI(resultText.replace("🔊 正在播放語音...", "✅ 播放完成！可以繼續提問"))
            },
            onError = { error ->
                updateUI("$resultText\n\n❌ 播放失敗: $error")
            }
        )
    }

    // ===== 錯誤處理 =====

    private fun handleError(e: Exception) {
        updateUI(
            "❌ 處理失敗\n\n錯誤：${e.message}\n\n請檢查網路連線和 FastAPI"
        )
        e.printStackTrace()
    }

    // ===== UI 更新 =====

    private fun updateUI(message: String) {
        tvReply.text = message
    }

    private fun updateButtonState(recordingActive: Boolean) {
        btnStartRecord.isEnabled = !recordingActive
        btnStopRecord.isEnabled = recordingActive
    }

    // ===== 權限檢查 =====

    private fun ensureAudioPermission(): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQ_RECORD_AUDIO
            )
        }
        return granted
    }

    // ===== 清理資源 =====

    override fun onDestroy() {
        super.onDestroy()
        if (::audioRecorder.isInitialized) {
            audioRecorder.release()
        }
    }

    companion object {
        private const val REQ_RECORD_AUDIO = 1001
    }
}