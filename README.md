# DocMorph

> **Open-source Android PDF Editor — View, Annotate & Export PDFs on Device**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Compose-2024.05-orange.svg)](https://developer.android.com/jetpack/compose)

---

## 🗂 Project Structure

```
DocMorph/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/docmorph/app/
│   │   │   │   ├── DocMorphApplication.kt          # Hilt entry point + PdfBox init
│   │   │   │   ├── MainActivity.kt                 # Single-activity host + splash
│   │   │   │   │
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── Annotation.kt           # Sealed hierarchy (TextBox, Drawing,
│   │   │   │   │   │   │                           #   Highlight, Shape, Strikethrough…)
│   │   │   │   │   │   ├── PdfDocument.kt          # Core domain model
│   │   │   │   │   │   └── RecentFileEntity.kt     # Room @Entity for recent files
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── DocMorphDatabase.kt     # Room database
│   │   │   │   │       ├── PdfRepository.kt        # Repository interface (contract)
│   │   │   │   │       ├── PdfRepositoryImpl.kt    # PdfBox-Android + SAF impl
│   │   │   │   │       └── RecentFileDao.kt        # Room DAO
│   │   │   │   │
│   │   │   │   ├── domain/
│   │   │   │   │   └── usecase/
│   │   │   │   │       └── PdfUseCases.kt          # OpenPdf · Save · Export ·
│   │   │   │   │                                   # DeletePage · RotatePage
│   │   │   │   │
│   │   │   │   ├── di/
│   │   │   │   │   └── AppModule.kt               # Hilt: DatabaseModule + RepositoryModule
│   │   │   │   │
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   └── DocMorphNavGraph.kt    # Screen routes + NavHost
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   └── Theme.kt              # Material 3 colours + typography
│   │   │   │   │   └── ui/
│   │   │   │   │       ├── home/
│   │   │   │   │       │   ├── HomeScreen.kt     # SAF picker + recent files grid
│   │   │   │   │       │   └── HomeViewModel.kt  # Recent files · open PDF
│   │   │   │   │       ├── viewer/
│   │   │   │   │       │   ├── ViewerScreen.kt   # AndroidPdfViewer bridge · ⋮ menu
│   │   │   │   │       │   └── ViewerViewModel.kt# Load · page nav · search · events
│   │   │   │   │       ├── editor/
│   │   │   │   │       │   ├── EditorScreen.kt   # Canvas overlay · toolbar · ⋮ menu
│   │   │   │   │       │   ├── EditorViewModel.kt# Annotations · undo/redo · page ops
│   │   │   │   │       │   ├── EditorColorPicker.kt  # ColorPicker overlay wrapper
│   │   │   │   │       │   └── SaveExportViewModel.kt# Save As · image/text export
│   │   │   │   │       └── components/
│   │   │   │   │           ├── Dialogs.kt        # TextInput · Export · PageJump ·
│   │   │   │   │           │                     #   ColorPicker
│   │   │   │   │           ├── StrokeWidthSlider.kt  # StrokeWidth + FontSize dialogs
│   │   │   │   │           └── PropertiesDialog.kt   # File metadata viewer
│   │   │   │   │
│   │   │   │   └── utils/
│   │   │   │       ├── Constants.kt             # MIME types · format keys · limits
│   │   │   │       ├── FileProviderUtils.kt     # FileProvider URI + share intents
│   │   │   │       ├── PdfUtils.kt              # Thumbnail render · page count · magic-byte check
│   │   │   │       └── UriUtils.kt             # formatFileSize · formatTimestamp · URI helpers
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── drawable/
│   │   │   │   │   └── ic_splash_logo.xml       # Vector splash icon
│   │   │   │   ├── values/
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   └── xml/
│   │   │   │       ├── backup_rules.xml
│   │   │   │       ├── data_extraction_rules.xml
│   │   │   │       └── file_provider_paths.xml  # FileProvider path config
│   │   │   │
│   │   │   └── AndroidManifest.xml              # Permissions · Activity · FileProvider
│   │   │
│   │   ├── test/                                # JVM unit tests (MockK + Turbine)
│   │   │   └── java/com/docmorph/app/
│   │   │       ├── domain/usecase/
│   │   │       │   └── PdfUseCasesTest.kt
│   │   │       ├── presentation/ui/
│   │   │       │   ├── home/HomeViewModelTest.kt
│   │   │       │   ├── viewer/ViewerViewModelTest.kt
│   │   │       │   └── editor/EditorViewModelTest.kt
│   │   │       └── utils/UriUtilsTest.kt
│   │   │
│   │   └── androidTest/                         # Instrumented tests (Hilt + Compose)
│   │       └── java/com/docmorph/app/
│   │           ├── HiltTestRunner.kt
│   │           └── DocMorphNavigationTest.kt
│   │
│   ├── build.gradle.kts                         # App module dependencies
│   └── proguard-rules.pro
│
├── gradle/
│   └── libs.versions.toml                       # Version catalog (single source of truth)
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── .gitignore
├── LICENSE                                      # MIT
├── CONTRIBUTING.md
└── README.md
```

---

## Features (MVP)

| Feature | Status |
|---|---|
| Open PDF via Android SAF | ✅ |
| Smooth PDF viewing (zoom, scroll) | ✅ |
| Recent files list (persistent) | ✅ |
| Three-dot context menu (⋮) | ✅ |
| Edit mode with annotation canvas | ✅ |
| Text box annotations | ✅ |
| Freehand drawing | ✅ |
| Highlight & Strikethrough | ✅ |
| Shapes (rectangle, circle, line) | ✅ |
| Comments / sticky notes | ✅ |
| Eraser tool | ✅ |
| Undo / Redo (50-step stack) | ✅ |
| Save (flatten annotations to PDF) | ✅ |
| Export as PNG / JPEG / TXT | ✅ |
| Page rotation (CW / CCW) | ✅ |
| Page deletion | ✅ |
| Share PDF via system sheet | ✅ |
| Open PDF from external apps | ✅ |
| Material You dynamic colour | ✅ |

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────┐
│                  Presentation Layer                  │
│  HomeScreen  ─  ViewerScreen  ─  EditorScreen       │
│  HomeVM      ─  ViewerVM      ─  EditorVM           │
└──────────────────────┬──────────────────────────────┘
                       │  Use Cases
┌──────────────────────▼──────────────────────────────┐
│                  Domain Layer                        │
│  OpenPdfUseCase  SavePdfUseCase  ExportPdfUseCase   │
│  DeletePageUseCase  RotatePageUseCase               │
└──────────────────────┬──────────────────────────────┘
                       │  Repository interface
┌──────────────────────▼──────────────────────────────┐
│                   Data Layer                         │
│  PdfRepositoryImpl  (PdfBox-Android + SAF)          │
│  DocMorphDatabase   (Room — recent files)           │
└─────────────────────────────────────────────────────┘
```

**Pattern:** MVVM + Repository + Use Cases (Clean Architecture lite)  
**DI:** Hilt  
**Async:** Kotlin Coroutines + StateFlow / SharedFlow  
**Navigation:** Jetpack Navigation Compose (single back-stack)

---

## 🔧 Tech Stack

| Component | Library |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| PDF Viewing | AndroidPdfViewer 3.2.0 |
| PDF Editing | PdfBox-Android 2.0.27 |
| DI | Hilt 2.51 |
| Database | Room 2.6 |
| Navigation | Navigation Compose 2.7 |
| Async | Coroutines 1.8 |
| Min SDK | 26 (Android 8.0) |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 35

### Clone & Build
```bash
git clone https://github.com/Dhananjay-234/DocMorph.git
cd DocMorph
./gradlew assembleDebug
```

### Install on device
```bash
./gradlew installDebug
```

---

## Key Design Decisions

### Non-destructive overlay editing
Annotations are stored as a Kotlin `List<Annotation>` in the ViewModel (in memory during a session) and only **flattened into the PDF bytes via PdfBox when the user explicitly saves**. The original bytes are never modified in the editor canvas.

### AndroidView bridge for PDFView
`AndroidPdfViewer` is a traditional `View`, not a Composable. It is bridged using `AndroidView {}` inside Compose. Scrolling is disabled when an active draw tool is selected so drag gestures are captured by the annotation canvas instead.

### Single Activity
The entire app runs inside `MainActivity`. All routing is managed by Jetpack Navigation Compose with URI-encoded arguments passed between screens to avoid serialisation issues with `android.net.Uri`.

### Undo/Redo
A simple 50-entry capped `ArrayDeque` stores immutable `List<Annotation>` snapshots. No diff algorithm is needed since the lists are small per session.

---

## 🛣 Roadmap

### Phase 2
- [ ] MuPDF integration for accurate text search
- [ ] Page reordering (drag-and-drop)
- [ ] Form field detection and filling
- [ ] Annotation comments viewer panel

### Phase 3
- [ ] OCR (ML Kit)
- [ ] Digital signatures
- [ ] Auto-save with crash recovery
- [ ] Accessibility (TalkBack) audit

---

## 🤝 Contributing

1. Fork the repository
2. Create your branch: `git checkout -b feat/your-feature`
3. Commit: `git commit -m "feat: add your feature"`
4. Push: `git push origin feat/your-feature`
5. Open a Pull Request

Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting.

---

## 📄 License

```
MIT License — Copyright (c) 2026 Dhananjay-234
```

See [LICENSE](LICENSE) for full text.
