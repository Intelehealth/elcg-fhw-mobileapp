# Architecture Review Memo

**Project:** eZAZI (elcg-fhw-mobileapp)  
**Document Type:** Formal Architecture Review  
**Date:** February 2026  
**Audience:** Engineering Leadership, Architects, Tech Leads  

---

## 1. Context

This memo summarizes the architectural and technical-debt assessment of the **eZAZI** Android mobile application. The review was triggered by recurring build failures, Play Store compliance warnings, and onboarding friction when new developers attempt to run the project from a fresh clone.

**Scope:** Build configuration, dependency management, toolchain alignment, compliance posture, and developer experience.

---

## 2. Executive Summary

The project is **not currently "clone → sync → run" ready** for a modern Android development environment. It combines legacy Gradle patterns with newer AGP/Kotlin requirements, lacks documented runtime configuration, and includes a native WebRTC binary that fails Android 15+ 16KB page-size compliance **(a mandatory Google Play requirement starting November 1, 2025, forcing apps targeting Android 15 (API 35) or higher to support 16 KB memory page sizes on 64-bit devices. This shift from traditional 4 KB pages improves system performance, reduces power consumption, and increases app launch speeds on devices with larger RAM capacities.)**. Remediation is feasible in phases; immediate attention is required for onboarding, compliance, and runtime stability.

---

## 3. Findings

### 3.1 Build & Sync Stability

| Finding | Severity | Location | Description |
|---------|----------|----------|-------------|
| Invalid module includes | **Critical** | `settings.gradle` | `:fcm` and `:mylib` were included but directories do not exist. Caused Gradle sync failure and Run button disabled. **Remediated:** removed from `settings.gradle`; only `:app` and `:klivekit` remain. |
| Toolchain skew | **High** | Root `build.gradle`, `gradle.properties` | compileSdk 36 + AGP 8.7.x + Kotlin 1.9.x require aligned Android Studio and command-line tools. SDK XML version mismatch and Kotlin JVM target errors observed. |
| Deprecated Gradle patterns | **Medium** | Root `build.gradle` | Dual `buildscript` + `plugins` usage; deprecated `allprojects { repositories }`. |

### 3.2 Configuration & Secrets

| Finding | Severity | Location | Description |
|---------|----------|----------|-------------|
| Missing Firebase config | **Critical** | `app/` | `google-services.json` absent for `devDebug`. Build fails without workaround; Firebase features non-functional at runtime. **Partial workaround:** `app/build.gradle` disables Google Services tasks when file is missing, allowing compile-only; runtime may still crash if app assumes Firebase init. |
| Endpoint config fragility | **High** | `app/environment.gradle` | Reads `endpoint.properties` from project root; `.toString()` on potentially null keys causes NPE if file is missing or keys absent. No documented schema. (File is currently committed in repo.) |
| Local SDK path | **Medium** | `.gitignore` | `local.properties` excluded. New developers get "SDK location not found" until they create it or set `ANDROID_HOME`. |

### 3.3 Cross-Platform & Resource Layout

| Finding | Severity | Location | Description |
|---------|----------|----------|-------------|
| Windows path separators | **High** | `app/resdir.gradle` | Uses `src\\main\\res`; fails or behaves incorrectly on macOS/Linux. |
| Nested resources | **Medium** | `app/resdir.gradle` | Creates nested `res/ezazi` under `res/`; AGP 9 will treat as error. |

### 3.4 Compliance & Native Dependencies

| Finding | Severity | Location | Description |
|---------|----------|----------|-------------|
| 16KB page-size non-compliance | **Critical** | `lib/arm64-v8a/libjingle_peerconnection_so.so` | WebRTC binary from `io.github.webrtc-sdk:android:104.5112.10` (via LiveKit 1.2.1) not aligned. Google Play policy (Nov 1, 2025) blocks new/updated apps targeting Android 15+ without 16KB support. **Partial remediation:** `klivekit/build.gradle` adds constraint to force `io.github.webrtc-sdk:android:137.7151.05`; validate APK with `apkanalyzer` or equivalent to confirm `.so` alignment. |
| Duplicate manifest permissions | **Low** | `AndroidManifest.xml` | `FOREGROUND_SERVICE` declared twice. |
| Manifest vs build minSdk | **Low** | `AndroidManifest.xml` vs `app/build.gradle` | `FOREGROUND_SERVICE_DATA_SYNC` has `minSdkVersion="34"`; app `minSdk` is 26. |

### 3.5 Dependency & Technical Debt

| Dependency | Current | Risk | Notes |
|------------|---------|------|-------|
| `io.livekit:livekit-android` | 1.2.1 | High | Old WebRTC; 16KB compliance issue. Upgrade path requires API migration. |
| `com.google.guava:guava` | 27.1-jre | High | Very old; security/compatibility risk. |
| `com.parse:parse-android` | 1.15.7 | High | Legacy/deprecated. |
| `io.socket:socket.io-client` | 1.0.0 | Medium | Major version behind. |
| `joda-time:joda-time` | 2.10.3 | Medium | Prefer `java.time` on Android. |
| `org.apache.commons:commons-lang3` | 3.6 | Medium | Security updates available. |

### 3.6 Documentation & Onboarding

| Finding | Severity | Description |
|---------|----------|-------------|
| Minimal README | **High** | README lacked prerequisites and setup; **remediated** with links to `docs/DEVELOPER_ONBOARDING_CHECKLIST.md` and `docs/ARCHITECTURE_REVIEW.md`. |
| No onboarding checklist | **High** | **Remediated:** `docs/DEVELOPER_ONBOARDING_CHECKLIST.md` added. |
| Tribal knowledge | **Medium** | `endpoint.properties`, Firebase config, build variants not documented. |

---

## 4. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Play Store rejection (16KB) | High | Blocking | Upgrade WebRTC/LiveKit or force compatible binary; validate with APK analyzer. |
| New developer cannot run app | High | High | Document setup; provide sample configs; fix path/resource issues. |
| Runtime crash (Firebase/native) | Medium | High | Add `google-services.json` stub or doc; capture crash stacktrace; validate WebRTC override. |
| Future AGP 9 breakage | Medium | Medium | Address deprecations (lint, buildFeatures, nested res) before upgrade. |
| Security exposure (old deps) | Medium | Medium | Prioritize Guava, Parse, Socket.IO updates. |

---

## 5. Recommendations

### 5.1 Phase 0: Stabilize Onboarding (1–2 days)

- **Documentation:** README expanded with links to onboarding and architecture docs. **Done.**
- **Developer checklist:** `docs/DEVELOPER_ONBOARDING_CHECKLIST.md` exists. **Done.**
- **Cross-platform:** Fix `resdir.gradle` path separators; restructure nested resources.
- **Endpoint safety:** Add null checks and clear error messages in `environment.gradle`; document `endpoint.properties` schema.

### 5.2 Phase 1: Compliance & Dependencies (3–7 days)

- **16KB page size:** Migrate LiveKit to 2.x (preferred) or force compatible WebRTC and validate `.so` alignment.
- **Firebase:** Document `google-services.json` placement; consider stub for dev builds.
- **Manifest:** Remove duplicate permissions; align minSdk usage.

### 5.3 Phase 2: Gradle Modernization (1–2 weeks)

- Move repositories to `dependencyResolutionManagement` in `settings.gradle`.
- Consolidate plugin management; remove `allprojects`.
- Resolve `kotlin-stdlib` forcing; remove redundant kapt if KSP-only.

### 5.4 Phase 3: Security & Lifecycle (Ongoing)

- Dependency upgrades (Guava, Socket.IO, AWS SDK, etc.).
- Replace or sunset Parse SDK.
- Introduce version catalog and dependency update automation.

---

## 6. Migration Plan (High Level)

```
Phase 0 (P0) ──► Phase 1 (P1) ──► Phase 2 (P2) ──► Phase 3 (P3)
   │                  │                  │                  │
   │                  │                  │                  └── Security, catalogs, CI
   │                  │                  └── Gradle cleanup, stdlib resolution
   │                  └── 16KB, Firebase, manifest
   └── Docs, paths, endpoint safety
```

**Estimated effort:**
- Phase 0: 1–2 days  
- Phase 1: 3–7 days  
- Phase 2: 1–2 weeks  
- Phase 3: Ongoing  

---

## 7. Appendix: Key File References

| Purpose | Path |
|---------|------|
| Root build config | `build.gradle` |
| App module | `app/build.gradle` |
| Library module | `klivekit/build.gradle` |
| Module includes | `settings.gradle` |
| Endpoint config | `app/environment.gradle` |
| Resource dirs | `app/resdir.gradle` |
| Manifest | `app/src/main/AndroidManifest.xml` |
| Gradle props | `gradle.properties` |

---

*Document version: 1.0 | Last updated: February 2026*
