package org.intelehealth.ezazi.utilities;

import org.intelehealth.ezazi.BuildConfig;

/**
 * Single source of truth for every behaviour that varies by deployment region.
 *
 * <p>Region is derived from the {@code client} product flavour, not from the server URL and not from
 * a country name typed into or displayed by the UI. Deriving it from the build means it cannot drift
 * at runtime when someone re-points the server in the setup screen.
 *
 * <p>Every test is written as "is this Nepal?" and never as "is this not-India?". The difference only
 * shows up when a third brand is added: the negative form would silently hand a new deployment
 * Nepal's Bikram Sambat calendar and Nepal's address rules, because it is not ezaziDefault. The
 * positive form defaults every future brand to Gregorian dates and the standard address shape, which
 * is what a new deployment should inherit.
 *
 * <p>Callers should prefer the named capability methods over {@link #isNepal()} so the reason for a
 * branch is legible at the call site.
 */
public final class AppRegion {

    private AppRegion() {
    }

    public static boolean isNepal() {
        return BuildConfig.FLAVOR_client.equalsIgnoreCase(FlavorKeys.ELCG_NEPAL);
    }

    /**
     * Nepal captures and displays dates in Bikram Sambat. Every other region uses the Gregorian
     * calendar. Note that only capture and display vary: persisted values are Gregorian for every
     * region, and that must stay true.
     */
    public static boolean usesBikramSambat() {
        return isNepal();
    }

    /**
     * Nepal labels the first-level administrative division "Province" where every other region calls
     * it "State". This is a labelling difference only — the field, its adapter and its data are the
     * same — so it must not be used to gate behaviour.
     *
     * <p>Whether a <em>district</em> field appears is deliberately NOT a region question. It is
     * derived from the shipped address asset: a state that lists districts gets the field, one that
     * lists none does not. Encoding "Nepal has no districts" as a brand rule is what let a Nepal-only
     * change disable India's district loading when the two lines were merged. The asset already knows
     * the answer, so it is the only place that is asked.
     */
    public static boolean usesProvinceLabel() {
        return isNepal();
    }

    /**
     * Video calling to a remote doctor is offered on eZazi only. Nepal ships the same screens with the
     * button hidden, so the feature is absent rather than merely unused.
     *
     * <p>Both surfaces that host the button read this, and the layouts carry no visibility of their
     * own, so the two screens cannot drift apart.
     */
    public static boolean showsVideoCall() {
        return !isNepal();
    }

    public static int postalCodeLength() {
        return isNepal() ? 5 : 6;
    }

    public static String dialCode() {
        return isNepal() ? "+977" : "+91";
    }

    /**
     * The country value written to OpenMRS. This comes from a build config field rather than a string
     * resource so that it can never be localised by accident; a translated country name would corrupt
     * the persisted record. The on-screen label is a separate string resource.
     *
     * <p>Country is a property of the deployment, not of the patient. Each client flavour talks to its
     * own server, so every record a build can reach belongs to that build's country — which makes this
     * the definition of the correct value, not merely a sensible default. Persist it from here
     * unconditionally, on create and on edit alike. Never read it back off the address screen's country
     * field: that field is a label, not an input, and taking a display string as the value to store is
     * how a tablet-only layout once wrote "India" into Nepali records.
     */
    public static String persistedCountryName() {
        return BuildConfig.COUNTRY_NAME;
    }
}
