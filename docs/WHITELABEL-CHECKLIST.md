# White-Labelling Rebuild — Ordered Checklist

Companion to [`WHITELABEL.md`](WHITELABEL.md). Decisions referenced as **A1–A19** live there.

---

## The two rules that make this safe

**1. Split every task into "changes nothing" and "changes one thing."**
Most steps below can be done as a pure refactor whose output must be *byte-identical*, followed by a
small intentional change you can eyeball. Mixed together, you can't tell which half broke something.

**2. There is no test suite.** The entire suite is two Android Studio templates, and there is no CI.
So byte-identical build output **is** the regression net. Phase 0 builds it. Do not skip it.

---

## Who runs each check

A check with no owner does not happen. Three categories here, and only one of them should become a
permanent tool:

| Check | Who runs it | Lifespan |
|---|---|---|
| `main/res` vs `res/ezazi` collisions (0.3, 3.1) | **Gradle** — a collision is a `MergingException`, so a successful resource merge is the check | **Expires at Phase 3** |
| Byte-identical output after a refactor (1.x, 3.x, 4.x) | **You**, comparing against the Phase 0 snapshot. Worth a throwaway shell script since you will run it 3–4 times | Ends with the rebuild |
| Flavour overrides a name `main` also defines in another qualifier bucket (Phase 7) | **Gradle, on every build, forever** — this is the one that deserves to be written properly | **Permanent** |

The third is the only one worth engineering. It guards an invariant that persists for every future
brand, and it is the check that would have caught the `app_name` locale-shadowing bug years before
anyone noticed. Everything else here is scaffolding — build it cheap, throw it away.

Note the asymmetry: the *temporary* checks are the ones a human runs, and the *permanent* one is the
one a machine runs. If that gets inverted — a human remembering to check the permanent invariant —
it will be skipped under deadline, which is exactly how this codebase arrived here.

---

## Phase 0 — Safety net and unknowns
*Nothing changes. Do not start Phase 1 until all of this is done.*

- [x] **0.1 — Branch. DONE** — working on `ezazi_sprint_47_whitelabel_master`. Do **not** do this work on `dev_kaveri_nepal_sprint_46`. HEAD is
      `bb21866 "Version name upgraded for release"`, i.e. a release is likely pending. Cut a branch
      per phase and merge each one only after its verification passes.

- [x] **0.2 — Snapshot baseline. DONE (Claude, 2026-08-28), with one gap.**
      Existing build output covers **2 of 12 variants** — `ezaziProductionRelease` and
      `ezaziStagingDebug` — and it is **fresh** (newest build output 26 Aug 18:43 vs newest source
      26 Aug 17:25), so those two are a valid baseline as-is. No `elcg*` variant has **ever** been
      built in this workspace.

      To cover the gap, the expected values for **all 12 variants** were computed directly from
      `endpoint.properties` and **validated against the two real generated files — exact match**.
      That table is the Phase 1 baseline; see `WHITELABEL.md` Part F for the resolved hosts.
      Distinct backends across the whole matrix: `testezazi` (ezaziDev only), `nezazi`
      (ezaziStaging **and all three elcg tiers**), `nepalezazi` (ezaziProduction only).

      *Optional:* build one `elcg*` variant once to get a real generated file rather than a
      computed one. Not required — the computed table matched exactly where it could be checked.

- [x] **0.3 — Collision invariant. DONE (Claude, 2026-08-28): 0 collisions**, across 9,355 names in
      `main/res` vs 1,340 in `res/ezazi`. Phase 3 is safe to execute today.
      **Who runs it going forward: Gradle** — a collision between those two srcDirs is a
      `MergingException`, so a successful `merge<Variant>Resources` *is* the check. **This check
      expires when Phase 3 lands** and `res/ezazi` stops existing. Re-run before starting 3.2.

- [x] **0.4 — Qualifier-shadowing audit. DONE (Claude, 2026-08-28).**
      **1,101 of 2,063** resource names in `main` are defined in more than one qualifier bucket.

      | Bucket | Names it can shadow |
      |---|---|
      | 11 locale buckets (`te-rIN`, `mr-rIN`, `ru`, `kn-rIN`, `ml-rIN`, `gu-rIN`, `ta-rIN`, `or`, `as-rIN`, `bn`, `hi-rIN`) | 619–692 **each** |
      | `values-sw600dp` | 373 |
      | `values-w720dp` | 74 |
      | `values-v21` | 63 — *matches 100% of devices (minSdk 26)* |
      | `values-w820dp` | 33 |
      | `values-night` | 14 |

      **The locale buckets are the real problem, not `values-v21`** — correcting the earlier emphasis
      in this file. The app is fully translated into 11 languages, so nearly every user-facing string
      exists in 12 buckets. What matters for Phase 5 is only the names a *flavour wants to override*:

      | Name | Buckets defining it | Consequence |
      |---|---|---|
      | `app_name` | **12** (`values` + all 11 locales) | flavour override in `values/` is dead on 11 locales |
      | `title_activity_login` | **12** | brand-bearing (`eZazi`), same problem |
      | `email_link` | **12** | brand-bearing (`support@intelehealth.io`) |
      | `notification_title` | **4** (`values`, `hi-rIN`, `or`, `ru`) | **live bug today** — elcg overrides only `values/`, so a Hindi device shows the eZazi text |
      | `this_option_available_tablet_device` | 3 (`values`, `v21`, `sw600dp`) | brand-bearing — all three say *"for viewing the eLCG"* |
      | `enter_postal_limit`, `unique_Ezazi_registration_number` | 1 (`values`) | safe to override from a flavour |

      **This validates the Phase 5 approach.** Deleting a brand-varying name from `main` *entirely* —
      every bucket — means there is nothing left to shadow, and each flavour defines it once. The
      12-bucket problem disappears rather than needing 12 overrides per brand.

      Note `values-v21`'s 63 names are almost all clinical strings no brand would rebrand
      (`apgar_1min`, `birth_weight`, `refer_note`…) — with the one exception in the table above. So
      `values-v21` is a maintenance hazard, not a white-label blocker.

- [x] **0.5 — Distribution map. ANSWERED (owner, 2026-08-28).**
        | Build | Endpoints | Delivery |
        |---|---|---|
        | eZazi default (production) | `ezazi.intelehealth.org` | **Play Store** |
        | eZazi dev | `testezazi.intelehealth.org` | — |
        | eZazi stage | (currently `nezazi`) | **APK deployment** |
        | eLCG Nepal | `nezazi` | **APK deployment — not on Play** |

        Confirmed against `origin/development_master:endpoint.properties`, which carries exactly this
        scheme (`PROD_` → `ezazi`, `DEV_` → `testezazi`, both on `ezazi-8712a`) — i.e. **A2 is the
        Play Store build's configuration**, and HEAD has drifted away from it.
        Remaining unknown: **is `ezazi-402e7` a typo for `elcg-402e7`?** Firebase console. Gates 2.2 only.

- [ ] **0.6 — ⚠ LIVE RISK: force-update, and the shared-identity coupling behind it.**
      `SplashActivity.java:84-107` runs on every cold start (`setMinimumFetchIntervalInSeconds(0)`),
      compares `force_update_version_code` to `BuildConfig.VERSION_CODE` at `:97`, and on exceed shows
      a dialog with `hideNegativeButton(true)` whose only action opens `market://details?id=` +
      package name, then `finish()`. Latent today only because the key is unset (no
      `remote_config_defaults.xml`, no `setDefaultsAsync`, so `getLong` returns 0).

      **The eLCG case:** versionCode pinned to **1**, so any value ≥ 2 trips it, and the Play listing
      for `org.intelehealth.elcg` does not exist. Every install bricked, unrecoverable without a
      manual APK push.

      **The worse case — shared identity.** The Play Store eZazi app and the **Nepal production APK**
      are the *same* `applicationId` (`org.intelehealth.ezazi`), the *same* Firebase project
      (`ezazi-8712a`), and the *same* `versionCode` (18) — they differ only in which branch they were
      built from. So setting `force_update_version_code` ≥ 19 to push a Play Store update **also fires
      on every Nepal APK install**, and sends those users to a Play listing that serves the India app.
      One Firebase project is driving two different products that cannot be told apart.

      This is the strongest argument for giving each deployment its own `applicationId` — the item
      currently sitting unscheduled. Decide before Phase 4.

## Phase 1 — Gradle refactor · output must be byte-identical ✅ COMPLETE (2026-08-31)
*Removes the substring dispatch without changing a single resolved value.*

> **Verified on 4 variants, both clients.** `ezaziStaging/debug`, `ezaziDev/debug` and
> `ezaziProduction/release` matched the baseline; `elcgProductionDebug` — **the first elcg variant
> ever generated in this workspace** — matched all 12 predicted fields including
> `APPLICATION_ID=org.intelehealth.elcg`, the `versionCode 1` pin, `ACTIVE_CRASH=true`, and the
> `SOCKET_URL` double semicolon. The `ELCG_PROD_` prefix composes correctly, so the explicit-key
> mechanism is proven for both clients.
>
> One gotcha hit during implementation: the 1.3 assertion must sit **after** the `Properties` load,
> not before. Groovy locals are only in scope from their declaration onward, so referencing
> `property` earlier falls through to a Project property lookup and fails with
> `MissingPropertyException: Could not get unknown property 'property'`.
>
> Also confirmed empirically: `flavour.ext.has(...)` / `.get(...)` works on a `ProductFlavor` read
> back from `variant.productFlavors` — the one thing that could not be verified without running Gradle.

Everything in this phase happens in **one file: `app/whitelabel.gradle`** (96 lines). Nothing else is
touched. `endpoint.properties` is not edited in this phase — that is Phase 2.

### The problem being fixed

Lines 44–66 today derive the endpoint key by **substring-matching the composed variant name**:

```groovy
if (name.contains("ezazi"))      domain = "EZAZI"
else if (name.contains("elcg"))  domain = "ELCG"
else                             domain = "EZAZI"        // ← silent fallthrough
if (name.contains("Dev"))        domain += "_DEV"        // ← runs on the SAME string
else if (name.contains("Staging"))    domain += "_STAGE"
else if (name.contains("Production")) domain += "_PROD"
```

Three defects: `ezaziBangladesh` would match `contains("ezazi")` and steal `ezaziDefault`'s
endpoints; a client flavour whose *name* contains `Dev` (e.g. `ezaziDevTrust`) built as **production**
matches `contains("Dev")` first and silently resolves the dev backend; and an unmapped flavour hits
the `else` and ships pointing at eZazi production.

### 1.1 — Declare the key on each flavour

Add one line to each of the five `productFlavors` blocks (lines 12–42). Leave everything else in
those blocks exactly as-is:

```groovy
ezazi      { dimension client; isDefault = true; /* … */  ext.endpointKey = "EZAZI" }
elcg       { dimension client; applicationId "…";/* … */  ext.endpointKey = "ELCG"  }
dev        { dimension server; /* … */                    ext.endpointKey = "DEV"   }
staging    { dimension server; /* … */                    ext.endpointKey = "STAGE" }
production { dimension server; /* … */                    ext.endpointKey = "PROD"  }
```

These are today's existing key names — **the composed prefix must come out identical to what the
substring code produces now** (`EZAZI_PROD`, `ELCG_DEV`, …). Renaming them to `EZAZI_DEFAULT` /
`ELCG_NEPAL` is Phase 4, not now.

### 1.2 — Replace the dispatch with a lookup

Replace the whole of lines 44–66 with something that reads the key off the flavours instead of the
name. The shape:

```groovy
applicationVariants.configureEach { variant ->
    def keyFor = { String dim ->
        def flavour = variant.productFlavors.find { it.dimension == dim }
        if (flavour == null) throw new GradleException("No flavour for dimension '${dim}'")
        if (!flavour.ext.has("endpointKey"))
            throw new GradleException("Flavour '${flavour.name}' is missing ext.endpointKey")
        return flavour.ext.get("endpointKey")
    }
    buildVariantConfigField("${keyFor('client')}_${keyFor('server')}", variant)
}
```

There is no `else` any more — that is **1.3 done for free**. An unmapped flavour now throws at
configuration time instead of silently inheriting eZazi's servers.

> **The one thing to verify yourself:** the exact `ext` accessor on a ProductFlavor read back from
> `variant.productFlavors`. `ext.has(...)` / `ext.get(...)` is the safe form; Groovy may also let you
> write `flavour.endpointKey` directly. Confirm at configure time before moving on — if the accessor
> is wrong you will get a `MissingPropertyException`, which is loud and harmless.

### 1.3 — Fail on a missing endpoint key

In `buildVariantConfigField` (lines 70–96), after the properties file is loaded, assert every key
exists **before** any `buildConfigField` call:

```groovy
def keys = [serverUrl, realTimeFbUrl, liveKitUrl, socketUrl, fbRtDb].collect { clientServer + it }
def missing = keys.findAll { !property.containsKey(it) }
if (missing) throw new GradleException("endpoint.properties missing: ${missing.join(', ')}")
```

Use `containsKey`, not a null test. Today a missing key yields Groovy `null`, which
`.toString()` turns into the 4-character string `null`, which is emitted **verbatim as a raw Java
expression** → `public static final String SERVER_URL = null;`. That compiles, then NPEs in the field.

**This assertion must not fire today** — all 30 keys are present. If it does fire, your 1.1 key names
compose to the wrong prefix. That makes 1.2 a free self-test of 1.1.

While you are in this function: it re-reads and re-parses `endpoint.properties` on **every** variant
(12 times per configure). Hoisting the load out is optional and safe.

### Do not touch in this phase

- **`ACTIVE_CRASH`** (lines 30, 35, 40) — read by zero code, but removing it changes `BuildConfig.java`
  and breaks the stop condition below. It is a Phase 7 decision.
- **`archivesBaseName`** (lines 17, 25) — already inert; it resolves to the project-global property
  via Groovy's owner chain, so `app/build.gradle:37` wins. Leave it; Phase 4 deals with it.
- **The quoting in `endpoint.properties`** — values carry their own surrounding double quotes, which
  is the only reason `buildConfigField "String", …` emits a valid Java literal. Do not strip them.
- **The trailing semicolons in `endpoint.properties`.** All six `*_SOCKET_URL` values end with a `;`
  *inside the value* (lines 21, 27, 40, 48, 54, 61), so the emitted Java is
  `public static final String SOCKET_URL = "https://…:3004";;` — a stray empty statement. It is legal
  Java and it compiles. **Tidying it changes `BuildConfig.java` and breaks the stop condition below.**
  Confirmed present in the real generated output. Fix it in Phase 2 if at all, never here.

> **Stop condition:** generated `BuildConfig.java` for every variant is **byte-identical** to the
> Phase 0 snapshot. If anything differs, the refactor is wrong — do not proceed.

---

## Phase 2 — Endpoint values ✅ 2.1 / 2.3 COMPLETE (2026-08-31)

> **Verified: all six variants regenerated, 16 field changes, exactly as predicted.**
> `ezaziDev` unchanged (the canary held); `ezaziStaging` → `testezazi` + Firebase corrected;
> `ezaziProduction` → `ezazi.intelehealth.org`; `elcgProduction` → `nepalezazi`; `elcgDev`/`elcgStaging`
> Firebase only. Semicolons intact on all six, `ACTIVE_CRASH` correct per tier, applicationIds correct.
> The Phase 1 assertion stayed silent throughout, independently confirming all 30 keys resolve.
>
> **All six `REAL_TIME_FB_URL` now point at `ezazi-8712a` (`firebaseio.com`, not the regional domain).**
> This is correct and matches production reality: the console audit showed `ezazi-8712a` already holds
> all five deployment trees, and `FB_RT_INSTANCE` namespaces each host's reads and writes into its own
> subtree, so co-tenancy cannot cross-contaminate. `elcg-402e7` is empty and nothing writes to it, so
> pointing elcg there was a guaranteed failure; `ezazi-8712a` is the only option that can work.
>
> **Consequence to carry into 2.2:** this pre-selects Option A for *all* elcg variants. They now
> initialise Firebase from `elcg-402e7`'s `google-services.json` while calling `getInstance()` on an
> `ezazi-8712a` URL — cross-project access, still untested, but now testable by building
> `elcgStagingDebug` and placing a call.
>
> **Still open:** 2.2 (deferred to the PM / web-dev conversation) and 2.4 (scheduled, not executed).


> ### ⚠ Correct an earlier characterisation
> This file previously called Phase 2 "small, isolated, trivially verifiable." The **file edit** is.
> Its **consequence** is not, and the difference matters before you start.
>
> Today the Nepal production app is built as **`ezaziProduction`** → `org.intelehealth.ezazi` +
> Firebase `ezazi-8712a`. `nepalezazi` sits under the `EZAZI_PROD_` key family.
> After the remap, `nepalezazi` moves to `ELCG_PROD_`, so the Nepal production build becomes
> **`elcgProduction`** → `org.intelehealth.elcg` + `elcg-402e7`.
>
> **That is an app identity migration, not a config change.** Different `applicationId` means
> existing field installs **cannot update** — uninstall and reinstall, losing unsynced clinical
> records on devices. Different Firebase project means a new FCM sender id, so every push token
> invalidates, and the existing call-signalling tree is orphaned.
>
> **Scope Phase 2 to the config only. Ship nothing from it.** Which flavour ships to Nepal is a
> separate, scheduled migration — see 2.4.

Everything here happens in **one file: `endpoint.properties`**. `whitelabel.gradle` is not touched;
Phase 1 already made it read whatever these keys say.

### Target mapping (A2 + the 0.5 distribution map)

| Key family | Host | Firebase | Change |
|---|---|---|---|
| `EZAZI_PROD_*` | `ezazi.intelehealth.org` | `ezazi-8712a` | **uncomment lines 11–15**, delete the nepalezazi block at 17–21 |
| `EZAZI_STAGE_*` | `testezazi.intelehealth.org` | `ezazi-8712a` | **changed** — A14 duplication of DEV |
| `EZAZI_DEV_*` | `testezazi.intelehealth.org` | `ezazi-8712a` | unchanged |
| `ELCG_PROD_*` | `nepalezazi.intelehealth.org` | ← **decision, see 2.2** | **changed** — inherits the values currently at lines 17–21 |
| `ELCG_STAGE_*` | `nezazi.intelehealth.org` | `elcg-402e7` | unchanged |
| `ELCG_DEV_*` | `nezazi.intelehealth.org` | `elcg-402e7` | unchanged |

> ### ✅ 2.1 and 2.3 are cleared to proceed without 2.2 (2026-08-31)
> The five fields split along the blocker. **`SERVER_URL`, `LIVE_KIT_URL`, `SOCKET_URL` and
> `FB_RT_INSTANCE` all derive from the host** — move them together in 2.1. **`REAL_TIME_FB_URL` is
> the Firebase-project choice** — leave every one of them untouched, pending 2.2.
>
> **2.3 fixes the `ezazi-402e7` typo for free.** A14 makes `EZAZI_STAGE_*` a copy of `EZAZI_DEV_*`,
> which uses `ezazi-8712a` — so the nonexistent-project line is simply overwritten by a correct one.
> Confirmed by console: `testezazi_intelehealth_org` is a live tree in `ezazi-8712a`.
>
> `elcgProduction` will end up knowingly inconsistent (host `nepalezazi`, database `elcg-402e7`,
> which is empty). Acceptable **only because 2.4 says nothing ships from elcg** until the migration
> is planned. It resolves whichever way 2.2 lands.
>
> **Console findings that make this safe:** `DeviceInfo` carries no timestamp field and
> `saveDeviceInfo` uses `setValue()` at a fixed key, so `device_info` records cannot be dated — the
> absence of timestamps is a property of the schema, not a gap in the audit. The `https:` tree
> mirrors every deployment including `devezazi_intelehealth_org` (a host commented out since before
> the white-label work) and a node named `ezazi-8712a-default-rtdb_firebaseio_com`, meaning
> `getServerUrl()` has at times returned the Firebase URL. Both are separate tracks.
>
> *Also noted for someone's attention, outside this work: `DeviceInfo` stores `userName` and
> `userUUID`, so health-worker identities live in that database.*

- [x] **2.1 — Remap the host families. DONE (committed `c609ce9`).** Uncomment 11–15, retire 17–21 into `ELCG_PROD_*`, and point
      `EZAZI_STAGE_*` at `testezazi`. Note lines 29–33 are a **second** commented `EZAZI_STAGE` block
      pairing `nepalezazi` with `elcg-402e7` — evidence for the 2.2 decision; delete it once resolved.

- [~] **2.2 — Firebase topology. RESOLVED BY CONSOLE AUDIT (owner, 2026-08-31); one decision left.**

      **Findings — only two projects exist:**
      | Project | applicationId | RTDB URL | Contents |
      |---|---|---|---|
      | `ezazi-8712a` | `org.intelehealth.ezazi` | `…firebaseio.com/` | **all five trees** |
      | `elcg-402e7` | `org.intelehealth.elcg` | `…asia-southeast1.firebasedatabase.app/` | **completely empty** |

      `ezazi-8712a` root nodes: `device_info`, `ezazi_intelehealth_org`, `https:`,
      `nepalezazi_intelehealth_org`, `nezazi_intelehealth_org`, `testezazi_intelehealth_org`.

      **Three things this settles:**
      1. **`ezazi-402e7` does not exist** — line 24 is confirmed a typo. `ezaziStaging` has been
         pointing at a nonexistent project.
      2. **The elcg flavour's Firebase wiring has never worked.** All `ELCG_*` variants point at
         `elcg-402e7`, which is empty; the `nezazi` backend writes to `ezazi-8712a` instead
         (`nezazi_intelehealth_org` is present there). Never noticed because elcg has never shipped.
      3. **Therefore Phase 2 cannot regress elcg** — there is no working path to break. Risk on
         2.1/2.2 is much lower than first assessed.

      **The remaining decision:**
      - **Option A — config follows reality.** Point `ELCG_*_REAL_TIME_FB_URL` at `ezazi-8712a`.
        No server work; elcg calls work immediately. But both brands then share one project (awkward
        against A3), and elcg builds would initialise FirebaseApp from `elcg-402e7`'s
        `google-services.json` while calling `getInstance()` on an `ezazi-8712a` URL — cross-project
        access depends on that database's security rules. **Test before committing.**
      - **Option B — reality follows config.** Keep `elcg-402e7`; have whoever runs `nezazi` write
        call notifications there. Cleaner, matches A3, but nothing works until the backend lands.

      Still worth checking: whether `nezazi_intelehealth_org` under `ezazi-8712a` has **recent**
      timestamps. If it does, Option B is a live migration rather than a fresh start.

      **Bonus finding — a production bug confirmed.** The `https:` root node is
      `FirebaseRealTimeDBUtils.java:26` building its path from `SessionManager.getServerUrl()`
      (scheme-bearing) instead of `FB_RT_INSTANCE` like the other two call sites. Firebase treats `/`
      as a path separator, so diagnostic logs have been accumulating under a node literally named
      `https:`, disjoint from every other tree. Separate from white-labelling; fix on its own track.

- [x] **2.3 — Apply A14 duplication. DONE (committed `c609ce9`).** `EZAZI_STAGE_*` becomes a copy of `EZAZI_DEV_*`; `ELCG_DEV_*`
      is already a copy of `ELCG_STAGE_*`. Every key stays declared — the Phase 1 assertion fires on
      absence, and A14 says duplicate rather than omit.

- [ ] **2.4 — Schedule the Nepal identity migration (do NOT execute in this phase).** Moving Nepal
      production from `ezaziProduction` to `elcgProduction` needs: a device-side reinstall plan that
      preserves unsynced records, FCM re-registration, an RTDB data decision, and a distribution
      channel for the new `org.intelehealth.elcg` APK. Until that lands, **`elcgProduction` is
      configured but unshipped.**

### Do not touch in this phase
- **The trailing semicolons.** Every `*_SOCKET_URL` value ends with `;` inside the value, producing
  `= "…:3004";;` in the generated Java. Legal, compiles, and present in all four verified baselines.
  Fixing it is a real change with its own verification — not a drive-by.
- **`whitelabel.gradle`** — Phase 1 is verified and complete. Leave it alone.
- **The `#DEV_*` block at lines 1–6** — dead `environment.gradle`-era keys using the retired
  `FB_RT_DB` name. Harmless; retire them in Phase 7.

> ### ⚠ Footgun this phase introduces
> After 2.1, **`ezaziProduction` points at `ezazi.intelehealth.org`, not `nepalezazi`.** Anyone
> building `ezaziProduction` for Nepal out of habit ships against the wrong backend — and it will
> look fine, because the app has no visible indication of which server it is talking to. Tell whoever
> cuts Nepal builds before this merges.

> **Stop condition:** regenerate `BuildConfig.java` for all six flavour combinations and diff against
> the Phase 0 baseline table. **Only host values may differ.** No field added or removed, nothing
> resolving to `null`, `ACTIVE_CRASH` unchanged, `;;` still present. Specifically expect
> `ezaziProduction` → `ezazi.intelehealth.org` and `elcgProduction` → `nepalezazi.intelehealth.org`.

---

## Phase 3 — Dissolve `resdir.gradle` (A16) ✅ COMPLETE (2026-08-31, committed `fae89d8`)
*362 files relocated; resource set unchanged at 10,774 entries; R.txt byte-identical at 12,122 symbols.*

Two files change: **`app/resdir.gradle`** (deleted) and **`app/build.gradle`** (one `apply from:` line
removed). Plus a pure relocation of 362 resource files. **No file contents are edited.**

### Why it has to go

```groovy
sourceSets { main { res { srcDirs 'src\main\res', 'src\main\res\ezazi' } } }
```

Both paths are `<source>` entries of the **same** ResourceSet. They are one source set, so they
**cannot override each other** — a shared resource name between them is a `MergingException`, a hard
build failure, not a win. So `res/ezazi` is a brand-named folder that (a) ships in every brand and
(b) is the one place a per-brand override provably cannot work. It is also the folder a developer
will reach for first when adding brand resources.

Secondary: the backslashes are Windows path literals. `srcDirs(Object...)` is *additive*, so on
Linux/macOS the AGP default `src/main/res` survives by convention while both literal strings match
nothing — silently dropping all 362 files. Latent only because no CI exists.

### The move, exactly (measured 2026-08-31)

362 files across 13 buckets. All tracked, none gitignored, **zero resource-name collisions**
(9,355 names in `main/res` vs 1,340 in `res/ezazi`).

| Bucket | Files | Action |
|---|---|---|
| `color-v22`, `drawable-sw600dp`, `layout-sw600dp`, `navigation` | 5, 11, 7, 2 | **new dirs** — move wholesale |
| `anim`, `color`, `drawable`, `drawable-v21`, `font`, `layout`, `menu` | 2, 16, 152, 5, 3, 142, 6 | merge into existing — **no filename clash** |
| `values`, `values-sw600dp` | 9, 2 | merge — **9 filename collisions, rename on move** |

### 3.1 — Re-run the collision check
`main/res` and `res/ezazi` must still share zero resource names. **Gradle is the check**: a successful
`merge<Variant>Resources` proves it. Confirmed 0 on 2026-08-31; re-confirm immediately before 3.2,
since it decays with every resource anyone adds.

### 3.2 — Move the 11 non-`values` buckets
Straight relocation, `git mv` so history follows. Filenames are unique, so no renames needed.
Four of them create new directories in `main/res`.

### 3.3 — Move `values` and `values-sw600dp` with renames
**Do not merge file contents.** Android allows many XML files per `values/` folder; only *resource
names* must be unique, not filenames. So rename on move and leave contents untouched:

```
values/arrays.xml   → values/arrays_ezazi.xml       values/strings.xml → values/strings_ezazi.xml
values/attrs.xml    → values/attrs_ezazi.xml        values/styles.xml  → values/styles_ezazi.xml
values/bools.xml    → values/bools_ezazi.xml        values/themes.xml  → values/themes_ezazi.xml
values/dimens.xml   → values/dimens_ezazi.xml
values-sw600dp/bools.xml  → values-sw600dp/bools_ezazi.xml
values-sw600dp/dimens.xml → values-sw600dp/dimens_ezazi.xml
```

`content_descriptions.xml` and `dialog_theme.xml` have no counterpart in `main/values` — move as-is.

### 3.4 — Remove the wiring
Delete `app/resdir.gradle`, and its `apply from: "resdir.gradle"` line in `app/build.gradle`. The AGP
default already supplies `src/main/res`, so the first srcDir was redundant and the second is now gone.

### Traps
- **Use `git mv`.** A plain move records 362 deletions + 362 additions, and `git log --follow` on any
  layout stops working for good.
- **Change nothing else in the same commit.** No reformatting, no import tidy, no "while I'm here"
  edits. A one-line content change is invisible inside a 362-file rename diff, and the stop condition
  cannot catch it.
- **Verify after the move, before deleting `resdir.gradle`.** Once `res/ezazi` is gone, that srcDir
  points at a missing path — AGP skips missing srcDirs silently, so the build still passes. That gives
  you a clean two-step: move → verify → unwire.
- **`.idea/workspace.xml` holds stale `res/ezazi/...` paths.** Gitignored IDE state; the IDE rewrites
  it. Ignore.
- **Nothing else references the folder** — `resdir.gradle` is the only non-IDE reference in the repo.

> **Stop condition — two independent checks.**
> 1. **Source-level, no build needed:** the set of `(bucket, type, name)` resource entries across
>    `main/res` + `res/ezazi` before must exactly equal the set across `main/res` after. Same count,
>    same names. This catches a lost file or a botched rename directly. *(Ask Claude to produce the
>    before/after sets — it is the same scan used for 0.3.)*
> 2. **Build-level:** run a resource merge, then diff the generated resource symbol list (the `R.txt`
>    / symbol-list artefact under `app/build/intermediates/` — run `./gradlew :app:tasks --all` to get
>    the exact task name for your AGP version). **Byte-identical** before and after. This is a
>    ~362-file diff that must produce **zero** change in build output.

---

## Phase 4 — Flavour identity (A6, A13, A15, A17, A18) — 4a ✅ · 4b ✅ · 4c pending

The first phase where a mistake is **not** caught by a byte-identical check, and the first where a
choice becomes permanent. Split into three commits, each verified before the next.

| Commit | Items | Verification |
|---|---|---|
| **4a** Renames | 4.1 – 4.3 | endpoint values unchanged; only `FLAVOR*` fields move |
| **4b** Identity | 4.4 – 4.5 | versionCode per flavour as declared |
| **4c** API migration | 4.6 – 4.7 | **values equivalent, NOT byte-identical** — see 4.6 |

### Target names

| Old | New | Endpoint key | applicationId (unchanged) |
|---|---|---|---|
| `ezazi` | **`ezaziDefault`** | `EZAZI_DEFAULT` | `org.intelehealth.ezazi` |
| `elcg` | **`elcgNepal`** | `ELCG_NEPAL` | `org.intelehealth.elcg` |

Variants become `ezaziDefaultProductionRelease`, `elcgNepalStagingDebug`, and so on.
**Renaming a flavour does not change `applicationId`** — the Play Store build keeps its identity.

---

### 4a — Renames ✅ COMPLETE (2026-09-01)

> **Verified: all six variants regenerated, every field matches the Phase 2 baseline.**
>
> | Variant | Host | Firebase | crash | applicationId |
> |---|---|---|---|---|
> | `ezaziDefaultDev` | testezazi | ezazi-8712a | false | `org.intelehealth.ezazi` |
> | `ezaziDefaultStaging` | testezazi | ezazi-8712a | true | `org.intelehealth.ezazi` |
> | `ezaziDefaultProduction` | ezazi.intelehealth.org | ezazi-8712a | true | `org.intelehealth.ezazi` |
> | `elcgNepalDev` | nezazi | ezazi-8712a | false | `org.intelehealth.elcg` |
> | `elcgNepalStaging` | nezazi | ezazi-8712a | true | `org.intelehealth.elcg` |
> | `elcgNepalProduction` | nepalezazi | ezazi-8712a | true | `org.intelehealth.elcg` |
>
> `LIVE_KIT_URL` and `SOCKET_URL` consistent with each host, `;;` intact on all six, `ACTIVE_CRASH`
> correct per tier, `applicationId` unchanged for both brands. The Phase 1 assertion stayed silent —
> which is what proves all 30 renamed keys match the new `ext.endpointKey` values.
>
> **The trap, closed and proven rather than assumed:**
> `FlavorKeys.EZAZI_DEFAULT = "ezaziDefault"` vs `BuildConfig.FLAVOR_client = "ezaziDefault"` — match.
> `FlavorKeys.ELCG_NEPAL = "elcgNepal"` vs `FLAVOR_client = "elcgNepal"` — match.
>
> **Deprecated constants deleted** in the same phase. Zero references across `app`, `klivekit` and
> `abdm` including test source sets, verified immediately before removal; full
> `compileEzaziDefaultStagingDebugJavaWithJavac` passed afterwards.

**Two issues hit during implementation, both caught by the guardrails:**

1. **`EZAZI_DEFAULT_FB_RT_INSTANCE`** — the find-and-replace dropped the `DEV` segment on one line of
   30 (`EZAZI_DEV_FB_RT_INSTANCE` became `EZAZI_DEFAULT_FB_RT_INSTANCE` instead of
   `EZAZI_DEFAULT_DEV_FB_RT_INSTANCE`). **The Phase 1 assertion caught it at configuration time**
   rather than letting it reach a runtime `null`. Exactly what 1.3 was written for.
2. **`PatientAddressInfoFragment.java:478`** was briefly left on the deprecated `FlavorKeys.ELCG`
   after the flavour rename — the silent-failure case. Caught by review before it reached a build.

**Semantic change made deliberately:** both fragment sites were restructured from
`if (ELCG) → village-only, else → colon-joined` to
`if (EZAZI_DEFAULT) → colon-joined, else → village-only`. Identical for the two current brands, but
**the fall-through default flipped** — an unrecognised third brand now gets the village-only (Nepal)
form rather than the colon-joined (eZazi/India) form. Worth a comment on the else branch, since
nothing in the code states this. Note `FlavorKeys.ELCG_NEPAL` is consequently declared but
unreferenced.

**What is in the commit:** 37 renames (`app/src/elcg` → `app/src/elcgNepal`, `google-services.json`
moved by hand since it is gitignored), plus `app/whitelabel.gradle`, `endpoint.properties` (30 keys),
`FlavorKeys.kt`, and `PatientAddressInfoFragment.java`. The fragment's diff is larger than expected
(38 lines) because the IDE also reindented the district-validation block at `:668` from 8 to 12
spaces — that block was **already** inside the `if (!mCountryName ... "Nepal")` check at HEAD but
misleadingly indented, so this is an indentation-only fix with no behavioural change.

`app/src/ezaziDefault/` is empty and therefore **not in the commit** — git does not track empty
directories. It will need creating again on any other clone until Phase 5 puts content in it.

---

### 4b — Identity ✅ COMPLETE (2026-09-01) — 4.4 done, 4.5 dropped

Two files: `app/whitelabel.gradle` (versions) and `app/build.gradle` (signing). No source changes.

**Current state, verified 2026-09-01:**

| | versionCode | versionName | Source |
|---|---|---|---|
| `defaultConfig` | 18 | `3.1.1` | `app/build.gradle:30-31` |
| `ezaziDefault` | *(inherits 18)* | *(inherits 3.1.1)* | declares neither |
| `elcgNepal` | **1** | **`1.0`** | `app/whitelabel.gradle:23-24` |

`signingConfigs`: **none anywhere in the repo.** `buildTypes.release` (`app/build.gradle:48-58`) has
no `signingConfig`, so `assembleRelease` currently produces an *unsigned* APK — Android Studio's
"Generate Signed APK" dialog signs it as a separate step, with the keystore path recorded only in
`.idea/workspace.xml:326` (gitignored, one machine).

- [x] **4.4 — Version separation. ✅ DONE (2026-09-01).**

  **The problem, proven from git.** Nepal releases were cut from the `ezazi` flavour, so bumping a
  Nepal delivery bumped *eZazi's* version:

  | Commit | Date | Author | Effect |
  |---|---|---|---|
  | `997eb86f` | 2026-02-21 | zKaveri | `versionCode` → **18** — the Play Store release, `versionName '3.0'` |
  | `bb21866` | 2026-08-26 | arpansircardevelopment | `versionName` → **3.1.1** — a **Nepal** release, applied to eZazi's line |

  Worse, both shipped as `org.intelehealth.ezazi` **at the same versionCode 18** — the Play build and
  the Google-Drive APK are the same applicationId with different backends. That is 0.6's
  shared-identity coupling, live in the field.

  **What was done:**

  | Where | Value | Rationale |
  |---|---|---|
  | `app/build.gradle` `defaultConfig` | `18` / **`3.0.0`** | eZazi's version. Reverts `bb21866`'s mis-attributed bump. `ezaziDefault` is `isDefault` and declares nothing of its own, so this is its single source of truth. |
  | `app/whitelabel.gradle` `elcgNepal` | **`18`** / **`3.1.1`** | Matches the last Nepal delivery. |

  `3.0.0` rather than `3.0`: the org has always used 3-digit versioning and the 2-digit form was the
  anomaly. No Play mismatch results, because the next store release will be `3.0.1` or `3.1.0` — the
  string `3.0.0` never ships.

  **Why `ezaziDefault` gets no declaration of its own:** a ProductFlavor overrides `defaultConfig`, so
  declaring a version on `ezaziDefault` would silently swallow any future bump made in
  `app/build.gradle` — exactly the failure `bb21866` would have caused. Both files now carry comments
  recording this, so the asymmetry does not get "tidied up" later.

  **Why `elcgNepal` starts at 18, not 1:** `org.intelehealth.elcg` has never shipped. 18/3.1.1
  continues the Nepal **product's** version across the applicationId change — the APK in the field is
  `org.intelehealth.ezazi` at 18/3.1.1 — rather than continuing an install base. It stays at 18 until a Nepal delivery bumps it.
  **Bonus:** this lifts the 0.6 force-update floor from 2 to 19, matching the protection eZazi has.

  **Going forward:** eZazi Play release → bump `defaultConfig`. Nepal delivery → bump `elcgNepal`.
  The two numbers drift apart independently, which is the point of A17.

  *Still worth adding when convenient:* an assertion that any client flavour without `isDefault` must
  declare its own `versionCode`/`versionName`, so brand 3 cannot silently inherit eZazi's.

  **Verified:** all six variants regenerate with `ezaziDefault*` = 18/3.0.0 and `elcgNepal*` =
  18/3.1.1, endpoints and applicationIds unchanged from 4a.

- [~] **4.5 — ~~Add a `signingConfig`~~ DROPPED (owner, 2026-09-01). Replaced by: document the release process.**

  > **Why it was dropped.** The original sweep flagged "zero `signingConfigs` in the repo" as a gap.
  > It is an absence, not a defect — the org signs releases through Android Studio's *Generate Signed
  > Bundle / APK* dialog with the `.jks` files it already holds, and sends the output for testing.
  > APKs are signed, Play accepts `ezaziDefault`, sideloaded `elcgNepal` updates install. **Nothing is
  > broken.**
  >
  > A Gradle `signingConfig` would buy command-line signed releases, CI capability, and independence
  > from one machine's IDE state. **There is no CI in this repo**, and releases are cut by one person
  > from the IDE — so it costs secrets management and a new failure mode (machines without the
  > keystore) in exchange for nothing that is currently needed.
  >
  > **It is also not required by the white-label rebuild.** Phases 1–4a touched no signing; 4c does
  > not either. Brand 3's APK gets signed by the same dialog with the same key, exactly like brands 1
  > and 2. A18's "one key for all brands" is already what the current process does.

  **What remains — the actual residual risk, and it is documentation, not configuration:**
  the release process exists only as `"ExportApk.BuildVariants": ["ezaziProductionRelease"]` in
  `.idea/workspace.xml:326` — gitignored IDE state, on one machine. If that person is unavailable,
  the next one has to guess which keystore, which alias, and which variant.

  **4a has already invalidated that stored value** — `ezaziProductionRelease` no longer exists; the
  variant is now `ezaziDefaultProductionRelease`. The IDE will fail to match it silently.

  So: write a short runbook (which `.jks`, which alias, which variant per brand, where the keystore
  lives) and keep it wherever the team will actually find it. If a Gradle `signingConfig` is ever
  wanted later — say when CI arrives — note that **`keystore.properties` and `signing.properties` are
  NOT gitignored** (`.gitignore` carries only `/local.properties`, anchored to root), so the
  gitignore entry must be added *before* the file is created. `local.properties` is already ignored
  and is the lower-risk home for those four values.

> **Stop condition 4b:** each variant reports its own `versionCode` / `versionName` in the generated
> `BuildConfig.java`, and the five endpoint values are untouched from 4a. That is the whole check —
> 4.5 is now documentation, with nothing to verify in the build.

---

### 4c — API migration and artifact naming

> **All API claims below were verified against the real AGP 8.2.2 jars and sources on this machine**
> (2026-09-01), not from recollection. Note for reproduction: **`GRADLE_USER_HOME` is `D:\Gradle`**,
> not `C:\Users\arpan\.gradle` — the default cache holds only AGP 7.x and will mislead you.
> The API lives in `gradle-api-8.2.2.jar` (99 classes), and a **sources jar is available** alongside
> it, which carries the KDoc that settles the quoting question. `javap` is at
> `C:/Users/arpan/.jdks/jbr-17.0.12/bin/javap.exe` and is **not on PATH**.

#### ⚠ Correction to what this file previously said

An earlier draft of 4.6 claimed the doubled semicolon (`SOCKET_URL = "…:3004";;`) **might disappear**
under the new API, and told you to compare values semantically rather than byte-for-byte. **That was
wrong.** Both APIs funnel into the same `Map<String, BuildConfigField<out Serializable>>`, and
`BuildConfigField`'s own KDoc states: *"If [type] is [String], then [value] should include quotes."*
The generator then appends `;` unconditionally — verified down to the `JavaWriter` bytecode, which
writes `" = "`, the value, then a literal `";\n"`.

**The migration will not change the generated output at all.** The `;` inside every `*_SOCKET_URL`
value in `endpoint.properties` is the defect. **Fix the data, not the API.**

> **A latent trap that makes this worth fixing rather than tolerating.** The two emit paths disagree.
> The default *source* path emits the value verbatim, giving the `;;` you see today. The *bytecode*
> path (`android.enableBuildConfigAsBytecode=true`, **not currently set**) calls
> `value.toString().removeSurrounding("\"")` — which silently does **not** strip when the last
> character is `;` rather than `"`. That constant would then keep its literal quotes *and* the
> semicolon inside the string value. Same input, two different wrong answers, depending on a flag
> nobody has set yet. Stripping the `;` defuses both.

- [x] **4.8 — Strip the trailing `;` from the six `*_SOCKET_URL` values. ✅ DONE (2026-09-01).**
  Independent of 4.6 and 4.7; do it on its own. Stop condition: the six regenerated `BuildConfig.java`
  files show `SOCKET_URL = "https://host:3004";` with **one** semicolon, and no other field moves.
  Confirmed present today at
  `app/build/generated/source/buildConfig/elcgNepalDev/debug/…/BuildConfig.java`.

#### The tension between 4.6 and 4.7 — read before starting either

**Verified:** `com.android.build.api.variant.VariantOutput` in AGP 8.2.2 has exactly four members —
`versionCode`, `versionName`, `enabled`, `enable` (deprecated). **There is no `outputFileName`**; the
string does not appear anywhere in `gradle-api-8.2.2-sources.jar`. Per-variant output naming in the
new API requires the **Artifacts API**: `variant.artifacts.get(SingleArtifact.APK)` plus
`artifacts.getBuiltArtifactsLoader().load(dir)` wired into your own copy/rename task, or
`artifacts.use(task).wiredWithDirectories(…).toTransformMany(SingleArtifact.APK)`.

Meanwhile the **legacy** `applicationVariants.all { outputs.all { outputFileName = … } }` still works
in 8.2.2 and emits **no deprecation warning on the setter** — `BaseVariantOutputImpl.setOutputFileName`
only rejects absolute paths. AGP pins the legacy variant API at `VERSION_9_0`, so it dies in AGP 9.

**So 4.6 and 4.7 pull in opposite directions:** 4.6 migrates *off* the legacy API, while 4.7's simple
implementation *needs* it. Doing both now means running the old and new APIs side by side, which is
worse than either alone.

**Recommendation: do 4.7 on the legacy API now, defer 4.6.**
- 4.7 delivers value immediately — QA can identify a build from its filename.
- 4.6 delivers **nothing today**. It is future-proofing against a compiler that has not shipped.
- When AGP 9 forces the issue, the BuildConfig injection *and* the naming migrate together to the
  Artifacts API, in one coherent change, rather than being half-migrated for a year.

#### 4.7 — Per-brand artifact naming ✅ DONE (2026-09-01, legacy API)

> **Verified from real APKs:**
> ```
> elcgNepal-3.1.1-202609011827-production-debug.apk
> ezaziDefault-3.0.0-202609011827-production-debug.apk
> ```
> Brand first, correct per-brand version (3.1.1 vs 3.0.0, both versionCode 18), timestamp, tier,
> build type. Confirmed against `output-metadata.json`, not just the file listing.
>
> **The timestamp behaves as intended:** it is computed once per Gradle invocation, so variants built
> together share a stamp while separate sessions differ — an earlier `ezaziDefaultDev` build carries
> `202609011819` against this run's `202609011827`. That is what lets a partner tell one delivery
> from another.
>
> **Implementation:** `variant.outputs.all { outputFileName = ... }` inside the existing
> `applicationVariants.configureEach` block, using `variant.versionName` (already the merged
> per-flavour value from 4.4). The dimension lookup was refactored into a shared `flavourFor(dim)`
> so `keyFor` and the naming reuse one guard. **Both dead `archivesBaseName` lines were deleted**
> from the flavour blocks; `app/build.gradle:40` is left for 4.6, where `base { archivesName }`
> replaces it.
>
> **Note for release builds:** AGP appends `-unsigned.apk` rather than `.apk` when a release is not
> signing-ready, but `outputFileName` replaces the whole name — so a release APK reads
> `...-release.apk` either way. The filename does not indicate whether it is signed.



Target, per the format decision above:
`elcgNepal-3.1.1-202609011430-production-debug.apk`

**Verified naming rule:** AGP composes `"$archivesBaseName-$baseName$suffix"` where `baseName` is the
product flavours joined by `-`, then `-` + buildType (`VariantPathHelper.getOutputFileName`). That is
why today's real artifact reads `eZAZI3.1.1-202608261843-ezazi-production-release.apk`.

**`archivesBaseName` cannot express the target**, for three separately verified reasons:
1. It is **project-global** — read via `project.extensions.getByType(BasePluginExtension).archivesName`
   (`ProjectInfo.kt:42`). `javap` shows **no `archive*` member** on `ProductFlavor`, `DefaultConfig` or
   `BaseFlavor`, so the assignments in `whitelabel.gradle` escape up the Groovy owner chain to the Project.
2. Therefore **both existing assignments are dead code**: `elcgNepal` (line 29) overwrites
   `ezaziDefault` (line 16), and then `app/build.gradle:40` overwrites both, because
   `whitelabel.gradle` is applied at line 15 — *before* it.
3. AGP appends the flavours **on top** of whatever you set, so `elcgNepal-3.1.1` would yield
   `elcgNepal-3.1.1-elcgNepal-production-release.apk` — brand twice.

`outputFileName` replaces the **entire** name, so it sidesteps all three. Everything needed is on the
legacy variant, and `variant.versionName` is **already the merged per-flavour value** — elcgNepal
names itself `3.1.1` and ezaziDefault `3.0.0`, courtesy of 4.4.

Delete both dead `archivesBaseName` lines from `whitelabel.gradle` while you are there. Leave
`app/build.gradle:40` alone until 4.6, since it is what names anything `outputFileName` does not.

> **Stop condition 4.7:** verify by **artifact filename**, not `BuildConfig.java`. Every APK reads
> `<brand>-<versionName>-<timestamp>-<server>-<buildType>.apk`, with `elcgNepal` builds carrying
> `3.1.1` and `ezaziDefault` builds carrying `3.0.0`.

#### 4.6 — Migrate to `androidComponents.onVariants` (deferred; notes for when AGP 9 forces it)

- **`ext.endpointKey` is unreachable from the new API.** Verified: `ComponentIdentity.productFlavors`
  returns `List<kotlin.Pair<String,String>>` of *(dimension, flavourName)* — string pairs, not DSL
  objects — and there is no `ExtensionAware` or extra-properties accessor anywhere in the
  `api.variant` package. Carry the values in a plain Groovy map from flavour name to endpoint key,
  built in the build script. (The sanctioned alternative — `DslExtension` +
  `VariantExtensionConfig.productFlavorsExtensions` — is `@Incubating` in 8.2.2 and much heavier.)
- **Expect no change to generated `BuildConfig.java`.** Both APIs share the same field map and the
  same quoting convention, so byte-identical **is** the correct stop condition here after all —
  provided 4.8 has already landed. The earlier "compare semantically" advice was based on a wrong
  assumption and is withdrawn.
- **`archivesBaseName` is separately deprecated** in Gradle 8.11.1 (`DeprecationLogger…
  .willBeRemovedInGradle9()`), replaced by `base { archivesName }` (a `Property<String>`). Fold that
  in when you migrate.

### Also in this phase

- **`.idea/workspace.xml` will hold a stale variant name** — `ezaziProductionRelease` stops existing.
  Gitignored IDE state, so it fails quietly; whoever cuts releases must reselect the variant. Worth
  telling them, because that file *is* the release process today.
- **`applicationId` scheme for brand 3+** remains undecided and is the one **permanent** choice in
  this area. It does not block 4a–4c — both existing brands keep their current ids — but settle it
  before a third flavour is added, because it cannot be changed once that brand ships.

---

### Side-by-side install: can both brands coexist on one device?

**Yes.** Different `applicationId`s (`org.intelehealth.ezazi` / `org.intelehealth.elcg`) make them
separate apps to Android. True today; Phase 4 does not change it (renaming a flavour does not touch
its applicationId).

**Isolated automatically per applicationId:** `localrecords.db` (app-private internal storage, so no
patient-data crossover), SharedPreferences, the `${applicationId}.provider` FileProvider authority,
notification channels, WorkManager tags, and Firebase (separate projects and FCM sender ids).

**Shared, and NOT fixed by Phase 4:**

| Surface | Location | Impact |
|---|---|---|
| External DB migration file | `AppConstants.java:29` → `<external>/InteleHealth_DB/Intelehealth.db` | same file for both apps; only fires during migration. `SmoothUpgrade.java:432` also has a missing-separator bug producing `InteleHealth_DBIntelehealth.db` |
| PDF exports | `WebViewPdfExporter.kt:396` → `Downloads/eZazi` | both write here; files intermingle. Fixed in Phase 5 |
| Account type | `authenticator.xml:4` = `io.intelehealth.openmrs` | identical in both; latent, all AccountManager call sites commented out |
| Broadcast actions | `org.intelehealth.app.RTC_*`; exported receiver at `manifest:141`, no permission | most senders use `setPackage(getPackageName())`, which contains it |

**⚠ Practical blocker for side-by-side TESTING, until Phase 5:** both apps ship the **same
`app_name`** (`eLCG नेपाल`, hardcoded in `main`) and the **same launcher icon**
(`@mipmap/elcg_square_full_bg_icon`, hardcoded at `AndroidManifest.xml:41/:46`). Two identical icons
with identical labels — indistinguishable in the launcher. Tell them apart via Settings › Apps or
`adb shell pm list packages | grep intelehealth`.

Since side-by-side comparison is the natural way to validate Phases 5–6, consider pulling `app_name`
and the launcher icon to the **front** of Phase 5 rather than doing them in file order.

---

#### Co-install hygiene — decided 2026-09-01, NOT blocking QA

**QA is already unblocked** by separate applicationIds. The four shared surfaces below do not corrupt
anything in normal use. The only real obstacle is cosmetic — identical `app_name` and launcher icon —
so pull those to the **front of Phase 5**.

| # | Item | Verdict |
|---|---|---|
| 1 | Internal DB name | **Do NOT rename.** See below. |
| 2 | PDF export path | Per-flavour, in Phase 5. No migration concern — old files stay, new ones go to the new folder. |
| 3 | `accountType` | Inert. Declared as `io.intelehealth.openmrs` at `authenticator.xml:4`, but **every** AccountManager call site is inside a `/* */` block (`LoginActivity.java:413-415`, `HomeActivity.java:1632`). If ever enabled with two brands installed, Android binds one account type to one authenticator and the second is ignored. |
| 4 | Broadcast leakage | **One-line fix.** `RTCMessageReceiver` (`manifest:141`) is `exported="true"` with no permission, but its only senders are in-app (`MyFirebaseMessagingService.java:53`) and in-library (klivekit `ChatConstant.kt:9`). Set `exported="false"`. The codebase already shows the alternative pattern — `.setPackage(getPackageName())` at `ImagesPushDAO.java:80` and `:115`. |

**Why not to rename the internal DB (item 1).** Two different databases are being conflated:

- `localrecords.db` — opened via `SQLiteOpenHelper(context, DATABASE_NAME, ...)`, i.e. **app-private
  internal storage** at `/data/data/<applicationId>/databases/`. Co-installed brands already get
  separate copies. **There is no collision to fix.**
- `<external>/InteleHealth_DB/Intelehealth.db` (`AppConstants.java:29`) — genuinely shared public
  external storage, but only touched during migration, not normal operation.

The AMC pattern `DATABASE_NAME = BuildConfig.FLAVOR_client + "-localrecords.db"` renames the *internal*
one. Here that solves nothing and creates a real risk: on update `SQLiteOpenHelper` looks for the new
name, does not find it, and **creates a fresh empty database**, orphaning the old file. To a field
health worker, every local patient record disappears.

*If it is ever done anyway*, it needs a one-time migration running **before any DB open**, moving the
`-wal` and `-shm` sidecar files along with the main file — omitting those gives corruption or partial
data rather than a clean failure.

*Worth checking:* whether AMC ships multiple brands under a **single** applicationId. That would make
the rename essential there and still irrelevant here.

---

## Phase 5 — Un-hardcode the brand from `main`

*State verified 2026-09-01. This is the phase that makes the two apps distinguishable on a device,
so **5.1 and 5.2 unblock QA** — do them first regardless of file order.*

### The governing rule, from the 0.4 audit

**Do not add a flavour override. Delete the name from `main` entirely — every qualifier bucket —
and define it only in each flavour.**

A flavour override in plain `values/` loses to any qualified bucket `main` defines, because Android
matches qualifiers *before* source-set priority. Deleting from `main` leaves nothing to shadow, and a
flavour that forgets the string fails at **link time** instead of silently shipping the wrong one.

Measured bucket counts — this is the work:

| Name | Buckets in `main` | Current value in `values/` |
|---|---|---|
| `app_name` | **12** (`values` + 11 locales) | `eLCG नेपाल` |
| `title_activity_login` | **12** | `eZazi` |
| `email_link` | **12** | `support@intelehealth.io` |
| `notification_title` | **4** (`values`, `hi-rIN`, `or`, `ru`) | `eLCG नेपाल is running in the background` |
| `this_option_available_tablet_device` | **4** (`values`, `mr-rIN`, `sw600dp`, `v21`) | *"…for viewing the eLCG"* |

`app/src/ezaziDefault/` is currently **empty** — this phase is what populates it.

---

### Brand-string sweep + triage — settled 2026-09-01

A value-level sweep of every `<string>` in `main` (all 17 `values*` buckets, comments ignored) found
**22 names carrying brand or region content**. Reference-counting them cuts Phase 5's actual work
down sharply. **Rule set by the owner: this is a white-labelling task — placement only. Do NOT
rewrite any string's text.**

#### Group 1 — Brand identity → move to the flavours (Phase 5)

Delete from **every** bucket in `main`, then define once in each flavour's `values/strings.xml`.

| Name | Buckets in main | Refs |
|---|---|---|
| `app_name` | **12** — and they hold *four different brands* (eLCG नेपाल, इंटेलेहेल्थ, Телемед KG, একল আৰোগ্য হেল্পলাইন) | manifest |
| `email_link` | **12** | 2 |
| `title_activity_login` | **12** | 1 |
| `notification_title` | **4** (`values`, `hi-rIN`, `or`, `ru`) | — |

#### Group 2 — Organisation name, NOT product brand -> stays in `main`, no action

**Corrected 2026-09-01.** The initial sweep flagged any `Intelehealth` match as brand leakage. That
was wrong: **Intelehealth is the organisation that produces all of these apps.** Naming it in support
and account contexts is correct for every brand, present and future — a Bangladesh deployment is
still an Intelehealth product.

The distinction to apply from here on:

| | Belongs in |
|---|---|
| **Product brand** — eZazi, eLCG, Ayu | the flavour |
| **Organisation name** — Intelehealth | `main`, shared |

| Name | Value | Verdict |
|---|---|---|
| `email_link` | *"…contact your System Administrator to reset your password or send an email to support@intelehealth.io"* | **Correct as-is.** That is the org's real support address. Also a full sentence translated into 11 languages — moving it would either duplicate 12 translations per brand or lose 11. |
| `enter_registered_mobile_number` | *"Please enter the mobile number registered with Intelehealth"* | **Correct as-is.** The number is registered with the organisation, not with a product. |

*(If a future partner ever white-labels away from the Intelehealth org entirely, these become
per-brand. That is not the case for any planned deployment.)*

#### Group 3 — Clinical terminology, NOT brand → stays in `main`, unmodified

`eLCG` the brand and `LCG` the **WHO Labour Care Guide** are different things. These name the clinical
instrument and are correct as written. Owner's instruction: do not rename; place where referenced.
All are referenced by both brands, so **`main` is the correct placement — no action**.

| Name | Refs |
|---|---|
| `lbl_no_active_lcg_alerts` | 1 |
| `no_data_for_view_lcg_body` | 2 |
| `no_internet_content` | 4 |
| `this_option_available_tablet_device` | 2 |

> `this_option_available_tablet_device` reads *"Please use 8inch or more tablet for viewing the eLCG"*
> and is shown at `TimelineVisitSummaryActivity.java:571` before launching the partogram — so in
> context it means the Labour Care Guide, written as the brand name. **QA has tested and approved this
> wording, so it stays.** Consequence, recorded plainly: it lives in `main`, so `ezaziDefault` users
> also see "eLCG". Changing that would be a rename, which is out of scope for this task.

#### Group 4 — Region-duplicated → Phase 6, to be renamed and placed by the owner

The only genuine region pair in the codebase:

| Name | Buckets | Value | Refs |
|---|---|---|---|
| `str_check_India` | 3 (`values`, `hi-rIN`, `te-rIN`) | India | **4** — `PatientAddressInfoFragment.java`, `…OLD.java`, `fragment_patient_address_info.xml` |
| `str_check_nepal` | 1 (`values`) | Nepal | **1** |

Note the asymmetry: India is translated into 2 locales, Nepal into none. Owner will name these
appropriately and place them in the required brand as part of the region axis.

#### Group 5 — Dead → Phase 7 deletion. **100 `<string>` entries across 11 names, zero references.**

| Name | Buckets | Value |
|---|---|---|
| `Ayu_name` | 12 | `Ayu` |
| `hello_n_n_i_m_ayu_a_digital_health_assistant…` (×2 variants) | 12 each | *"I'm Ayu, a digital health assistant…"* |
| `intelehealth_name` | 12 | `Intelehealth` |
| `intelehealth_a_telemedicine_platform` | 12 | *"Intelehealth is a telemedicine application…"* |
| `hello_thankyou_for_using_intelehealth_app_to_downloa…` | 12 | *"Thank you for using Intelehealth…"* |
| `whatsapp_presc_toast` | 12 | *"Select prescription PDF from file explorer…"* |
| `user_logged_in` | 12 | *"Already logged in!"* |
| `country_matching_String_forIndia` | 2 | `India` |
| `setupUrl` | 1 | `ezazi.intelehealth.org` |
| `hello_blank_fragment` | 1 | *"Hello blank fragment"* |

**Correction 2026-09-01: Ayu is a FEATURE, not a brand.** It is a digital health assistant --
*"Hello, I'm Ayu, a digital health assistant"* -- with its own section marker at
`values/strings.xml:568` and copy translated across all 12 locales. It is currently unwired (0
references), but **that is not the same as abandoned.**

> **Confirm with the PM before deleting the Ayu strings.** If the feature is paused rather than
> dropped, those 12 translations are worth keeping -- re-translating later costs real money. Ask
> alongside the language-picker question. The other names in this group are genuinely dead.

Deleting the rest of this group removes dead weight at zero behavioural risk — but per
**[[verify-before-delete]]**, re-confirm each is still 0-reference immediately before removing, since
Phase 5 will have touched these files in between.


---

- [x] **5.1 — `app_name` and `notification_title`. ✅ DONE (2026-09-01).**
  **32 live `<string>` entries removed from `main`** across 12 buckets, replaced by 7 entries in
  three flavour files. Both flavours merge and link cleanly (`processXStagingDebugResources`).

  | | `ezaziDefault` | `elcgNepal` |
  |---|---|---|
  | `app_name` | `eZazi` | `eLCG नेपाल` |
  | `notification_title` | `eZazi is running in the background` | `eLCG is running in the background` |
  | `title_activity_login` | `eZazi` | `eLCG नेपाल` |
  | `values-hi-rIN/notification_title` | `eZazi बैकग्राउंड में चल रहा है।` | — |

  Three judgement calls, all reversible:
  - **Preserved** eZazi's Hindi `notification_title` into `ezaziDefault/res/values-hi-rIN/` — it was
    genuinely eZazi-branded, so it belongs to that flavour rather than being dropped.
  - **Dropped** the Odia and Russian `notification_title`s: both read *"Realtime sync is ON!"*, text
    from a superseded string, not brand content. Those locales now fall back to the flavour's English.
  - **Deleted** all 11 locale `app_name` / `title_activity_login` values — every one was upstream
    Intelehealth *product* branding (`इंटेलेहेल्थ`, `Телемед KG`, `একল আৰোগ্য হেল্পলাইন`) on keys
    marked `translatable="false"`.

  One inert leftover: a single-line commented entry at `values-ru/strings.xml:41`. Removing commented
  content is not placement work, so it stays.

- [x] **5.2 - Launcher icons. ✅ DONE (2026-09-01), verified in the APKs (2026-09-02).** `aapt2 dump badging` on both staging APKs gives `application-icon-640` = `res/mipmap-anydpi-v26/ic_launcher.xml` for each, resolving to the 5-density eZazi set for `ezaziDefault` and to Nepal’s 309 KB `ic_launcher` plus 238 KB adaptive foreground for `elcgNepal`. The two flavours’ adaptive XMLs are byte-identical **by design** — do not "fix" that; see the Android Studio preview note in the PM decisions. *(Reasoning below kept for the record.)*
  Full provenance in `docs/BRAND-ICON-FORENSICS.md`.

  | Flavour | Icon | Source asset |
  |---|---|---|
  | `ezaziDefault` | purple eZazi monogram | `main/res/mipmap-*/ic_launcher_india.*` - complete, 5 densities + adaptive |
  | `elcgNepal` | green mother-and-baby | `main/res/mipmap-xhdpi/elcg_square_full_bg_icon.png` - **xhdpi only, no adaptive** |

  The teal Jhpiego mark is **NOT** the Nepal launcher icon (owner, 2026-09-01). It was orphaned
  rather than replaced: `5d8f7b77` (19 Jun) moved the manifest off `@mipmap/ic_launcher` and
  repointed three layout logo slots onto main-only Nepal assets in one diff. It remains live on two
  notification paths (`NotificationUtils.java:168`, `:204`) - the only brand-differentiated pixels
  in the app today.

  > **TRAP - restoring `@mipmap/ic_launcher` alone ships the WRONG icon.**
  > `main/res/mipmap-anydpi-v26/ic_launcher_foreground.png` is a 1024x1024 **bitmap misfiled into an
  > `anydpi` folder**, carrying the green artwork. `anydpi` outranks every density bucket, and a
  > flavour overrides main only at a matching name **and qualifier**. `elcgNepal` supplies
  > `ic_launcher_foreground` only as `.webp` in density buckets and has nothing in `anydpi-v26`, so
  > its adaptive XML would resolve the foreground from **main**. Build stays clean, name looks right,
  > wrong artwork ships - the original bug's exact failure mode.

  **Steps:**
  1. **(Owner)** Run Android Studio's Image Asset Studio on `elcg_square_full_bg_icon.png`, output
     into `app/src/elcgNepal/res/`, name `ic_launcher`. This produces the density ramp **and** the
     `anydpi-v26` XML plus a foreground at every density *inside the flavour* - which is what closes
     the trap above. It generates the round variant too, so `app_launcher_logo_nepal_rounded.png` is
     not needed.
  2. Move `ic_launcher*_india.*` (5 densities + both `anydpi-v26` XMLs) into
     `app/src/ezaziDefault/res/mipmap-*/`, renaming `_india` off. Update the adaptive XML's
     `@mipmap/ic_launcher_foreground_india` reference to match.
  3. Delete `elcgNepal`'s teal launcher family (25 files) - superseded by step 1.
  4. Repoint `AndroidManifest.xml:41,:46` to `@mipmap/ic_launcher` / `@mipmap/ic_launcher_round`.
  5. Remove main's misfiled `anydpi-v26` bitmaps and the Nepal launcher assets left behind.

  > **Stop condition (superseded by the Phase 5 stop condition below):** install one flavour, confirm
  > its launcher icon, uninstall, then install the other - both share one applicationId per P1 -
  > purple monogram vs green mother-and-baby - *and* that each fills the adaptive mask rather than
  > being letterboxed onto a plate. Neither brand has a working adaptive icon today.

- [x] **5.3 — Remaining brand strings. ✅ DONE (2026-09-01), scope reduced.**
  `title_activity_login` moved: deleted from all 12 buckets in `main` (every one held upstream
  Intelehealth branding on a `translatable="false"` key), defined once per flavour — `eZazi` and
  `eLCG नेपाल`.
  `email_link` and `enter_registered_mobile_number` **stay in `main` unchanged** — see Group 2.
  They name the organisation, not the product.

- [x] **5.4 — PDF export directory. ✅ CLOSED BY DECISION P11 (2026-09-02) — no code change.** Implemented as a per-flavour `buildConfigField` on 2026-09-02 and **reverted the same day** on the owner’s instruction: `Downloads/eZazi` is the single core export folder for every brand, consistent across projects. Deliberately **not** per-flavour — no `buildConfigField`, no flavour string — so there is nothing for a future brand to diverge on. `WebViewPdfExporter.kt` is byte-identical to HEAD. This also keeps `pdf_saved_body` correct as written, which is why no string file was touched. The folder is not applicationId-scoped, so co-installed brands share it — now intended rather than accidental. *(Original problem statement below.)*
  `WebViewPdfExporter.kt` hardcodes `"eZazi"` at `:396` (MediaStore `RELATIVE_PATH`), `:410` (pre-Q
  `File` fallback) and `:413` (the exception text), plus KDoc at `:28`, `:160`, `:384`. The
  user-facing confirmation string `strings.xml:1160` says `Downloads/eZazi` to match.
  **No string override reaches these** — they need a `BuildConfig` field or a resource read through a
  `Context`. Every eLCG deployment currently writes clinical PDFs into a folder named after the other
  brand, and both co-installed brands write to the same place.

- [x] **5.5 — Notification icons. ✅ DONE (2026-09-01), corrected and verified (2026-09-02).** All seven `setSmallIcon` sites read `R.drawable.ic_notification`, resolving to main’s 512×288 silhouette (19.9% opaque) for both brands. The per-flavour override added on 2026-09-01 was **broken and has been removed** — it was a copy of the adaptive foreground whose figure interior is opaque white (alpha 255), and Android renders notification small icons from the alpha channel alone, so it would have drawn as a featureless blob. Confirmed in both APKs: `res/drawable/ic_notification.png` is 14,152 B and identical in each. *(Original site list below.)*
  `OptimizedSyncForegroundInfo.kt:44` (`R.mipmap.app_lanucher_logo_nepal`),
  `CardGenerationEngine.java:366`, `CallListenerBackgroundService.java:79`,
  `MyFirebaseMessagingService.java:237` (all `R.drawable.app_lanucher_logi_nepal`).
  Each has a commented-out `R.drawable.ezazi_logo` line directly above it — **that was the working
  flavour seam**, added deliberately in `552cb97c` and switched off later. Restoring those lines
  re-enables per-brand notification icons at zero cost, since `elcgNepal` already ships its own
  `ezazi_logo.png`.

  > Note the asset in use is an opaque full-colour bitmap. Android renders notification small icons
  > from the **alpha channel only**, so all five currently draw as a featureless square regardless of
  > brand.

- [x] **5.6 — Clean up the now-redundant overrides. ✅ DONE (2026-09-02).**
  Ran in two passes on the same day, because the second decision landed mid-task.

  **Pass 1 — remove restatements.** Of the **87** names `elcgNepal` declared, **74** were
  byte-identical restatements of `main`: 73 in `colors.xml`, plus `home_logo_width` in `dimens.xml`.
  All 74 removed, leaving 13. Per **[[verify-before-delete]]** byte-identity was re-confirmed against
  `main` immediately before removal rather than trusted from the audit — which was worth doing, since
  it came back **74, not the 73** the audit reported.

  **Pass 2 — P15 drops the teal palette outright.** `colors.xml` was deleted from the flavour, taking
  the 8 surviving overrides with it. Nepal now resolves all 83 colour names from `main`.

  Final state — the flavour declares **5** names:

  | file | names | what |
  |---|---|---|
  | `colors.xml` | — | **deleted** (P15); both brands share main’s palette |
  | `dimens.xml` | 1 | `home_logo_height` (`std_60dp` vs main `std_36dp`) |
  | `ic_launcher_background.xml` | 1 | `#40A47C`, the green behind the launcher icon — kept, it is eLCG’s own artwork |
  | `strings.xml` | 3 | `app_name`, `notification_title`, `title_activity_login` — **absent from main**, which is 5.1/5.3 working as intended |

  **Verification.** Pass 1 was proven a behavioural no-op: all 87 baseline names were captured from
  the merged output first, then re-read after — **87/87 resolved, 0 missing, 0 value changed**, in both
  `elcgNepalStagingDebug` and `elcgNepalProductionDebug`. After pass 2, the merged output confirms
  Nepal takes main’s originals (`colorPrimary` `#2e1e91`, `colorPrimaryDark` `#241871`,
  `titleBar_cardview` `#2e1e91`) while `ic_launcher_background` stays `#40A47C`. Colour declarations
  per source set are now main **83**, `ezaziDefault` **0**, `elcgNepal` **1** — so both brands
  provably share one palette. aapt2 link clean throughout.

  **The "purple on live Nepal screens" concern evaporated rather than being fixed.**
  `titleBar_cardview` = `#2e1e91` is now the *correct* value, and the 207 hardcoded `#2E1E91`
  occurrences across 31 vector drawables in `main` need no per-brand treatment at all. **Nothing about
  colour remains outstanding for Phase 7**, and A10 (colour-override strategy, parked) is resolved by
  P15 rather than deferred.

---

> **Stop condition — REVISED 2026-09-02.** Side-by-side install is no longer possible: per P1 both
> flavours share `org.intelehealth.ezazi`, so QA installs one, tests, **uninstalls**, then installs
> the other. On each build confirm the launcher label and icon, the notification icon, and the
> login-screen title.
> **Then repeat on a handset whose system language is set to Hindi.** Still required even though the
> app is English-only (P5): the launcher label, notification header and Settings entry are rendered
> by *other processes* reading the device locale, so the app’s own `en` pin does not cover them.
> That is exactly where the `:klivekit` shadow surfaced. An English-only check passes while the
> field fails.
> *Status: NOT yet run on a physical device.*

---

## Phase 6 — Region axis (biggest, riskiest, last)
*Nepal logic currently lives in `main` for every variant. This is where clinical data is at stake.*

- [ ] **6.1** Add `DEPLOYMENT_REGION` as a `buildConfigField` on each client flavour. Use BuildConfig,
      not a resource — the consumers (data transformers, validators) often have no `Context`.
- [ ] **6.2** Split location data per country (**A4**) and select by `DEPLOYMENT_REGION`. Two things to
      know first: `state_district_tehsil.json` already has top-level keys `["india","nepal"]` but
      `StateDistMaster.java:14-16` binds `@SerializedName("states")`, so it deserialises to `null`
      today; and **all 7 Nepal provinces have empty district arrays** — a content gap no mechanism fixes.
- [ ] **6.3** Gate the address logic: country literal, district visibility, the `":"` join
      (`PatientAddressInfoFragment.java:221-226`, `:478`, `:550`). **This changes the persisted
      OpenMRS `cityvillage` shape** — confirm with the backend before shipping.
- [ ] **6.4** Gate the 5-digit postal rule (`:697`, `:797`), the `+977` WhatsApp prefix
      (`PatientDetailActivity.java:561`), and the `"91"`-only phone-length branch
      (`ForgotPasswordFragment.java:82-84`).
- [ ] **6.5** Gate Bikram Sambat — **16 files, 73 references, no seam today.** Note the existing prior
      art: `Stage3DataTransformer.kt:44-47` emits `isNepalClient` which `stage3.html` honours, but the
      native PDF renderers ignore it and convert unconditionally. Screen and printed sheet can already
      disagree on the calendar for the same visit.
- [ ] **6.6** Remove the forced `sessionManager.setAppLanguage("en")` at
      `PatientAddressInfoFragment.java:221` — it discards the user's language choice mid-registration.

> **Stop condition:** register a patient end-to-end on each flavour and compare the persisted record
> against what the backend expects. Do **not** rely on UI inspection alone here.

---

## Phase 7 — Deferred cleanups
*Per [[verify-before-delete]]: these must happen, but only after their own check. Make it report
before you make it delete.* Full table in `WHITELABEL.md` → "Supporting moves".

- [ ] Re-enable `MissingTranslation` lint (safe to trial — `abortOnError false` is set, so warnings only)
- [ ] Add the flavour/qualifier collision check — **make it report first**, fail the build only once clean
- [ ] **Delete the 100 dead brand-bearing `<string>` entries** (11 names, 0 references),
      listed in the Phase 5 triage above: `Ayu_name`, both `hello_...ayu...` variants,
      `intelehealth_name`, `intelehealth_a_telemedicine_platform`,
      `hello_thankyou_for_using_intelehealth_app...`, `whatsapp_presc_toast`, `user_logged_in`,
      `country_matching_String_forIndia`, `setupUrl`, `hello_blank_fragment`. Most span all 12
      locale buckets. **Re-confirm 0 references immediately before deleting** -- Phase 5 touches
      these same files. Removing them also retires the fourth product brand ("Ayu") entirely.
- [ ] Delete stale `values-v21` string overrides (keep `search_visits_hint`, `seconday_doct_val_txt`,
      and `am` / `click_to_enter` in `values-w820dp` — re-confirm that list independently)
- [ ] Delete `values-v21/styles.xml` (malformed `<resources>>`, declares nothing, but a `-v21` styles
      override would win on every device)
- [ ] Delete the 11 locale `app_name` overrides (`translatable="false"`, so the translations are wrong
      on their own terms)
- [ ] Decide `ACTIVE_CRASH`: wire it up or drop it (declared on all 12 variants, read by zero code)
- [ ] 🔔 **Language picker decision** — see `WHITELABEL.md` → Open items. Settle this *before* the
      locale-bucket cleanup: if the picker goes, most of main's 11 locale buckets become dead weight
      and this phase shrinks considerably.

---

## Still unscheduled

- **applicationId scheme for brand 3+** — permanent once published, so decide before Bangladesh work starts.
- ~~**73 duplicated colours** in the elcg palette (A10, parked).~~ **Resolved 2026-09-02** by 5.6 and
  P15: the flavour `colors.xml` is deleted, so no colour can silently revert a future `main` change.

---

## PM decisions — 2026-09-02

Answers from the PM call. These supersede the open questions they resolve.

| # | Decision |
|---|---|
| P1 | **Nepal keeps `org.intelehealth.ezazi`.** Comment out `applicationId "org.intelehealth.elcg"` in the `elcgNepal` flavour — do **not** delete the line; the id will be used later, just not now. Both flavours therefore share one package and cannot coexist on a device. QA installs one, tests, uninstalls, installs the other. Accepted. |
| P2 | **Firebase stays on `ezazi-8712a`.** No change. Delete `app/src/elcgNepal/google-services.json` so both flavours fall back to the root `app/google-services.json`. Closes audit blocker B5. |
| P3 | **Versions unchanged: `elcgNepal` 18 / 3.1.1, `ezaziDefault` 18 / 3.0.0.** eLCG is at 3.1.1 because of the Nepal releases; eZazi is at 3.0.0 because of the Play listing. Already implemented — no action. |
| P4 | **`ezaziDefault` on Play is a demo listing only.** It will never ship as a product to users, and eZazi India will never deploy. The next Nepal release goes out under whatever package the PM is given; today that is `org.intelehealth.ezazi`. Closes audit blocker B6 — there is no fleet to upgrade in place. |
| P5 | **English only, for every project including Bangladesh.** No in-app language picker is required. |
| P6 | **Do not touch any string file.** No locale folders removed, no `values-hi-rIN` removal, nothing deleted. Leave main's 11 locale buckets exactly as they are. This overrides the Phase 7 locale-bucket cleanup. |
| P7 | **Ayu stays.** Unwired and not needed, but the strings are not to be deleted. |
| P8 | **`ic_launcher_legacy_playstore` is eLCG Nepal artwork — move it, do not delete.** It belongs to the Nepal flavour, not the core project. Note it is one 1024×1024 file duplicated across five density folders (identical md5, ~2 MB), one copy wrongly in `mipmap-anydpi-v26`. |
| P9 | **The four `elcgNepal/res/drawable` files are Jhpiego and are NOT to be used** — `home_logo.png`, `home_logo_older.png`, `login_screen_icon.jpeg`, `logo_ezazi.jpeg`. Partner-supplied temporary assets. Verified by opening each. |
| P10 | **Notification icon: revert to the prior behaviour** — one shared icon, no per-flavour override. The `elcgNepal/res/drawable/ic_notification.png` introduced in Phase 5 is broken (figure interior is opaque white, so it renders as a featureless blob) and is to be removed. |
| P11 | **PDF folder stays `Downloads/eZazi` for every brand — REVISED 2026-09-02.** The initial answer was a per-brand folder; the owner reversed it: one core folder, consistent across all projects, so exported data always lands in the same place. Consequences: this is deliberately **not** a per-flavour value, so no `buildConfigField` and no flavour string — the literal stays in `WebViewPdfExporter.kt`. It also keeps `pdf_saved_body` (`values/strings.xml:1148`) correct as written, which is why that string needs no change. Note the folder is not applicationId-scoped, so on a dual-install device both brands share it — now intended rather than accidental. |
| P12 | **Wire `ACTIVE_CRASH`.** `IntelehealthApplication.java:157` currently hardcodes `setCrashlyticsCollectionEnabled(true)`; change to `BuildConfig.ACTIVE_CRASH` (already `false` on dev, `true` on staging and production). |
| P13 | **Bangladesh package naming: skipped**, to be confirmed later. |
| P14 | **India location master data: dropped**, not a concern. |
| P15 | **The teal palette is dropped permanently — DECIDED 2026-09-02.** Teal was **Jhpiego’s** brand colour, supplied by the partner as a temporary measure, and will not be implemented. Per the partner’s own message: *"We will keep our original logo colors everywhere for our core projects."* This is the same call as P9, which strips the Jhpiego logo files — the palette was the other half of that branding. **Action taken:** `app/src/elcgNepal/res/values/colors.xml` deleted outright. Both brands now share one palette from `main` (`ezaziDefault` declares 0 colour names, `elcgNepal` declares 1). Verified in the merged output: `colorPrimary` `#2e1e91`, `colorPrimaryDark` `#241871`, `titleBar_cardview` `#2e1e91`. **Kept deliberately:** `ic_launcher_background` `#40A47C`, the green behind the mother-and-baby launcher icon — that is eLCG Nepal’s own approved artwork, not Jhpiego teal, so it stays. Consequence worth noting: the client dimension now varies **only** by app name, launcher icon and endpoints, so a flavour `colors.xml` should not be recreated unless a future brand genuinely needs one. |

### Deferred to Phase 6 by this call

- **Region split confirmed:** India = Gregorian dates, 6-digit PIN, `+91`. Nepal = Bikram Sambat, 5-digit postal, `+977`.
- **Postal keystroke limit becomes dynamic** — initialise the input filter to 6 digits when the selected project is India, else 5. **Do not implement now.**
- **`cityvillage` format is settled:** the `district:city` colon form is **India only**; Nepal stores the bare village name. Current implementation already matches. Two follow-ons stay in Phase 6, not now:
  - the prefill guard at `PatientAddressInfoFragment.java:279` requires `split(":").length == 2`, so City/Village blanks on edit and back-navigation for Nepal
  - `:594` calls `substring(0, 2)` for `REGISTRATION_NUMBER`, which throws on a one-character village name

### Also found, for Phase 7

- `app/src/main/res/drawable-v24/ic_launcher_foreground.xml` is the stock Android Studio template vector. Dead debris; it is a `drawable`, not a `mipmap`, so it does not affect the adaptive icons.
- **The adaptive-icon XMLs are byte-identical across both flavours by design.** Android Studio's gutter preview resolves `@mipmap/ic_launcher_foreground` against the *selected build variant*, so previewing elcgNepal's file with ezaziDefault selected shows the eZazi mark. Verified correct in the merged output for both flavours. Do not "fix" this.
