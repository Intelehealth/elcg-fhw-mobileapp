package org.intelehealth.ezazi.activities.epartogramActivity;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wire-format DTO consumed by the offline ePartogram WebView.
 *
 * Field names mirror the Angular component bindings in epartogram.component.html
 * (e.g. {@code pinfo.name}, {@code parameters[i].stage1values}, {@code timeFullStage1},
 * {@code sosEncounterUUIDs}). Keep them in lockstep with that template.
 *
 * Serialised via Gson and returned by {@link EpartogramBridge#getEpartogramData(String)}.
 */
public class EpartogramSnapshot {

    @SerializedName("pinfo")
    public PatientInfo pinfo = new PatientInfo();

    @SerializedName("visitCompleted")
    public boolean visitCompleted;

    @SerializedName("visitCompleteReason")
    public String visitCompleteReason;

    @SerializedName("outOfTimeReason")
    public String outOfTimeReason;

    @SerializedName("referTypeOtherReason")
    public String referTypeOtherReason;

    @SerializedName("birthOutcome")
    public String birthOutcome;

    @SerializedName("birthOutcomeOther")
    public String birthOutcomeOther;

    @SerializedName("birthWeight")
    public String birthWeight;

    @SerializedName("apgar1")
    public String apgar1;

    @SerializedName("apgar5")
    public String apgar5;

    @SerializedName("babyGender")
    public String babyGender;

    @SerializedName("babyStatus")
    public String babyStatus;

    @SerializedName("motherDeceased")
    public String motherDeceased;

    @SerializedName("motherDeceasedReason")
    public String motherDeceasedReason;

    /** ISO timestamps, one per stage-1 column (full grid including empty sub-columns). */
    @SerializedName("timeFullStage1")
    public List<String> timeFullStage1 = new ArrayList<>();

    @SerializedName("timeFullStage2")
    public List<String> timeFullStage2 = new ArrayList<>();

    /** ISO timestamps, one per recorded hour bucket in stage 1 (no sub-columns). */
    @SerializedName("timeStage1")
    public List<String> timeStage1 = new ArrayList<>();

    @SerializedName("timeStage2")
    public List<String> timeStage2 = new ArrayList<>();

    /** Number of sub-columns each hour bucket spans. Mirrors {@code subColsPerHourStageN}. */
    @SerializedName("subColsPerHourStage1")
    public List<Integer> subColsPerHourStage1 = new ArrayList<>();

    @SerializedName("subColsPerHourStage2")
    public List<Integer> subColsPerHourStage2 = new ArrayList<>();

    /** One entry per stage-1 hour bucket; carries the encounter UUID for SOS detection. */
    @SerializedName("encuuid1")
    public List<EncounterRef> encuuid1 = new ArrayList<>();

    @SerializedName("encuuid2")
    public List<EncounterRef> encuuid2 = new ArrayList<>();

    /** One entry per stage-1 sub-column (full grid). */
    @SerializedName("encuuid1Full")
    public List<EncounterRef> encuuid1Full = new ArrayList<>();

    @SerializedName("encuuid2Full")
    public List<EncounterRef> encuuid2Full = new ArrayList<>();

    /** Encounter UUIDs that should render with the SOS treatment. */
    @SerializedName("sosEncounterUUIDs")
    public Set<String> sosEncounterUUIDs = new HashSet<>();

    @SerializedName("initialsStage1")
    public List<String> initialsStage1 = new ArrayList<>();

    @SerializedName("initialsStage2")
    public List<String> initialsStage2 = new ArrayList<>();

    /** Indexed access (parameters[0..N]); index order must match epartogram.component.html. */
    @SerializedName("parameters")
    public List<Parameter> parameters = new ArrayList<>();

    @SerializedName("assessmentHistory")
    public List<HistoryItem> assessmentHistory = new ArrayList<>();

    @SerializedName("planHistory")
    public List<HistoryItem> planHistory = new ArrayList<>();

    @SerializedName("medicationPrescribedHistory")
    public List<HistoryItem> medicationPrescribedHistory = new ArrayList<>();

    @SerializedName("oxytocinPrescribedHistory")
    public List<HistoryItem> oxytocinPrescribedHistory = new ArrayList<>();

    @SerializedName("ivPrescribedHistory")
    public List<HistoryItem> ivPrescribedHistory = new ArrayList<>();

    public static class PatientInfo {
        @SerializedName("name") public String name;
        @SerializedName("Parity") public String parity;
        @SerializedName("Gravida") public String gravida;
        @SerializedName("LMP") public String lmp;
        @SerializedName("EDD") public String edd;
        @SerializedName("LaborOnset") public String labourOnset;
        @SerializedName("ActiveLaborDiagnosed") public String activeLaborDiagnosed;
        @SerializedName("MembraneRupturedTimestamp") public String membraneRupturedTimestamp;
        @SerializedName("Riskfactors") public String riskFactors;
    }

    public static class EncounterRef {
        @SerializedName("enc_uuid") public String encUuid;
        @SerializedName("encounter_time") public String encounterTime;
    }

    /**
     * One row in the partogram grid. The Angular template indexes this list
     * by parameter position (parameters[0] = Companion, parameters[4] = Baseline FHR, etc.).
     */
    public static class Parameter {
        @SerializedName("section") public String section;
        @SerializedName("label") public String label;
        @SerializedName("conceptUuid") public String conceptUuid;
        @SerializedName("stage1values") public List<Cell> stage1values = new ArrayList<>();
        @SerializedName("stage2values") public List<Cell> stage2values = new ArrayList<>();
    }

    /**
     * A grid cell. {@code value} is normally a string; for Oxytocin / IV Fluids it's
     * a structured payload ({@link InfusionValue}) so the Angular template's
     * {@code item?.value?.strength} / {@code item?.value?.type} bindings work.
     * For Medicine, the cell holds a list — see {@link Cell#valueList}.
     */
    public static class Cell {
        @SerializedName("value") public Object value;
        @SerializedName("comment") public String comment;
        @SerializedName("initial") public String initial;
        @SerializedName("obsDatetime") public String obsDatetime;
        /** Used for Medicine rows where multiple meds may be recorded in the same cell. */
        @SerializedName("valueList") public List<MedicineEntry> valueList;
    }

    public static class InfusionValue {
        @SerializedName("strength") public String strength;
        @SerializedName("type") public String type;
        @SerializedName("otherType") public String otherType;
        @SerializedName("infusionRate") public String infusionRate;
        @SerializedName("infusionStatus") public String infusionStatus;
    }

    public static class MedicineEntry {
        @SerializedName("value") public String value;
        @SerializedName("initial") public String initial;
        @SerializedName("obsDatetime") public String obsDatetime;
    }

    public static class HistoryItem {
        @SerializedName("value") public Object value;
        @SerializedName("initial") public String initial;
        @SerializedName("obsDatetime") public String obsDatetime;
    }
}
