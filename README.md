# 自動連點器 Auto Clicker

在任何 Android App 上方自動點擊指定位置的工具。

## 功能
- 懸浮氣泡控制面板，可在任何 App 上方操作
- 可拖曳十字準星定位點擊位置
- 可調整點擊間隔（50ms ~ 5000ms）
- 播放 / 暫停 / 停止控制

## 安裝步驟（零基礎新手版）

### 方法一：GitHub Actions 自動編譯（推薦，不需安裝任何開發工具）

1. **建立 GitHub 儲存庫**
   - 登入 GitHub → New Repository → 名稱輸入 `AutoClicker` → Create

2. **上傳專案檔案**
   - 在儲存庫頁面點 `uploading an existing file`
   - 把整個專案的所有資料夾和檔案拖曳上去
   - 按 `Commit changes`

3. **等待自動編譯**
   - 點上方 `Actions` 分頁
   - 會看到一個 workflow 正在執行（黃色圓圈）
   - 等它變成綠色打勾（約 3~5 分鐘）

4. **下載 APK**
   - 點進綠勾的 workflow run
   - 頁面最下方 `Artifacts` 區塊
   - 點 `AutoClicker-debug` 下載 zip
   - 解壓後得到 `app-debug.apk`

5. **安裝到手機**
   - 把 APK 傳到手機（Line 傳給自己 / Google Drive / USB）
   - 手機打開 APK 安裝（需允許「不明來源」安裝）

### 方法二：Android Studio 本地編譯

1. 安裝 [Android Studio](https://developer.android.com/studio)
2. File → Open → 選這個資料夾
3. 等 Gradle 同步完成
4. 手機開啟 USB 偵錯 → 連接電腦
5. 按 ▶ Run

## 使用方式

1. 開啟 App，授予兩項權限：
   - **懸浮視窗權限**：讓控制面板顯示在其他 App 上方
   - **無障礙服務**：讓 App 可以模擬點擊
2. 調整點擊間隔速度
3. 按「啟動連點器」→ App 自動縮小
4. 切換到目標 App
5. 拖曳 ⊕ 十字準星到要點擊的位置
6. 點擊綠色懸浮氣泡 → 展開控制面板
7. 按 ▶ 開始連點
8. 按 ⏸ 暫停 / ⏹ 停止

## 技術架構

| 元件 | 用途 |
|------|------|
| `MainActivity` | 設定介面、權限管理 |
| `FloatingClickService` | 前景服務 + 懸浮視窗 UI |
| `AutoClickAccessibilityService` | 透過 AccessibilityService 執行點擊 |

## 系統需求
- Android 7.0 (API 24) 以上
- 約 1.5 MB 安裝大小
