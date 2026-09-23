# Gemma4 Camera

## Quick Reference

| 項目 | 値 |
|------|-----|
| パッケージ | com.example.gemma4camera |
| 最小 SDK | 26 (Android 8.0) |
| ターゲット SDK | 35 |
| 言語 | Kotlin |
| UI | Jetpack Compose |
| 推論 | MediaPipe GenAI Tasks |
| モデル | Gemma 4 E2B (INT4, ~1.5GB) |

## プロジェクト概要

カメラ映像をオンデバイスの Gemma 4 E2B で解析し、説明文をリアルタイム生成して TTS で読み上げる Android アプリ。完全オフライン動作。

## ディレクトリ構成

| パス | 役割 |
|------|------|
| `app/src/main/java/.../` | Kotlin ソースコード |
| `app/src/main/java/.../ui/` | Compose UI |
| `app/src/main/java/.../inference/` | Gemma 推論エンジン |
| `app/src/main/res/` | リソース |

## ビルド

```bash
./gradlew assembleDebug    # デバッグビルド
./gradlew assembleRelease  # リリースビルド
```
