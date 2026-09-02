# What the eLCG brand actually is — icon provenance and the Jhpiego connection

*Established from git history at `D:\Android Projects\Intelehealth\elcg-fhw-mobileapp`, HEAD = `4579f67` (2026-09-01). Every claim below carries a SHA or a path. Inference is labelled inline. Refuted claims from the source investigations have been dropped and are not restated.*

---

## 1. The timeline — which launcher icon was live when

There has only ever been **one** `AndroidManifest.xml` in the app module (`app/src/main/AndroidManifest.xml`; the only other manifest in the repo is `klivekit/src/main/AndroidManifest.xml`, a library module with no `android:icon`). No flavour manifest has ever existed (`git log --all --diff-filter=A -- '*/AndroidManifest.xml'`). So a single hardcoded resource name has always governed the launcher icon for **every** flavour simultaneously. Its value changed exactly three times in the repo's entire history.

| Date | Commit / author | `android:icon` (and `roundIcon`) | Launcher icon that resulted |
|---|---|---|---|
| 2024-02-07 | `d9f1d0b3` — Intelehealth, "Initial commit" | — (no code yet) | repo opened; `README.md` only |
| 2024-03-07 | `1d380515` — Mithun Vaghela, "Repository initial commit" | `@mipmap/ic_launcher` | **Purple/pink eZazi monogram** (`app/src/main/res/mipmap-*/ic_launcher.png`). Single-flavour era. |
| 2024-08-30 | `e836844a` — arpansircardevelopment, "App Updated - 2.8 - beta (8)" | `@mipmap/ic_launcher` (last manifest touch `380487f1`, 2024-04-30) | **Purple — proven, not inferred.** The AAB `eZAZI2.8-beta-202408301254-production-release.aab` was committed (33,344,300 B, 3190 entries). Its `base/res/mipmap-xxxhdpi-v4/ic_launcher.png` hashes to `ab766c1a…`, byte-identical to today's `app/src/main/res/mipmap-xxxhdpi/ic_launcher_india.png`. Bundle contains no Nepal and no `elcg_square` asset. |
| 2026-04-22 | `23e343a4` + `82dfd1d6` — hatanvirintele, "(ez-786) ELCG white labeling…" | `@mipmap/ic_launcher` | `elcg` flavour born (`app/whitelabel.gradle`, `applicationId "org.intelehealth.elcg"`). First elcg icon set (webp, 5 densities) and `app_name` = **`eLCG नेपाल`** land here. ezazi builds: purple. |
| 2026-04-27 | `eba05421` — hatanvirintele, "(ez-786) ELCG white labeling completed" | `@mipmap/ic_launcher` | Second elcg generation: the webp set is deleted and replaced by `ic_launcher.png` ×5 plus the first `mipmap-anydpi-v26/ic_launcher.xml`. |
| **2026-05-13** | `552cb97c` — hatanvirintele, "Elcg applogo and small notification icon updated, EZ-824 resolved" | `@mipmap/ic_launcher` | **Teal Jhpiego window opens.** Third and final elcg generation: `ic_launcher.webp`, `ic_launcher_round.webp`, `ic_launcher_foreground.webp` ×5 densities, both anydpi-v26 XMLs, `values/ic_launcher_background.xml` (#FFFFFF), `ic_launcher-playstore.png`, `drawable/ezazi_logo.png`. An `elcg*` build from here would have shown the teal mark. |
| **2026-06-19** | `5d8f7b77` — zKaveri, "EZ-935 - Changed logo and icons for Ezazi nepal on all required screens." | → `@mipmap/app_lanucher_logo_nepal` | **Launcher window closes after 37 days.** The new name exists only in `app/src/main/res/mipmap-xhdpi/`; no flavour overrides it. Both flavours now get the **green mother-and-baby** art. |
| 2026-07-01 | `df78af58` — zKaveri, "EZ-989 - app icon is very small compared to screen size on app launcher screen." | unchanged (its only manifest edit is the splash theme) | Main's `ic_launcher*` renamed to `ic_launcher*_india` at 5 densities plus both anydpi XMLs. Purple monogram becomes permanently unreferenced. `app/src/main/res/xml/authenticator.xml:5,7` is left pointing at `@mipmap/ic_launcher`, which now exists in **no** source set an ezazi build can see. |
| **2026-07-03** | `c9f7b91c` — zKaveri, "App icon related changes." | → `@mipmap/elcg_square_full_bg_icon` | Current value (`AndroidManifest.xml:41`, `:46`). 892×892, 309,002 B, **`mipmap-xhdpi` only, no adaptive XML**. Same commit repoints `authenticator.xml` to `@mipmap/app_lanucher_logo_nepal` — which is what un-breaks the ezazi build, and which retires the teal `ic_launcher`'s **last** consumer. Total live span of the teal `ic_launcher` resource: 2026-05-13 → 2026-07-03, 51 days, of which 37 were the launcher slot. |
| 2026-08-26 | `bb21866` (tip of `dev_kaveri_nepal_sprint_46`); build `eZAZI3.1.1-202608261843-ezazi-production-release.apk`, `org.intelehealth.ezazi`, versionCode 18 | `@mipmap/elcg_square_full_bg_icon` (last manifest touch `09137a99`, 2026-07-14) | **Green.** *Inference, clearly marked:* no 3.1.1 artifact survives on disk, only its `output-metadata.json`; this is manifest-dating, unlike the 2.8-beta case which was opened and hashed. |
| 2026-09-01 | `aa0c99cc`, `4579f67` — the white-label rebuild | unchanged | `app/src/elcg` → `app/src/elcgNepal` as a **byte-identical** rename (blobs `e546ab7c` mdpi, `29cf6c68` hdpi, `f52d0034` xhdpi, `6aa81e87` xxhdpi, `99d1b24f` xxxhdpi, `29715ded` `drawable/ezazi_logo.png` — all unchanged from `bb21866`). Today's `elcgNepalProductionDebug` APK still carries the green icon. |

Two facts govern every line above and are easy to miss:

- **`minSdk 26`** (`app/build.gradle:28`). Every supported device is adaptive-icon capable, so `mipmap-anydpi-v26` outranks every density bucket. During the 2026-05-13 → 2026-06-19 window, main had no `anydpi-v26` foreground of its own (it arrives at `df78af58`), so the flavour's own adaptive XML plus its density `ic_launcher_foreground.webp` composed the teal mark over `#FFFFFF`. What was live was the **adaptive composite**, cropped to the system mask — not the pre-composed `.webp` bitmap.
- **`elcg_square_full_bg_icon` has no adaptive form and exists at one density.** With minSdk 26 that means 100% of devices legacy-shim a flat square, and `android:roundIcon` names the same square resource. EZ-989's complaint ("app icon is very small… on app launcher screen") is the symptom this shape produces.

---

## 2. What eLCG Nepal is

**"eLCG" names two different things, and the clinical one came first by two years.**

**LCG = the WHO Labour Care Guide**, the WHO's successor to the paper partograph — a clinical instrument, not a brand. It is in the first code drop (`1d380515`, 2024-03-07):

- `app/src/main/res/values/strings.xml:858` — `title_activity_epartogram` = "WHO LCG View"
- `app/src/main/res/values/strings.xml:1095` — `content_elcg_internet_require` = "WHO LCG view requires an internet connection…"
- `app/src/main/assets/epartogram.html` — `<title>WHO Labour Care Guide</title>`, `<h1>WHO LABOUR CARE GUIDE VIEW</h1>`
- `app/src/main/assets/elcg.html` — a saved Angular render (`_ngcontent-ouj-c184` attributes) scraped from the doctor web app
- An entire feature package: `app/src/main/java/org/intelehealth/ezazi/ui/elcg/` (20 files, incl. `WhoElcgActivity.kt`, `ELCGDataSource`, `ELCGRepository`, `ELCGViewModel`), plus `ObsDAO.java:957 getELCGObsByEncounterUuid(...)`

At that same commit `app_name` = "eZazi" and `applicationId` = `org.intelehealth.ezazi`, while the repository is already called `elcg-fhw-mobileapp`. The root `Release Note` ("Release 2.4-Beta 01/12/2023") describes the E-Partogram view as "presented through weblink" and cached — which is why `elcg.html` is a scraped web render. So "elcg" originally denoted the *electronic Labour Care Guide*, sibling of the ePartogram.

**eLCG = the brand** appears on 2026-04-22. `app/src/elcgNepal/res/values/strings.xml` (blob `eb35e012`, identical at `82dfd1d6`, `bb21866` and HEAD):

```
<string name="app_name" translatable="false">eLCG नेपाल</string>
<string name="notification_title">eLCG is running in the background</string>
```

So the brand was never plain "eLCG" — it was **"eLCG Nepal"**, a country-deployment brand, from the day the flavour existed. It is named *after* the clinical instrument, which is why the two meanings collide in one word.

**The deployment.** Nepal is the only deployment line in the repo. Of 35 refs (`git branch -a`), the long-lived branches are `development_master_nepal_deployment` and its variants (`_calendar`, `_kaveri_bugfix`, `_sp_39`, `_sp_39_tvr_up`, `_sp_41_tvr`), `dev_kaveri_nepal_sprint_46`, `eZazi_Stage3_and_Nepali_Calendar`, `eZazi_final_nepal_calendar_implementation`. There is no `*_india_deployment` or any other country line. All ticket ids are `EZ-###` (range EZ-451 → EZ-1080) — the tracker is organised around the eZazi *product*; eLCG Nepal is a deployment of it. *(Note: `development_master_nepal_merge_sp_38_tvr_elcg_wl` is often cited as a branch; it is not a ref — no ref anywhere in this repo contains the string "elcg". The name survives only inside merge-commit messages `2a97f05c` and `8d957c59`.)*

Localisation confirms the deployment is thin-skinned: there is **no `values-ne`** (`app/src/main/res/` carries ten Indian locales plus `values-ru`, all inherited from the Intelehealth IDA base). Nepal adaptation is at the data layer instead — Bikram Sambat via `NepaliDateConverter.java` and `app/src/main/assets/stage3.html` (`IS_NEPAL_CLIENT` at lines 305, 342, 378, 487), a 5-digit postal rule (`strings.xml:1132`, comment "For nepal deployment"), and a district-free address model. `Stage3DataTransformer.kt:44` decides "Nepal" at runtime from `serverUrl.contains("nepal")`, not from the flavour.

**Jhpiego.** The word "Jhpiego" appears **nowhere as text** — `git grep -I -i -E "jhpiego|family welfare|department of health services|ministry of health"` across all 356 commits on all refs returns nothing. The branding exists only as pixels. Opening the images in `app/src/elcgNepal/`:

- `res/drawable/home_logo.png` — full Jhpiego wordmark with the "j;" mark and the tagline "Saving lives. Improving health. Transforming futures.", in teal
- `res/drawable/login_screen_icon.jpeg` and `res/drawable/logo_ezazi.jpeg` — the same wordmark, stacked
- `res/drawable/home_logo_older.png` — wordmark without the tagline
- `ic_launcher-playstore.png`, `res/drawable/ezazi_logo.png`, `res/mipmap-*/ic_launcher.webp` — the teal "j;" mark alone

The palette matches: `app/src/elcgNepal/res/values/colors.xml` overrides exactly 8 of 81 colour names, every one a primary-brand slot — `colorPrimary` `#1F6F78`, `colorPrimaryDark`/`newPrimaryColor` `#15565D`, `colorIconButtonBg` `#E6F4F5` — against main's eZazi indigo `#2e1e91`. And the substitution is name-for-name: main's `home_logo`, `ezazi_logo`, `login_screen_icon`, `logo_ezazi` each have a Jhpiego counterpart in the flavour. That is a complete, deliberate brand swap.

**What the repo can and cannot establish about Jhpiego.** It establishes the *brand hierarchy the team designed*: in the eLCG Nepal flavour, Jhpiego's mark outranked both Intelehealth's and eZazi's. It cannot establish the relationship — implementer, funder, licensee, prime contractor. Nothing in 356 commits names the organisation. *(External knowledge, not repo-derived, flagged as such: Jhpiego is a Johns Hopkins–affiliated global health non-profit that works with national health ministries on maternal and newborn care. That context is not evidence from this codebase.)*

**A third party, live on screen today.** Two images in **main**, opened directly, carry the Nepal national emblem and the text block *Government of Nepal / Ministry of Health and Food Safety / Department of Health Services / Family Welfare Division / Teku, Kathmandu, Nepal*:

- `app/src/main/res/drawable/elcg_nepal_home_screen_logo.png` — added by `a045d42e` (2026-06-12, zKaveri, "Keyboard issue resolved in elcg risk panel.")
- `app/src/main/res/drawable/nepal_ezazi_spalsh_icon.png` — added by `5d8f7b77` (2026-06-19)

They are wired at `activity_home_ezazi.xml:40`, `activity_login_ezazi.xml:26`, `activity_splash_ezazi.xml:36`, `layout-sw600dp/activity_login_ezazi.xml:25`. On the same day, `dd7ff75d` ("Intelehealth logo removed from splash screen") added `android:visibility="invisible"` to the TextView carrying `@drawable/power_by_ih_logo` — the vendor mark was **hidden, not deleted**; the view, string and drawable all still ship.

---

## 3. Resolving the contradiction — teal Jhpiego mark vs green mother-and-baby

**Both statements are true, because they are about different resource slots, and one slot was severed by a commit nobody connected to branding.**

`5d8f7b77` (2026-06-19) is the severance. In one commit it moved the shared manifest off `@mipmap/ic_launcher` **and** repointed the three on-screen logo slots off flavour-overridable names:

```
activity_home_ezazi.xml:   @drawable/ic_home_logo        → @drawable/elcg_nepal_home_screen_logo
activity_login_ezazi.xml:  @drawable/login_screen_icon   → @drawable/nepal_ezazi_spalsh_icon
activity_splash_ezazi.xml: @drawable/logo_ezazi          → @drawable/nepal_ezazi_spalsh_icon
```

Every name on the left is one the elcg flavour overrides. Every name on the right exists only in main. The same commit commented out main's eZazi `app_name` and substituted `eLCG नेपाल`. That is the whole bypass, in one diff.

Nothing was deleted and nothing overruled the teal set — **the pointer moved**. `git grep -n -E "mipmap/ic_launcher\b" -- '*.xml'` returns zero hits at both `bb21866` and HEAD. The flavour's 25 launcher files remain complete, correctly structured, and unreachable.

### Which brand is on which surface at HEAD

| Surface | Reference | Resolves to | Both flavours? |
|---|---|---|---|
| Launcher + round icon | `AndroidManifest.xml:41,46` → `@mipmap/elcg_square_full_bg_icon` | **Green mother-and-baby**, main-only | Yes — identical for both |
| Account authenticator (Settings → Accounts) | `app/src/main/res/xml/authenticator.xml:5,7` → `@mipmap/app_lanucher_logo_nepal` | Green Nepal logo, main-only | Yes |
| Sync foreground notification | `app/src/main/java/org/intelehealth/ezazi/optimized_sync/OptimizedSyncForegroundInfo.kt:44` → `R.mipmap.app_lanucher_logo_nepal` | Green Nepal logo | Yes |
| 4 notification paths | `CardGenerationEngine.java:366`, `CallListenerBackgroundService.java:79`, `MyFirebaseMessagingService.java:237`, `AppNotification.java:40` → `R.drawable.app_lanucher_logi_nepal` | Green Nepal logo | Yes |
| 2 notification paths | `NotificationUtils.java:168`, `:204` → `R.drawable.ezazi_logo` | **Teal Jhpiego "j;"** in an elcgNepal build (flavour blob `29715ded` shadows main's `2d23921c`) | **No — the only live brand differentiation in the app** |
| Home / login / splash | three layouts above | **Government of Nepal / FWD lockup**, main-only | Yes |
| `app_name` | flavour `strings.xml` | `eZazi` / `eLCG नेपाल` | No — correctly split *(uncommitted; see §4)* |

**So: the teal Jhpiego mark is current as the designed brand identity of eLCG Nepal and is still live on exactly two notification code paths. The green mother-and-baby is current as the shipped launcher icon of both flavours. Neither displaced the other as a decision.**

`a4d938c1` (2026-07-07, zKaveri, "App notification icons fixed for ezazi nepal.", empty body) is what narrowed the teal mark to those two lines: it commented out `setSmallIcon(R.drawable.ezazi_logo)` at four sites (`CardGenerationEngine.java:367`, `CallListenerBackgroundService.java:78`, `MyFirebaseMessagingService.java:235`, `AppNotification.java:39`) and substituted the main-only Nepal drawable directly above or below each. `NotificationUtils.java` was simply not in the commit's file list. That is the direct undoing of `552cb97c`, which had moved those same five files *onto* `R.drawable.ezazi_logo` precisely so a flavour could swap them.

### The green asset is a Nepal asset despite its name

`elcg_square_full_bg_icon` reads as an eLCG asset and is not one. Its lineage runs entirely from `5d8f7b77`, whose subject is *"Changed logo and icons for **Ezazi nepal**"*, which added `app_lanucher_logo_nepal.png` (mipmap, 896×892) and `app_lanucher_logi_nepal.png` (drawable) — the **same blob** `aa0cf350`, 341,447 B, committed twice under two misspellings of "launcher". Opening all three: `elcg_square_full_bg_icon.png` (892×892, blob `e5091149`) and `app_lanucher_logo_nepal.png` are the same mother-and-baby artwork on the same green at different crops; `app_launcher_logo_nepal_rounded.png` (311,818 B, added by `c9f7b91c`, referenced by nothing) is the circular version. They are *different blobs* — no byte-level derivation is demonstrable — but the artwork is one image. The subject is specifically a seated woman in a sari **breastfeeding a newborn**, the idiom of ministry infant-feeding IEC material.

### What I am inferring, marked as inference

- **Intent.** `5d8f7b77` and `c9f7b91c` look like Nepal-delivery work done in `main` by a developer (zKaveri, Jun–Jul 2026) who did not know a flavour override existed, five weeks after a different developer (hatanvirintele, Apr–May 2026) had built it. Nothing warns when a flavour resource becomes unreachable: the app still builds and the icon still resolves. That `df78af58` carefully preserved the purple set as `*_india` shows its author knew two brands existed — yet left the shared manifest aiming both at one Nepal asset. That inconsistency is what an accident looks like, not a decision. **No commit message anywhere states the green icon is the eLCG brand.**
- **The `elcg_` prefix.** I read it as "eLCG" having become a synonym for "the Nepal deployment" in the team's vocabulary by mid-2026 (the elcg flavour's endpoints all pointed at `nezazi.intelehealth.org`). No commit explains the rename from `app_lanucher_logo_nepal` to `elcg_square_full_bg_icon`.
- **"Never shipped."** No release artifact of any elcg variant exists anywhere on disk (`find app -type d -name release` → only `app/production/release` and `app/ezaziProduction/release`, both `org.intelehealth.ezazi`); `app/whitelabel.gradle:22` carries the project's own comment that `org.intelehealth.elcg` has never shipped. Supporting but not proving: an elcg build on the pre-rebuild endpoints (`ELCG_*` → `nezazi`, which fails the `contains("nepal")` test at `Stage3DataTransformer.kt:44`) would have shown Gregorian dates to Nepali midwives. That is a strong hint, not proof — apps ship with worse. **Counter-evidence worth naming:** HEAD's `whitelabel.gradle` gives elcgNepal versionCode 18 / versionName 3.1.1 explicitly to continue the Nepal *product's* version across an applicationId change, which asserts an install base exists — under `org.intelehealth.ezazi`. A Play Console listing for `org.intelehealth.elcg` would falsify "never shipped"; git cannot.

---

## 4. What this means for the rebuild

*Facts and constraints only — the decisions below are the owner's.*

### Which asset belongs to which flavour, as the tree currently states it

**elcgNepal (Jhpiego / eLCG Nepal), `app/src/elcgNepal/` — 38 files, carried byte-identically from `app/src/elcg` by `aa0c99cc`:**
- Complete teal launcher family: `ic_launcher.webp`, `ic_launcher_round.webp`, `ic_launcher_foreground.webp` ×5 densities; `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`; `values/ic_launcher_background.xml` (#FFFFFF — note main defines the identical colour, so that one file overrides nothing); `ic_launcher-playstore.png`
- `drawable/ezazi_logo.png` (teal "j;") — the **only** live differentiator in the app today
- `drawable/home_logo.png`, `login_screen_icon.jpeg`, `logo_ezazi.jpeg`, `home_logo_older.png` — all four unreachable since `5d8f7b77` repointed the layouts
- `values/colors.xml` — 8 brand colour overrides, still functional
- `values/strings.xml` — `eLCG नेपाल`
- `ic_launcher_adaptive_back.png` / `ic_launcher_adaptive_fore.png` ×5 densities — added by `eba05421` and referenced by nothing, ever; the flavour's own adaptive XMLs name `@color/ic_launcher_background` and `@mipmap/ic_launcher_foreground` instead

**ezaziDefault (eZazi India), `app/src/ezaziDefault/` — currently only `res/values/strings.xml` (`eZazi`) and `res/values-hi-rIN/strings.xml`, and both are UNTRACKED (`?? app/src/ezaziDefault/`).** On a fresh clone this flavour contributes nothing. Its brand assets are still sitting in main under the `_india` suffix — `ic_launcher_india.png`, `ic_launcher_round_india.png`, `ic_launcher_foreground_india.png` ×5 densities plus `mipmap-anydpi-v26/ic_launcher_india.xml` and `ic_launcher_round_india.xml` — a complete, correct, adaptive-capable icon family that `git grep ic_launcher_india` shows is referenced by nothing outside its own two XMLs.

**Main (shared, currently branding both flavours as Nepal):** `elcg_square_full_bg_icon.png`, `app_lanucher_logo_nepal.png`, `app_lanucher_logi_nepal.png`, `app_launcher_logo_nepal_rounded.png`, `elcg_nepal_home_screen_logo.png`, `nepal_ezazi_spalsh_icon.png`, and (working-tree, uncommitted) `app_name` removed from `app/src/main/res/values/strings.xml` and devolved to the flavours.

### Mechanical facts any decision has to account for

1. **The manifest is the only lever, and no flavour can pull it.** No flavour manifest has ever existed. Icon differentiation can only run through the *resource name* the shared manifest happens to say — which is exactly the seam `552cb97c` built and `5d8f7b77` abandoned.
2. **Restoring `@mipmap/ic_launcher` would not, by itself, restore the teal icon.** elcgNepal's `mipmap-anydpi-v26/ic_launcher.xml:4` resolves its foreground via `@mipmap/ic_launcher_foreground`. elcgNepal supplies that name only as `.webp` in the five density buckets and has **nothing** in `mipmap-anydpi-v26` except the two XMLs. Main, since `df78af58`, supplies `app/src/main/res/mipmap-anydpi-v26/ic_launcher_foreground.png` (blob `2dfac4ff`, 238,857 B, 1024×1024) — opened: the green mother-and-baby art on transparency. With `minSdk 26`, `anydpi-v26` outranks every density bucket, and a flavour overrides main only at a matching name **and qualifier**. *(Inference from documented Android resource resolution; not build-verified.)* The name looks right, the build stays clean, and the wrong artwork ships — the same failure mode as the original bug.
3. **Neither brand has a working adaptive icon today.** `elcg_square_full_bg_icon` exists at one density with no `anydpi-v26` entry, so on every supported device it is a legacy-shimmed square, letterboxed rather than masked, and `roundIcon` names that same square. Both brands own complete adaptive machinery (`ic_launcher_india.xml` pair; elcgNepal's pair) and neither is wired.
4. **The notification surface has the same disease.** Five of seven paths hardcode main-only, misspelled names (`app_lanucher_logi_nepal`, `app_lanucher_logo_nepal`) that no flavour can override; only `ezazi_logo` at `NotificationUtils.java:168,204` is overridable. Any brand fix confined to the manifest leaves five notification paths green in an elcgNepal build.
5. **Roughly 3.5 MB of unreferenced launcher art ships in every APK of both brands.** `ic_launcher_legacy_playstore.png` 401,003 B × 5 copies (same blob `5026c7c3`, in `anydpi-v26` + hdpi/xhdpi/xxhdpi/xxxhdpi) = 2,005,015 B, referenced by nothing; main's `ic_launcher_foreground.png` 238,857 B × 5 = 1,194,285 B, dead in both flavours; `ic_launcher_background.png` 6,492 B × 5 = 32,460 B, dead (both adaptive XMLs use `@color/ic_launcher_background`); `app_launcher_logo_nepal_rounded.png` 311,818 B, dead. Total 3,543,578 B. Note also that three of these are **bitmaps sitting in `mipmap-anydpi-v26/`**, an XML-only qualifier by convention — harmless while unreferenced, and the reason for constraint (2) the moment anything references them. `mipmap-mdpi` has only the three `_india` files, so main's `ic_launcher_foreground` density ladder starts at hdpi.
6. **Firebase is mismatched on this branch right now.** `app/src/elcgNepal/google-services.json` (untracked, `.gitignore:2:*.json`) declares `project_id elcg-402e7` / `package_name org.intelehealth.elcg` and has no `firebase_url`; HEAD's `ELCG_NEPAL_*_REAL_TIME_FB_URL` all point at `ezazi-8712a`. The generated `app/build/generated/res/processElcgNepalProductionDebugGoogleServices/values/values.xml` has no `firebase_database_url` and no `default_web_client_id`. All three live RTDB call sites (`RealTimeDataChangedObserver.java:59`, `DeviceInfoUtils.java:17`, `FirebaseRealTimeDBUtils.java:30`) would pass an `ezazi-8712a` URL from a FirebaseApp authenticated against `elcg-402e7`. This is the inverse of the historical bug: `827181c4` (2026-05-19) → `a19e197` (2026-07-17) pointed the *ezazi* staging flavour's `EZAZI_STAGE_REAL_TIME_FB_URL` at `elcg-402e7` while its FirebaseApp came from `ezazi-8712a` — which is a coherent explanation for `elcg-402e7` reading empty in the console. `a19e197` then retyped that host as **`ezazi-402e7`**, which appears nowhere else in the repo or in any `google-services.json`, and that nonexistent host is what sits on `bb21866`, the pre-rebuild tip. `c609ce9d` removed both.

### Unresolved — for the owner to confirm

1. **Which launcher icon should eLCG Nepal ship?** Restoring the teal Jhpiego mark is a user-visible rebrand of a live deployment. Mitigating fact: elcgNepal changes applicationId to `org.intelehealth.elcg`, so it installs as a new app rather than updating in place.
2. **Was the green mother-and-baby ever an approved brand decision?** `c9f7b91c`'s message is "App icon related changes." with no rationale, landing two days after the EZ-989 rename. Nothing in git records who chose it or why it superseded the Nepal-named asset it was cropped from. The EZ-989 ticket may say.
3. **Was the artwork supplied by the Family Welfare Division as an existing IEC asset, or commissioned?** If it is a government asset it may be contractually required, which constrains what the rebuild is permitted to change. The launcher icon on every deployed device currently depicts breastfeeding — a programme-appropriate choice in an FWD maternal-health context, but a Play-listing and co-branding decision with an owner.
4. **Was Jhpiego's mark cleared for use?** It entered at `552cb97c` (EZ-824) with no licence or attribution file, and is live today in elcgNepal notifications. It is an external organisation's trademark.
5. **Was the June 2026 substitution of the Government of Nepal lockup for the Jhpiego wordmark a client decision or a side effect** of editing `main` instead of building the flavour? `5d8f7b77` has an empty body — and the government home-screen logo was already committed a week earlier, inside `a045d42e`, a commit about a keyboard bug. That timing is evidence the lockup arrived from outside engineering.
6. **Should the purple eZazi monogram be restored for ezaziDefault**, and is `_india` still a meaningful brand axis? The family is intact and orphaned; ezaziDefault's source set is untracked and holds no icons.
7. **Is `org.intelehealth.elcg` / `elcg-402e7` still the intended Nepal Play listing?** The versionCode-18 continuity in `whitelabel.gradle` implies yes; it is a one-way door for the installed base.
8. **Which ref was the field APK cut from?** The whole "never reached a device as a launcher icon" conclusion rests on the elcg flavour never shipping; confirming the field build's provenance (and any Play Console listing for `org.intelehealth.elcg`) is the one check git cannot do.

*Out of scope but flagged: `TestAccount` at the repo root has held committed test login credentials since `1d380515` in a repository described as open source. No values reproduced here; worth a separate, deliberate look.*