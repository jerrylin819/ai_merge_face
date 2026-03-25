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

