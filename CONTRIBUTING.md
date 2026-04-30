# Contributing to DocMorph 🙌

Thank you for considering a contribution to DocMorph!  
This document explains the workflow, coding standards, and review process.

---

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How to Contribute](#how-to-contribute)
- [Development Setup](#development-setup)
- [Branch Naming](#branch-naming)
- [Commit Style](#commit-style)
- [Pull Request Checklist](#pull-request-checklist)
- [Architecture Overview](#architecture-overview)
- [Reporting Bugs](#reporting-bugs)

---

## Code of Conduct

Be respectful, inclusive, and constructive. We follow the
[Contributor Covenant v2.1](https://www.contributor-covenant.org/version/2/1/code_of_conduct/).

---

## How to Contribute

1. **Fork** the repository on GitHub.
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/<your-username>/DocMorph.git
   ```
3. **Create a branch** from `main`:
   ```bash
   git checkout -b feat/my-new-feature
   ```
4. Make your changes with tests.
5. **Run the full test suite**:
   ```bash
   ./gradlew test
   ./gradlew connectedAndroidTest   # requires a connected device/emulator
   ```
6. **Push** and open a **Pull Request** against `main`.

---

## Development Setup

| Tool | Version |
|---|---|
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17 |
| Android SDK | API 35 |
| Kotlin | 2.0 |

### First-time setup

```bash
# Clone
git clone https://github.com/Dhananjay-234/DocMorph.git
cd DocMorph

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

---

## Branch Naming

| Prefix | Purpose | Example |
|---|---|---|
| `feat/` | New feature | `feat/page-reorder` |
| `fix/` | Bug fix | `fix/crash-on-empty-pdf` |
| `docs/` | Documentation only | `docs/update-readme` |
| `refactor/` | Code refactor | `refactor/repo-interface` |
| `test/` | Tests only | `test/editor-viewmodel` |
| `chore/` | Build / tooling | `chore/bump-compose-bom` |

---

## Commit Style

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short summary>

[optional body]

[optional footer]
```

**Examples:**
```
feat(editor): add arrow shape tool
fix(viewer): crash when opening password-protected PDF
docs(readme): add roadmap Phase 3 items
test(home-vm): cover error-state flow
```

---

## Pull Request Checklist

Before opening a PR, make sure:

- [ ] All existing unit tests pass (`./gradlew test`)
- [ ] New code has corresponding unit tests
- [ ] No new lint warnings (`./gradlew lint`)
- [ ] Code follows the existing MVVM + Clean Architecture pattern
- [ ] No hardcoded strings — use `strings.xml`
- [ ] New screens/features are documented in `README.md`
- [ ] PR description explains *what* and *why*

---

## Architecture Overview

```
Presentation (Compose + ViewModel)
    ↓ calls
Domain (Use Cases)
    ↓ calls
Data (Repository + Room + PdfBox)
```

- **ViewModels** expose `StateFlow<UiState>` and `SharedFlow<Event>` — no Android
  framework classes leak into the ViewModel.
- **Use Cases** are thin single-responsibility wrappers around repository calls.
- **Repository** is an interface; `PdfRepositoryImpl` is the only concrete
  implementation. Mock the interface in tests.
- **Composables** collect state with `collectAsState()` and delegate all logic
  to the ViewModel.

---

## Reporting Bugs

Open an issue and fill in the **Bug Report** template, which includes:
- Device / Android version
- Steps to reproduce
- Expected vs actual behaviour
- Logcat output (if a crash)

---

Thank you for making DocMorph better! 🎉
