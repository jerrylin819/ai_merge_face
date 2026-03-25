# 🤖 AI助手與人臉識別整合系統

一個結合人臉識別、AI對話和語音處理的智能系統，使用 **Kotlin (Android)** 前端和 **Python (FastAPI)** 後端。

---

## 📋 目錄
- [功能特性](#功能特性)
- [技術棧](#技術棧)
- [專案結構](#專案結構)
- [快速開始](#快速開始)
- [API 文檔](#api-文檔)
- [配置說明](#配置說明)

---

## ✨ 功能特性

### 🔍 人臉識別
- 基於 **InsightFace** 的深度學習人臉識別
- 支援註冊、辨識、刪除人臉
- 可配置的相似度閾值（預設 0.4）
- 實時人臉檢測和身份驗證

### 🎤 語音處理
- **STT (語音轉文字)**：使用 OpenAI Whisper API
  - 支援多種音檔格式
  - 多語言支援（預設中文）
  
- **TTS (文字轉語音)**：使用 OpenAI TTS API
  - 多種聲音選擇（alloy 等）
  - 實時音檔生成

### 🤖 AI 對話
- 基於 **LangChain + GPT-4o-mini** 的智能對話
- 對話記憶功能（ConversationBufferMemory）
- 支援文字和語音對話流程

### 📊 管理界面
- Web 後台管理系統（`/admin`）
- 人臉數據庫可視化管理
- 批量上傳、改名、刪除功能

---

## 🛠️ 技術棧

| 層級 | 技術 | 說明 |
|------|------|------|
| **前端** | Kotlin | Android 應用開發 |
| **後端** | Python 3.8+ | FastAPI Web 框架 |
| **人臉識別** | InsightFace | buffalo_l 模型 |
| **AI 對話** | LangChain + OpenAI GPT-4o-mini | 智能對話引擎 |
| **語音 API** | OpenAI Whisper & TTS | 語音轉文字、文字轉語音 |
| **數據庫** | 本地文件系統 | face_database 目錄 |

---

## 📁 專案結構

```
ai_merge_face/
├── app/                        # Kotlin Android 應用
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/        # Kotlin 代碼
│   │   │   └── res/           # 資源文件
│   │   └── ...
│   └── build.gradle.kts       # 構建配置
│
├── python/                     # Python FastAPI 後端
│   ├── function/
│   │   ├── file_manager.py    # 臨時檔案管理
│   │   ├── stt.py             # 語音轉文字 (Whisper)
│   │   └── tts.py             # 文字轉語音 (TTS)
│   ├── face_engine.py         # 人臉識別引擎
│   ├── admin.py               # Web 管理後台
│   ├── test.py                # 主應用 (FastAPI 路由)
│   ├── requirements.txt       # Python 依賴
│   └── .env                   # 環境變數配置
│
├── build.gradle.kts           # Gradle 根構建配置
├── gradle.properties          # Gradle 屬性
├── local.properties           # 本地開發配置
└── README.md

```

---

## 🚀 快速開始

### 前置要求
- Python 3.8 以上
- Android Studio（開發 Android 應用）
- OpenAI API Key
- CUDA 支援（可選，加速人臉識別）

### Python 後端設置

1. **進入 Python 目錄**
   ```bash
   cd python
   ```

2. **安裝依賴**
   ```bash
   pip install -r requirements.txt
   ```

3. **配置環境變數**
   ```bash
   # 建立 .env 文件
   cat > .env << EOF
   api_key=your_openai_api_key_here
   EOF
   ```

4. **啟動伺服器**
   ```bash
   uvicorn test:app --reload --port 8000
   ```

5. **驗證伺服器**
   ```bash
   curl http://localhost:8000/
   # 應輸出：{"status":"ok","message":"FastAPI is running"}
   ```

### Android 應用設置

1. **在 Android Studio 中打開 app 目錄**
   ```bash
   # 或使用命令行
   ./gradlew build
   ```

2. **編譯並部署**
   ```bash
   ./gradlew installDebug
   ```

3. **配置伺服器地址**
   - 修改應用中的伺服器 URL（待 Android 代碼確認）

---

## 📡 API 文檔

### 基礎 URL
```
http://localhost:8000
```

### 1️⃣ 文字對話
**POST** `/chat`

請求體：
```json
{
  "message": "你好，今天天氣如何？"
}
```

回應：
```json
{
  "reply": "AI 的回覆文字"
}
```

---

### 2️⃣ 語音對話（完整流程）
**POST** `/chat_audio`

上傳音檔（支援 mp3, wav, m4a 等）

回應：
- **Body**: 音檔 (MP3)
- **Headers**:
  - `X-User-Text`: 識別的用戶文字 (Base64)
  - `X-Reply-Text`: AI 回覆文字 (Base64)

---

### 3️⃣ 文字轉語音
**POST** `/tts`

請求體：
```json
{
  "message": "請播放這段文字"
}
```

回應：MP3 音檔

---

### 4️⃣ 人臉識別
**POST** `/recognize`

上傳圖片，識別所有人臉

回應：
```json
{
  "success": true,
  "face_count": 2,
  "faces": [
    {
      "name": "Jerry",
      "confidence": 0.95,
      "bbox": [100, 50, 250, 280]
    },
    {
      "name": "unknown",
      "confidence": 0.35,
      "bbox": [300, 100, 420, 350]
    }
  ]
}
```

---

### 5️⃣ 註冊人臉
**POST** `/register`

參數：
- `name` (query): 人名
- `file` (form): 人臉圖片

回應：
```json
{
  "success": true,
  "message": "Registered new face：Jerry"
}
```

---

### 6️⃣ 刪除人臉
**DELETE** `/faces/{name}`

回應：
```json
{
  "success": true,
  "message": "已刪除 Jerry"
}
```

---

### 7️⃣ 列出已註冊人臉
**GET** `/faces`

回應：
```json
{
  "faces": ["Jerry", "Alice", "Bob"],
  "total": 3
}
```

---

### 8️⃣ 重新載入資料庫
**POST** `/reload`

回應：
```json
{
  "message": "資料庫已重新載入",
  "faces": ["Jerry", "Alice", "Bob"]
}
```

---

## 🔧 配置說明

### Python 環境變數 (.env)
```env
api_key=sk-...  # OpenAI API Key
```

### 人臉識別參數 (face_engine.py)
```python
engine = FaceEngine(
    db_path="face_database",        # 人臉數據庫路徑
    similarity_threshold=0.4         # 相似度閾值（0-1）
)
```

**閾值說明**：
- 值越低 → 識別越寬鬆（誤認率高）
- 值越高 → 識別越嚴格（漏認率高）
- 推薦值：0.4-0.5

### 數據庫結構
```
face_database/
├── Jerry/
│   ├── Jerry_1.jpg
│   ├── Jerry_2.jpg
│   └── ...
├── Alice/
│   ├── Alice_1.jpg
│   └── ...
└── Bob/
    └── Bob_1.jpg
```

---

## 🌐 Web 管理後台

訪問 `http://localhost:8000/admin` 進行可視化管理

功能：
- ✏️ 新增人臉（批量上傳）
- 📝 改名已註冊人臉
- 🗑️ 刪除人臉及相關圖片
- 🔄 重新載入數據庫

---

## 📦 依賴包

主要依賴（見 `python/requirements.txt`）：
- `fastapi` - Web 框架
- `uvicorn` - ASGI 伺服器
- `openai` - OpenAI API 客户端
- `langchain` - 對話鏈框架
- `langchain-openai` - LangChain OpenAI 集成
- `insightface` - 人臉識別模型
- `opencv-python` - 圖像處理
- `python-dotenv` - 環境變數管理

---

## 🐛 常見問題

### 1. 人臉識別效果不佳
- 增加訓練圖片數量（每個人 5+ 張）
- 調整相似度閾值
- 確保圖片質量（光線充足、臉部清晰）

### 2. OpenAI API 錯誤
- 檢查 API Key 是否正確
- 確認額度充足
- 檢查網路連接

### 3. 無法啟動伺服器
- 確認 Python 版本 ≥ 3.8
- 重新安裝依賴：`pip install -r requirements.txt --force-reinstall`
- 檢查埠 8000 是否被佔用

---

## 📞 支援與貢獻

歡迎提交 Issue 和 Pull Request！

---

## 📄 授權

MIT License - 詳見 LICENSE 文件

---

**最後更新**: 2026-03-25  
**作者**: jerrylin819
