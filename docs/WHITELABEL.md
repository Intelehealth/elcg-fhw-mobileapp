# White-Labelling — Reference Document

**eZazi / eLCG Android app (`elcg-fhw-mobileapp`)**

Compiled 2026-08-27/28 from a full R&D sweep of the white-label control surfaces.
Repo state at time of writing: branch `dev_kaveri_nepal_sprint_46`, HEAD `bb21866`.

> This document exists because the original white-label work shipped with **four empty commit
> bodies** and three subject lines of recorded intent. The next developer had no way to know that
> `552cb97c` was a deliberate seam rather than an incidental icon change, and the mechanism was
> bypassed within four weeks. Keep this file current.

---

## PART A — Decisions locked

| # | Decision |
|---|---|
| A1 | eLCG Nepal branding (including Devanagari `app_name`) is **valid**. So are its launchers, themes, drawables. The artefacts are correct — the *binding* is wrong. They belong to a variant, not to `main`. |
| A2 | Endpoint mapping: `nepalezazi` = Nepal **PROD**; `nezazi` = Nepal **dev/stage**; `testezazi` = default-product **dev/stage**; `ezazi` = default-product **PROD**. |
| A3 | Two `google-services.json` files (one per brand) is correct and stays. |
| A4 | Location/address data: **per-country files in per-deployment folders**. Deployments are country-wise; district-level scoping rejected. |
| A5 | OpenMRS Address Hierarchy as a data source is **out of scope**. |
| A6 | Client dimension = brand + country (`elcgNepal`, `ezaziDefault`), not brand alone. |
| A7 | Endpoint key scheme: `ELCG_NEPAL_PROD`, `ELCG_NEPAL_STAGE`, `EZAZI_DEFAULT_PROD`, … |
| A8 | Flavour folders carry **everything** per-deployment: assets, theming, strings, localisation. |
| A9 | Server dimension must tolerate a brand having only *some* environments. |
| A10 | ~~Colour-override strategy: **parked**, revisit later.~~ **RESOLVED 2026-09-02 by P15:** there is no colour-override strategy, because there are no colour overrides. Teal was Jhpiego’s temporary partner branding and is dropped; every brand keeps main’s original palette. The one surviving flavour colour is `ic_launcher_background` (`#40A47C`), which belongs to the launcher artwork, not to a palette. |
| A11 | eZazi flavour content is sourced from **`origin/ezazi_sprint_43_arpan`** (`6cc66ba`, 13 Jul 2026) — the newest clean unbranded eZazi state. Not `development_master`. |
| A12 | `.gitignore` stays as-is: **JSON remains untracked**; per-brand config is an out-of-band handover with a documented onboarding checklist. |
| A13 | Default/base client flavour is **`ezaziDefault`**. The product will not be deployed in India; if it ever is, `ezaziDefault` is that deployment. |
| A14 | **A9 resolved by duplication**: if a brand lacks a dev or stage environment, declare both keys with the same endpoints. Already the de-facto state for `ELCG_*`. |
| A15 | Brand name, flavour name, endpoint key, applicationId and displayed `app_name` are **five separate things**. A brand wanting a different display name never constrains the flavour name. |
| A16 | **`resdir.gradle` to be dissolved**: `src/main/res/ezazi` merges up into `src/main/res`. |
| A17 | **versionCode/versionName: independent per brand.** *Settled 2026-09-01:* `defaultConfig` = eZazi (**18 / 3.0.0**, matching the Play release); `elcgNepal` declares its own (**18 / 3.1.1**, matching the last Nepal delivery). `ezaziDefault` declares **nothing** — a flavour declaration would override `defaultConfig` and silently swallow a bump, which is what `bb21866` did in reverse. Non-default brands must declare their own. |
| A18 | **Signing stays as-is** — one key for all brands, applied via Android Studio’s signed-build dialog. *Revised 2026-09-01: a Gradle `signingConfig` was considered and **dropped** — no CI, one release engineer, nothing broken. The residual risk is that the process is undocumented, which a runbook fixes.* |
| A19 | No Nepali localisation exists or is required today; keep the seam in mind before Bangladesh ships. |

---

## PART B — How white-labelling works today

Four mechanisms, and no others. A repo-wide grep of every `.gradle` returns **zero** hits for
`manifestPlaceholders`, `resValue`, `resConfigs`, `signingConfig`, `applicationIdSuffix`,
`versionNameSuffix`, `splits`, `abiFilters`, `missingDimensionStrategy`, `matchingFallbacks`,
`variantFilter`, or per-flavour dependency configurations.

1. **Flavour property overrides** — `applicationId`, `versionCode`, `versionName`
2. **Six `buildConfigField`s per variant** — five endpoints from `endpoint.properties`
   (`SERVER_URL`, `REAL_TIME_FB_URL`, `LIVE_KIT_URL`, `SOCKET_URL`, `FB_RT_INSTANCE`) plus
   `ACTIVE_CRASH`, which **zero lines of code read**
3. **Res sourceSet overlay** at `app/src/elcg/res`
4. **Per-flavour `google-services.json`**

### Current state
Real but shallow, and currently bypassed. Of `app/src/elcg`'s 38 files (37 tracked), **three have
runtime effect**: `colors.xml`, `notification_title`, `drawable/ezazi_logo.png`. A merged build
artifact confirms the outcome: `ezaziStagingDebug` ships `app_name = "eLCG नेपाल"` rendered in
`#2e1e91` eZazi purple. **Both brands are mis-branded, in opposite directions.**

`BuildConfig.FLAVOR_client` is read at **exactly two sites in the entire codebase**
(`PatientAddressInfoFragment.java:478`, `:550`), both choosing a `":"` separator.

---

## PART C — The eZazi baseline

**`development_master` is not a separate eZazi product line.** It is an *ancestor snapshot* of the
current branch, frozen at 2026-03-25. `origin/development_master` = `5cfe698`, a no-op merge whose
tree is byte-identical to its second parent `94a98d6`; both parents are already ancestors of HEAD.
`git rev-list --left-right --count origin/development_master...HEAD` = `1 230` — the "1 ahead" *is*
that empty merge. **Nothing to merge back.**

It remains the reference photograph of unbranded eZazi, and it settles A2:

| | `development_master` | HEAD |
|---|---|---|
| `app_name` | `eZazi` | `eLCG नेपाल` |
| manifest icon | `@mipmap/ic_launcher` / `ic_launcher_round` | `@mipmap/elcg_square_full_bg_icon` |
| endpoint scheme | `PROD_* → ezazi`, `DEV_* → testezazi` | 6 cross-wired families |
| flavour sourceSets | **none** | + `elcg/` |
| white-label file | `environment.gradle`, **live** | `whitelabel.gradle` |
| `"nepal"` matches in tree | **zero** | pervasive |
| versionName / SDK | `3.0` / compileSdk **36** | `3.1.1` / compileSdk **34** (walked back in `10282832`) |

**The newest clean eZazi state is elsewhere.** Two sibling branches are unbranded eZazi, are newer,
contain `development_master`'s tip, and carry 12 commits present on neither it nor HEAD:

- **`origin/ezazi_sprint_43_arpan`** = `6cc66ba`, 13 Jul 2026 (**the A11 source of record**)
- **`origin/development_master_sp_41_tvr`** = `67c35ff`, 15 Jun 2026

---

## PART D — The original design, reconstructed

**Six commits, one author** (hatanvirintele, 2026-04-22 → 2026-05-21).
`git log --all -- app/whitelabel.gradle` returns **one commit**; the blob is byte-identical on all 15
refs that carry it. Written once, never edited.

| Commit | What it did |
|---|---|
| `23e343a4` | The engine. Replaced the single-dimension `beta` mechanism with `client × server` and one generic resolver composing a `<CLIENT>_<SERVER>_` prefix. Added `staging`. |
| `82dfd1d6` | The `app/src/elcg` sourceSet |
| `2eaf44b4` | `FlavorKeys.kt`, the typed-constants half of the dispatch |
| `eba05421` | Pointed ELCG at its own Firebase project |
| `552cb97c` | **The design thesis.** Moved notification icons from `R.mipmap.ic_launcher` to `R.drawable.ezazi_logo` *so a flavour sourceSet could swap them*, adding the elcg override in the same commit. |
| `0d50acaa` | Last touch |

**What was right:** two dimensions with one generic resolver; a real flavour sourceSet; per-brand
`google-services.json` placed correctly; own `applicationId`; the `FB_RT_DB → FB_RT_INSTANCE` rename
(one property now means "which host is this build talking to", escaping moved into
`AppConstants.java:173`); and the `552cb97c` resource-indirection method.

**What was never finished:**
- **No `app/src/ezazi/` sourceSet ever existed** — not on any branch, never created, never deleted.
  The default brand's assets stayed in `main`, so *brand 1 and "shared" are the same directory.*
  **This is the root architectural flaw.**
- `ACTIVE_CRASH` — Gradle half wired, zero consumers. Legibly an intended per-environment
  Crashlytics toggle, abandoned midway.
- **Zero documentation.**

**What a later, different author destroyed:** `df78af58` renamed main's `ic_launcher*` family to
`ic_launcher_india*`; `c9f7b91` repointed the manifest at `elcg_square_full_bg_icon`. That orphaned
**all 25 elcg launcher files** — a complete, self-consistent adaptive-icon set at five densities plus
`anydpi-v26` plus a matching background colour. **The mechanism works; the manifest stopped pointing
at it.**

---

## PART E — Design notes for the rebuild

### Replace the substring dispatch
`whitelabel.gradle:44-66` resolves the endpoint prefix by substring-matching the **composed variant
name**. Three failure modes:

- `ezaziDefault` and any future `ezaziBangladesh` both match `contains("ezazi")`; first branch wins.
- The server chain runs on the **same string**. A client flavour containing `Dev`/`Staging`/
  `Production` false-matches: `ezaziDevTrust` built as production → `ezaziDevTrustProductionDebug`
  → `contains("Dev")` fires → resolves `EZAZI_DEV`. **Production build, dev backend, compiles clean.**
- `else { domain = "EZAZI" }` silently points an unrecognised brand at eZazi's servers.

**Fix: each flavour declares its own key** (`ext.endpointKey = "ELCG_NEPAL"`), composed from the
flavours rather than the variant name. Adding brand #10 = add one flavour block.

### Fail fast on missing endpoint keys
A missing key yields Groovy `null`, emitted verbatim as a raw Java expression →
`public static final String SERVER_URL = null;`. Builds clean, NPEs in the field. A
configuration-time assertion that every enabled variant has all its keys is the highest-value
guardrail in the design. (With A14's duplication it never fires on missing environments — it still
catches typo'd key names.)

### Migrate off removed APIs
`applicationVariants.configureEach` is the legacy AGP variant API, **removed in AGP 9**. Its
replacement is `androidComponents.onVariants` / `beforeVariants`. `archivesBaseName` relies on a
Gradle convention removed in Gradle 9; `lintOptions` is deprecated in favour of `lint {}`.

### The string-shadowing rule
Android resolves resources on two independent axes — **sourceSet priority** (flavour beats main) and
**qualifier matching** (device config picks the folder). **Qualifier matching happens first, per
resource name.** A flavour override in plain `values/` is never consulted on a device that matches a
qualified bucket main defines.

`main/res/values-v21/strings.xml` holds 63 strings and **minSdk is 26**, so `-v21` matches **100% of
devices** — for those names a flavour override in `values/` is dead everywhere, not just on the
11 locales that redefine `app_name`.

**The fix inverts the model.** Not "main holds a default, flavour overrides it" but
**main holds everything shared; flavours exclusively own what is theirs.**

1. **Brand-varying names must not exist in `main` at all** — removed from `values/` *and every
   qualified bucket*, defined only in each flavour. Nothing left to shadow, and a flavour that
   forgets one fails at link time instead of shipping the wrong string.
2. Delete the legacy qualified debris (see the deferred table below).
3. Add a configuration-time check: enumerate every name any flavour defines, assert main does not
   define it in a different qualifier bucket.
4. Re-enable `MissingTranslation` (disabled at `app/build.gradle:50-54`) — it flags exactly the
   `translatable="false"` + translations-exist condition that produced this bug.

#### Supporting moves — REQUIRED, deferred pending verification

Do not delete or add anything here on the strength of this document. Each needs its own check first.

| Move | Verify before acting |
|---|---|
| Delete stale `values-v21` string overrides (63) | Diff each against the default bucket. The R&D found the **only** names that genuinely differ across qualifier buckets anywhere are `search_visits_hint`, `seconday_doct_val_txt`, and — in `values-w820dp` only — `am` and `click_to_enter`. Re-confirm independently, keep those, delete the rest. |
| Delete `values-v21/styles.xml` | Currently `<resources>>` — malformed root, declares nothing. A `-v21` styles override would win on every device, so this is a live trap even while empty. |
| Delete the 11 locale `app_name` overrides | `app_name` is `translatable="false"`, so translations are wrong on their own terms. Check the launcher label per locale before and after. |
| Re-enable `MissingTranslation` lint | Safe to trial: `abortOnError false` is also set, so it produces warnings, not failures. Turn on, read report, then decide. |
| Add the flavour/qualifier collision check | New code, no deletions. Make it *report* first; only fail the build once its output is clean. |

**Rule of thumb: make it report before you make it delete.**

#### Graduating a Nepal feature to the base product
Strings move **wholesale** from the flavour's `strings.xml` into main's, removed from the flavour in
the same change. Never duplicated across both — that reintroduces the shadowing bug.

### Dissolving `resdir.gradle` (A16)
`src/main/res/ezazi` — **362 files, 142 layouts** — is folded into `main` as a *second srcDir of the
same ResourceSet*. It therefore **cannot override anything**: a name collision between the two is a
hard build failure, not a win. It is a brand-named folder shared by every brand, and it is the folder
a developer will naturally reach for when adding brand resources.

The move is mostly relocation, not merging. Layouts, drawables, anim, menu, navigation and font have
unique filenames (zero file-resource collisions verified), so they move up a level unchanged. The
`values/` files need no content merge either — Android permits multiple XML files per `values/`
folder, and only *resource names* must be unique, not filenames. So `res/ezazi/values/strings.xml`
becomes `res/values/strings_ezazi.xml`, contents untouched; same for arrays, attrs, bools, dimens,
styles, themes.

Safe while the zero-collision invariant holds (values/: 1289 vs 601 names, 0 overlap) — **which
decays with every resource anyone adds.**

### Scale
10 countries × 2 tiers × 2 build types = 40 variants — workable. The real risk is **duplication
drift**: `elcg/colors.xml` declares 81 names, **8 differ**, 73 are byte-identical copies that
silently revert any future `main` change. At ten brands that's 730 landmines.
*Flavour folders must contain only what genuinely differs* — worth enforcing mechanically.

### A4/A8 constraint: `.gitignore:2` is `*.json`
`git rev-list --all --objects | grep -c google-services.json` → **0**. Neither file has ever been
tracked, on any branch. The cause is the blanket unanchored `*.json` (plus `*.jpg` and `*.txt`), not
a secrets policy. Per A12 this stays — so **per-country location JSON in flavour asset folders will
also be untracked**, and every new brand needs a documented handover checklist covering it.

---

## PART F — Endpoint topology

| Key prefix | Actually points at | Per A2 | |
|---|---|---|---|
| `EZAZI_PROD_*` | `nepalezazi` | Nepal PROD → belongs under ELCG | ✗ swapped |
| `EZAZI_STAGE_*` | `nezazi` | Nepal dev/stage → belongs under ELCG | ✗ swapped |
| `EZAZI_DEV_*` | `testezazi` | default dev/stage | ✓ |
| `ELCG_PROD_*` | `nezazi` | should be `nepalezazi` | ✗ stage box in prod slot |
| `ELCG_STAGE_*` / `ELCG_DEV_*` | `nezazi` | Nepal dev/stage | ✓ |

1. **`ezazi.intelehealth.org` is reachable from zero variants.** It survives only as a commented
   block at `endpoint.properties:11-15` (disabled by `5206d14d`). No runtime escape hatch — the setup
   screen exposes location, username and password, with **no server-URL field**. *This is the concrete
   form of "the eZazi flavour is not shippable": its production endpoint is gone from the build.*
2. **No variant combines eLCG identity with the Nepal production backend.** `nepalezazi` is named
   only by `EZAZI_PROD`, consumed only by the `ezazi` flavour. The shipping "eLCG नेपाल" production
   app **is an `ezazi`-flavour build wearing eLCG branding.**

**Firebase:** three project ids appear; two are real. `ezazi-402e7` (`endpoint.properties:24`) matches
no `google-services.json` anywhere and is near-certainly a typo for `elcg-402e7` — and it is compiled
into a real artifact. Correcting it would make `ezaziStaging` and `elcgProduction` the same app on the
same backend under two applicationIds. **Needs a Firebase console check.**

---

## PART G — Brand axis vs region axis

Currently conflated. The client dimension is a *brand* axis, but several behaviours riding it are
*region* concerns:

| Concern | Sites | Currently gated by |
|---|---|---|
| Bikram Sambat calendar | 16 files / 73 refs | **nothing** — hardcoded in `main` |
| Country literal, district visibility, `":"` join | `PatientAddressInfoFragment.java:221-226`, `:478`, `:550` | partly `FLAVOR_client`, partly hardcoded |
| 5-digit postal rule | `:697`, `:797` | hardcoded |
| `+977` WhatsApp prefix | `PatientDetailActivity.java:561` | hardcoded |
| `"91"`-only phone length branch | `ForgotPasswordFragment.java:82-84` | hardcoded |
| Forced `setAppLanguage("en")` | `PatientAddressInfoFragment.java:221` | hardcoded |

**Prior art already in the codebase:** `Stage3DataTransformer.kt:44-47` decides region by
`serverUrl.contains("nepal")` and emits `isNepalClient`, which `stage3.html` honours — but the native
PDF renderers ignore it and convert to BS unconditionally, so screen and printed sheet can disagree
on the calendar for the same visit.

**Location data:** `state_district_tehsil.json` already has top-level keys `["india","nepal"]`, but
`StateDistMaster.java:14-16` binds `@SerializedName("states")` — matches neither, deserialises to
`null`. All 7 Nepal provinces have **empty district arrays**: a content gap independent of mechanism.

**Localisation seams (A19):**
- `values-ne` does not exist anywhere. When added it belongs in the **flavour's** `values-ne/`, and
  main must not define those names in `values-ne`.
- The **language picker list is shared**: `arrays.xml:21-33` offers 11 Indian languages plus Russian
  to every brand. See the open item below.

---

## PART H — Open items

### 🔔 Language picker — decision pending, owner asked to be reminded
`arrays.xml:21-33` ships a language picker offering 11 Indian languages plus Russian to every brand.
The owner's position is that the app is used in English by health workers and the picker may not be
needed at all; they are confirming internally. Three outcomes live: remove it entirely, make the array
per-flavour, or leave it. Blocks nothing today, **must be settled before Bangladesh ships** — and it
interacts with main's 11 locale buckets, since removing the picker would make most of them dead
weight and shrink the shadowing problem considerably.

### Other
- **applicationId scheme** for brand 3+ (`org.intelehealth.ezazibd`?). Permanent once published.
- **Is eLCG distributed via Play at all?** Every eLCG build since April has shipped `versionCode 1`,
  which Play would reject on a second upload. Suggests sideloading; worth confirming, because it
  determines whether the versionCode pin has been silently causing problems.
- **Is `ezazi-402e7` a typo for `elcg-402e7`?** Requires Firebase console access.
- **Was the compileSdk 36 → 34 walkback (`10282832`) deliberate?**

---

## Appendix — key file map

| Path | Role |
|---|---|
| `app/whitelabel.gradle` | flavour matrix + endpoint injection (one commit, never edited) |
| `app/resdir.gradle` | folds `res/ezazi` into main — the structural blocker |
| `app/environment.gradle` | dead predecessor, body inside `/* */`, declares a colliding dimension |
| `endpoint.properties` | 6 key families, cross-wired |
| `app/src/elcg/` | the only flavour sourceSet; 3 of 38 files live |
| `app/src/main/res/ezazi/` | 362 shared files in a brand-named folder |
| `FlavorKeys.kt` | typed flavour constants; `EZAZI` has no consumer |
| `.gitignore:2` | `*.json` — untracks all Firebase config and any future JSON config |
