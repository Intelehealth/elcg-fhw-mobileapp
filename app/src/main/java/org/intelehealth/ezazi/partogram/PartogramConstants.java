package org.intelehealth.ezazi.partogram;

import org.intelehealth.ezazi.app.AppConstants;
import org.intelehealth.ezazi.app.IntelehealthApplication;
import org.intelehealth.ezazi.partogram.model.ParamInfo;
import org.intelehealth.ezazi.utilities.SessionManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public class PartogramConstants {
    public static String DROPDOWN_SINGLE_SELECT_TYPE = "A";
    public static String DROPDOWN_MULTI_SELECT_TYPE = "B";
    public static String AUTOCOMPLETE_SUGGESTION_EDITTEXT = "A1";

    public static String INPUT_TXT_TYPE = "C";
    public static String INPUT_TXT_2_CHR_TYPE = "D";

    public static String INPUT_INT_1_DIG_TYPE = "E";
    public static String INPUT_INT_2_DIG_TYPE = "F";
    public static String INPUT_INT_3_DIG_TYPE = "G";
    public static String INPUT_DOUBLE_4_DIG_TYPE = "H";

    public static String RADIO_SELECT_TYPE = "I";

    private static SessionManager sessionManager = null;

    public static final int STAGE_1 = 1;
    public static final int STAGE_2 = 2;

    public static final int STAGE_3 = 3;

    public static String INPUT_INT_4_DIG_TYPE = "J";

    public enum AccessMode {
        WRITE, EDIT, READ
    }

    public static final String TIMELINE_MODE = "access_mode";

    public static String[] SECTION_LIST = {
            "Supportive care",
            "Baby",
            "Woman",
            "Labour Progress",
            "Medication Administration",
            "Shared Decision Making"
//            ,
//            "Initials"
    };

    public static String[] infusionStatus = new String[]{"Started", "Continued", "Stopped"};

    public enum Params {
        COMPANION("Companion", "5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"),
        PAIN_RELIEF("Pain relief", "9d313f72-538f-11e6-9cfe-86f436325720"),
        ORAL_FLUID("Oral Fluid", "9d31451b-538f-11e6-9cfe-86f436325720"),
        POSTURE("Posture", "9d3148b1-538f-11e6-9cfe-86f436325720"),
        FHR_DEC("FHR Deceleration", "9d31573c-538f-11e6-9cfe-86f436325720"),
        AMNIOTIC_FLUID("Amniotic Fluid", "9d3160a6-538f-11e6-9cfe-86f436325720"),

        FETAL_POSITION("Fetal Position", "9d316387-538f-11e6-9cfe-86f436325720"),
        CAPUT("Caput", "9d316761-538f-11e6-9cfe-86f436325720"),
        MOULDING("Moulding", "9d316823-538f-11e6-9cfe-86f436325720"),
        SYSTOLIC_BP("Systolic BP", "5085AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"),
        DIASTOLIC_BP("Diastolic BP", "5086AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"),
        IV_FLUID("IV Fluids", "98c5881f-b214-4597-83d4-509666e9a7c9"),

        TEMPERATURE("Temperature (°F)", "5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"),
        BASELINE_FHR("Baseline FHR", "9d315400-538f-11e6-9cfe-86f436325720"),
        PULSE("Pulse", "5087AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"),
        URINE_PROTEIN("Urine protein", "9d3168a7-538f-11e6-9cfe-86f436325720"),
        URINE_ACETONE("Urine Acetone", "968f9bc2-b33d-4daf-b59f-79d9a899e018"),
        CONTRACTION_PER_10_MIN("Contractions per 10 min", "9d316929-538f-11e6-9cfe-86f436325720"),
        DURATION_OF_CONTRACTION("Duration of contractions", "9d3169af-538f-11e6-9cfe-86f436325720"),
        CERVIX_PLOT("Cervix Plot[X]", "9d316ab5-538f-11e6-9cfe-86f436325720"),
        DESCENT_PLOT("Descent Plot[O]", "9d316d41-538f-11e6-9cfe-86f436325720"),
        OXYTOCIN("Oxytocin (U/L, drops/min)", "9d316d82-538f-11e6-9cfe-86f436325720"),
        MEDICINE("Medicine", "c38c0c50-2fd2-4ae3-b7ba-7dd25adca4ca"),
        ASSESSMENT("Assessment", "67a050c1-35e5-451c-a4ab-fff9d57b0db1"),
        PLAN("Plan", "162169AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"),
        SUPERVISOR_DOCTOR("Supervisor Doctor", "7a9cb7bc-9ab9-4ff0-ae82-7a1bd2cca93e"),
        PRESCRIBED_OXYTOCIN("Prescribed Oxytocin (U/L, drops/min)", "6eef8ae6-e6b3-46f1-a58c-393bd6d0e8c8"),
        PRESCRIBED_MEDICINE("Prescribed Medicine", "ce75ed49-0eac-44f0-ac91-bcf9c5d1d700"),
        PRESCRIBED_IV_FLUID("Prescribed IV Fluids", "0c21f925-d225-48ce-9e98-bb1cc5638e04"),

        RESPIRATORY_RATE_MOTHER("Respiratory Rate (per min)", "5242AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"),
        BLOOD_LOSS_MOTHER("Blood loss (ml)", "585c2f4a-16d3-4fd7-8b50-2927e14ca379"),
        UTERUS_CONTRACTED_MOTHER("Uterus contracted", "13de11c4-76cc-4851-8b37-513cee310c93"),
        URINE_PASSED_MOTHER("Urine passed in 2 hours", "cb6ee6f9-6476-4b44-adac-8e85798a716c"),
        HEMATOMA_MOTHER("Hematoma", "afd25b45-cb3f-4e77-8c3d-8b8f6cc83944"),
        ONGOING_COMPLICATIONS_MOTHER("Any signs of ongoing \ncomplications", "caa174fc-5889-443e-b53d-701fb2e85c23"),
        ASSESSMENT_MOTHER("Assessment", "67a050c1-35e5-451c-a4ab-fff9d57b0db1"),
        PLAN_MOTHER("Plan", "5991aeaf-c4c3-457d-ada1-334d90c545fc"),
        GRUNTING_NEWBORN("Grunting", "694d18a3-8163-43ba-9ac8-cf07a8c84b6b"),
        CHEST_INDRAWING_NEWBORN("Chest indrawing", "5d9c44df-eb2b-435b-aac9-ee6b83ee6d27"),
        FAST_BREATHING_NEWBORN("Fast breathing", "9d2ebbfd-538f-11e6-9cfe-86f436325720"),
        RESPIRATORY_RATE_NEWBORN("Respiratory Rate (per min)", "e642e4a6-2799-488f-9f95-eec27a3bb756"),
        SPO2_NEWBORN("SPO2 (%)", "ef9490bc-b039-45bc-98a8-34d55b750e04"),
        FEET_WARM_NEWBORN("Feet (warm)", "d16058b1-8756-4db2-bf71-834466e1f436"),
        TEMPERATURE_NEWBORN("Temperature (°F)", "5c74ba33-b194-4a1a-b6e8-cfca07d2a593"),
        SKIN_COLOR_NEWBORN("Colour of the skin (Cyanosed)", "641cf6ac-ce38-42ca-9672-d30dce1c56de"),
        UC_OOZING_NEWBORN("Umbilical cord oozing", "995ab5c8-76a0-4e3c-ac70-2270fdbc14a8"),
        SUCKING_FEEDING_NEWBORN("Suckling & Feeding", "1a49a4ef-ad20-4cd8-b176-06eb6fbf9971"),
        ONGOING_COMPLICATIONS_NEWBORN("Any signs of ongoing \ncomplications", "820ae093-d3ab-402a-9a81-279963cf9dc8"),
        ASSESSMENT_NEWBORN("Assessment", "15fc4eca-7635-4e7c-baa4-20c250cfe62a"),
        PLAN_NEWBORN("Plan", "a79853d9-e45e-41b2-8473-b19e5cae66cb");

        public final String value;
        public final String conceptId;

        Params(String value, String conceptId) {
            this.value = value;
            this.conceptId = conceptId;
        }
    }

    public static TreeMap<String, List<ParamInfo>> getSectionParamInfoMasterMap(int stage) {
        TreeMap<String, List<ParamInfo>> sectionParamInfoMap = new TreeMap<>();

        sessionManager = new SessionManager(IntelehealthApplication.getAppContext());
        //Supportive care
        List<ParamInfo> stringList = new ArrayList<ParamInfo>();
        ParamInfo paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[0]);
        paramInfo.setParamName(Params.COMPANION.value);
        paramInfo.setParamDateType(DROPDOWN_SINGLE_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"Yes", "No", "Woman Declines"});
        paramInfo.setValues(new String[]{"Y", "N", "D"});
        paramInfo.setConceptUUID("5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        paramInfo.setOnlyOneHourField(true);
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[0]);
        paramInfo.setParamName(Params.PAIN_RELIEF.value);
        paramInfo.setParamDateType(DROPDOWN_SINGLE_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"Yes", "No", "Woman Declines"});
        paramInfo.setValues(new String[]{"Y", "N", "D"});
        paramInfo.setConceptUUID("9d313f72-538f-11e6-9cfe-86f436325720");
        paramInfo.setOnlyOneHourField(true);
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[0]);
        paramInfo.setParamName(Params.ORAL_FLUID.value);
        paramInfo.setParamDateType(DROPDOWN_SINGLE_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"Yes", "No", "Woman Declines"});
        paramInfo.setValues(new String[]{"Y", "N", "D"});
        paramInfo.setConceptUUID("9d31451b-538f-11e6-9cfe-86f436325720");
        paramInfo.setOnlyOneHourField(true);
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[0]);
        paramInfo.setParamName(Params.POSTURE.value);
        paramInfo.setParamDateType(DROPDOWN_SINGLE_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"Supine", "Mobile"});
        paramInfo.setValues(new String[]{"SP", "MO"});
        paramInfo.setConceptUUID("9d3148b1-538f-11e6-9cfe-86f436325720");
        paramInfo.setOnlyOneHourField(true);
        stringList.add(paramInfo);

        sectionParamInfoMap.put(SECTION_LIST[0], stringList);

        //BABY
        stringList = new ArrayList<>();

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[1]);
        paramInfo.setParamName(Params.BASELINE_FHR.value);
        paramInfo.setParamDateType(INPUT_INT_3_DIG_TYPE);
        paramInfo.setHalfHourField(true);
        paramInfo.setFifteenMinField(true);
        paramInfo.setConceptUUID("9d315400-538f-11e6-9cfe-86f436325720");
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[1]);
        paramInfo.setParamName(Params.FHR_DEC.value);
        paramInfo.setParamDateType(DROPDOWN_SINGLE_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"No", "Early", "Late", "Variable"});
        paramInfo.setValues(new String[]{"N", "E", "L", "V"});
        paramInfo.setHalfHourField(true);
        paramInfo.setFifteenMinField(true);
        paramInfo.setConceptUUID("9d31573c-538f-11e6-9cfe-86f436325720");
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[1]);
        paramInfo.setParamName(Params.AMNIOTIC_FLUID.value);
        paramInfo.setParamDateType(DROPDOWN_SINGLE_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"I (Intact)", "C (Clear fluid)", "M+ (Meconium stained fluid-Nonsignificant)",
                "M++ (Meconium stained fluid-Medium)", "M+++ (Meconium stained fluid-Thick)", "B (Blood stained)"});
        paramInfo.setValues(new String[]{"I", "C", "M+", "M++", "M+++", "B"});
        paramInfo.setConceptUUID("9d3160a6-538f-11e6-9cfe-86f436325720");
        paramInfo.setFiveHourField(true);
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[1]);
        paramInfo.setParamName(Params.FETAL_POSITION.value);
        paramInfo.setParamDateType(DROPDOWN_SINGLE_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"A (Occiput anterior)", "P (Occiput posterior)", "T (Occiput transverse)"});
        paramInfo.setValues(new String[]{"A", "P", "T"});
        paramInfo.setConceptUUID("9d316387-538f-11e6-9cfe-86f436325720");
        paramInfo.setFiveHourField(true);
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[1]);
        paramInfo.setParamName(Params.CAPUT.value);
        paramInfo.setParamDateType(DROPDOWN_SINGLE_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"0 (None)", "+", "++", "+++ (Marked)"});
        paramInfo.setValues(new String[]{"0", "+", "++", "+++"});
        paramInfo.setConceptUUID("9d316761-538f-11e6-9cfe-86f436325720");
        paramInfo.setFiveHourField(true);
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[1]);
        paramInfo.setParamName(Params.MOULDING.value);
        paramInfo.setParamDateType(DROPDOWN_SINGLE_SELECT_TYPE);
//        paramInfo.setOptions(new String[]{"None", "Sutures apposed", "Sutures overlapped but reducible", "Sutures overlapped and not reducible"});
        paramInfo.setOptions(new String[]{"0 (None)", "+ (Sutures apposed)", "++ (Sutures overlapped but reducible)", "+++ (Sutures overlapped but not reducible)"});
        paramInfo.setValues(new String[]{"0", "+", "++", "+++"});
        paramInfo.setConceptUUID("9d316823-538f-11e6-9cfe-86f436325720");
        paramInfo.setFiveHourField(true);
        stringList.add(paramInfo);

        sectionParamInfoMap.put(SECTION_LIST[1], stringList); //BABY

        //Woman
        stringList = new ArrayList<>();

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[2]);
        paramInfo.setParamName(Params.PULSE.value);
        paramInfo.setParamDateType(INPUT_INT_3_DIG_TYPE);
        paramInfo.setConceptUUID("5087AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        paramInfo.setFiveHourField(true);
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[2]);
        paramInfo.setParamName(Params.SYSTOLIC_BP.value);
        paramInfo.setParamDateType(INPUT_INT_3_DIG_TYPE);
        paramInfo.setConceptUUID("5085AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        paramInfo.setFiveHourField(true);
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[2]);
        paramInfo.setParamName(Params.DIASTOLIC_BP.value);
        paramInfo.setParamDateType(INPUT_INT_3_DIG_TYPE);
        paramInfo.setConceptUUID("5086AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        paramInfo.setFiveHourField(true);
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[2]);
        paramInfo.setParamName(Params.TEMPERATURE.value); // in centigrade i.e. C
        paramInfo.setParamDateType(INPUT_DOUBLE_4_DIG_TYPE);
        paramInfo.setConceptUUID("5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        paramInfo.setFiveHourField(true);
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[2]);
        paramInfo.setParamName("Urine protein");
        paramInfo.setParamDateType(DROPDOWN_SINGLE_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"No Proteinuria (P-)", "Trace of Proteinuria (P Trace)", "P1+", "P2+", "P3+", "P4+"});
//        paramInfo.setValues(new String[]{"P-", "P", "P1", "P2", "P3"});
        paramInfo.setValues(new String[]{"Negative", "Trace", "P1+", "P2+", "P3+", "P4+"});
        paramInfo.setConceptUUID("9d3168a7-538f-11e6-9cfe-86f436325720");
        paramInfo.setFiveHourField(true);
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[2]);
        paramInfo.setParamName("Urine Acetone");
        paramInfo.setParamDateType(DROPDOWN_SINGLE_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"No Acetonuria (A-)", "Trace of Acetonuria (A Trace)", "A1+", "A2+", "A3+", "A4+"});
        paramInfo.setValues(new String[]{"Negative", "Trace", "A1+", "A2+", "A3+", "A4+"});
        paramInfo.setConceptUUID("968f9bc2-b33d-4daf-b59f-79d9a899e018");
        paramInfo.setFiveHourField(true);
        stringList.add(paramInfo);

        sectionParamInfoMap.put(SECTION_LIST[2], stringList);//Woman

        //Labour Progress
        stringList = new ArrayList<>();

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[3]);
        paramInfo.setParamName("Contractions per 10 min");
        paramInfo.setParamDateType(INPUT_INT_1_DIG_TYPE);
        paramInfo.setHalfHourField(true);
        paramInfo.setFifteenMinField(true);
        paramInfo.setConceptUUID("9d316929-538f-11e6-9cfe-86f436325720");
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[3]);
        paramInfo.setParamName("Duration of contractions");
        paramInfo.setParamDateType(INPUT_INT_3_DIG_TYPE);
        paramInfo.setHalfHourField(true);
        paramInfo.setFifteenMinField(true);
        paramInfo.setConceptUUID("9d3169af-538f-11e6-9cfe-86f436325720");
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[3]);
        paramInfo.setParamName("Cervix Plot[X]");
        paramInfo.setParamDateType(DROPDOWN_SINGLE_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"10", "9", "8", "7", "6", "5"});
        paramInfo.setValues(new String[]{"10", "9", "8", "7", "6", "5"});
        if (stage == STAGE_2) {
            paramInfo.setOptions(new String[]{"Pushing", "10"});
            paramInfo.setValues(new String[]{"P", "10"});
            paramInfo.setFifteenMinField(true);
        } else {
            paramInfo.setFiveHourField(true);
        }
        paramInfo.setConceptUUID("9d316ab5-538f-11e6-9cfe-86f436325720");
        //paramInfo.setFifteenMinField(true); //as per WHO LCG
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[3]);
        paramInfo.setParamName("Descent Plot[O]");
        paramInfo.setParamDateType(DROPDOWN_SINGLE_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"5", "4", "3", "2", "1", "0"});
        paramInfo.setValues(new String[]{"5", "4", "3", "2", "1", "0"});
        paramInfo.setConceptUUID("9d316d41-538f-11e6-9cfe-86f436325720");
        stringList.add(paramInfo);
        if (stage == STAGE_2) {
            paramInfo.setFifteenMinField(true);

        } else {
            paramInfo.setFiveHourField(true);
        }
        sectionParamInfoMap.put(SECTION_LIST[3], stringList);//Labour Progress

        //Medication
        stringList = new ArrayList<>();

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[4]);
//        if (sessionManager.getOxytocinValue() != null)
//            paramInfo.setParamName("Oxytocin (" + sessionManager.getOxytocinValue() + ")");
//        else
        paramInfo.setParamName("Oxytocin (U/L, drops/min)");
        // paramInfo.setParamDateType(INPUT_TXT_TYPE);
        paramInfo.setParamDateType(RADIO_SELECT_TYPE);
        paramInfo.setStatus(infusionStatus);
        paramInfo.setConceptUUID("9d316d82-538f-11e6-9cfe-86f436325720");
        //paramInfo.setOnlyOneHourField(true);
        paramInfo.setEachEncounterField(true);
        stringList.add(paramInfo);


        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[4]);
        paramInfo.setParamName("Medicine");
        paramInfo.setParamDateType(RADIO_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"Yes", "No"});
        //paramInfo.setValues(new String[]{"Y", "N"});
        paramInfo.setConceptUUID("c38c0c50-2fd2-4ae3-b7ba-7dd25adca4ca");
        //paramInfo.setOnlyOneHourField(true);
        paramInfo.setEachEncounterField(true);
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[4]);
        paramInfo.setParamName(Params.IV_FLUID.value);
        paramInfo.setParamDateType(RADIO_SELECT_TYPE);
        paramInfo.setRadioOptions(new String[]{"Yes", "No"});
        paramInfo.setOptions(new String[]{"Ringer Lactate", "Normal Saline", "Dextrose 5% (D5)", "Other IV Fluid*"});
        paramInfo.setValues(new String[]{"Ringer Lactate", "Normal Saline", "Dextrose 5% (D5)", AppConstants.OTHER_OPTION});
        paramInfo.setStatus(infusionStatus);
        paramInfo.setConceptUUID("98c5881f-b214-4597-83d4-509666e9a7c9");
        //paramInfo.setOnlyOneHourField(true);
        paramInfo.setEachEncounterField(true);
        stringList.add(paramInfo);

        sectionParamInfoMap.put(SECTION_LIST[4], stringList);//Medication


       /* paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[4]);
        paramInfo.setParamName("Medicine");
        paramInfo.setParamDateType(RADIO_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"Yes", "No"});
        //paramInfo.setValues(new String[]{"Y", "N"});
        paramInfo.setConceptUUID("c38c0c50-2fd2-4ae3-b7ba-7dd25adca4ca");
        //paramInfo.setOnlyOneHourField(true);
        paramInfo.setEachEncounterField(true);
        stringList.add(paramInfo);*/

        //Shared Decision Making
        stringList = new ArrayList<>();


        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[5]);
        paramInfo.setParamName("Assessment");
        paramInfo.setParamDateType(RADIO_SELECT_TYPE);
        paramInfo.setConceptUUID("67a050c1-35e5-451c-a4ab-fff9d57b0db1");
        paramInfo.setOptions(new String[]{"Yes", "No"});
        //paramInfo.setOnlyOneHourField(true);
        paramInfo.setEachEncounterField(true);
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[5]);
        paramInfo.setParamName("Plan");
        paramInfo.setParamDateType(RADIO_SELECT_TYPE);
        paramInfo.setConceptUUID("162169AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        //paramInfo.setOnlyOneHourField(true);
        paramInfo.setEachEncounterField(true);
        stringList.add(paramInfo);

       /* paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[5]);
        paramInfo.setParamName("Assessment");
        paramInfo.setParamDateType(INPUT_TXT_TYPE);
        paramInfo.setConceptUUID("67a050c1-35e5-451c-a4ab-fff9d57b0db1");
        //paramInfo.setOnlyOneHourField(true);
        paramInfo.setEachEncounterField(true);
        stringList.add(paramInfo);

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[5]);
        paramInfo.setParamName("Plan");
        paramInfo.setParamDateType(INPUT_TXT_TYPE);
        paramInfo.setConceptUUID("162169AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        //paramInfo.setOnlyOneHourField(true);
        paramInfo.setEachEncounterField(true);
        stringList.add(paramInfo);*/

        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName(SECTION_LIST[5]);
        paramInfo.setParamName("Supervisor Doctor");
        paramInfo.setParamDateType(INPUT_TXT_TYPE);
        paramInfo.setConceptUUID("7a9cb7bc-9ab9-4ff0-ae82-7a1bd2cca93e");
        stringList.add(paramInfo);
        //paramInfo.setOnlyOneHourField(true);
        paramInfo.setEachEncounterField(true);
        sectionParamInfoMap.put(SECTION_LIST[5], stringList);//Shared Decision Making

        //Initials
//        stringList = new ArrayList<>();
//        paramInfo = new ParamInfo();
//        paramInfo.setParamSectionName(SECTION_LIST[6]);
//        paramInfo.setParamName("Initial");
//        paramInfo.setParamDateType(INPUT_TXT_TYPE);
//        paramInfo.setConceptUUID("165171AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
//        stringList.add(paramInfo);
//
//        sectionParamInfoMap.put(SECTION_LIST[6], stringList);//Initials

        //Actions
        stringList = new ArrayList<>();
        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName("Action");
        paramInfo.setParamName("Birth Outcome");
        paramInfo.setParamDateType(INPUT_TXT_TYPE);
        paramInfo.setConceptUUID("23601d71-50e6-483f-968d-aeef3031346d");
        stringList.add(paramInfo);
        sectionParamInfoMap.put("Action", stringList);//Actions

        //for stage 3
        if (stage == STAGE_3) {
            return getStage3SectionMap();
        }

        return sectionParamInfoMap;
    }

    private static TreeMap<String, List<ParamInfo>> getStage3SectionMap() {

        TreeMap<String, List<ParamInfo>> sectionMap =
                new TreeMap<>((a, b) -> {

                    if (a.equals("Woman Monitoring")) return -1;
                    if (b.equals("Woman Monitoring")) return 1;

                    return a.compareTo(b);
                });
        // ─── SECTION 1: WOMAN MONITORING ───────────────────────────────────────────

        List<ParamInfo> womanList = new ArrayList<>();
        ParamInfo paramInfo;

        // Pulse
        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName("Woman Monitoring");
        paramInfo.setParamName(Params.PULSE.value);
        paramInfo.setParamDateType(INPUT_INT_3_DIG_TYPE);
        paramInfo.setConceptUUID(Params.PULSE.conceptId);
        paramInfo.setEachEncounterField(true);
        /*paramInfo.setFifteenMinField(true);
        paramInfo.setHalfHourField(true);
        paramInfo.setOnlyOneHourField(true);*/
        womanList.add(paramInfo);

        // Systolic BP
        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName("Woman Monitoring");
        paramInfo.setParamName(Params.SYSTOLIC_BP.value);
        paramInfo.setParamDateType(INPUT_INT_3_DIG_TYPE);
        paramInfo.setConceptUUID(Params.SYSTOLIC_BP.conceptId);
        paramInfo.setEachEncounterField(true);
        /*paramInfo.setFifteenMinField(true);
        paramInfo.setHalfHourField(true);
        paramInfo.setOnlyOneHourField(true);*/
        womanList.add(paramInfo);

        // Diastolic BP
        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName("Woman Monitoring");
        paramInfo.setParamName(Params.DIASTOLIC_BP.value);
        paramInfo.setParamDateType(INPUT_INT_3_DIG_TYPE);
        paramInfo.setConceptUUID(Params.DIASTOLIC_BP.conceptId);
        paramInfo.setEachEncounterField(true);
        /*paramInfo.setFifteenMinField(true);
        paramInfo.setHalfHourField(true);
        paramInfo.setOnlyOneHourField(true);*/
        womanList.add(paramInfo);

        // Temperature (°F)
        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName("Woman Monitoring");
        paramInfo.setParamName("Temperature (°F)");
        paramInfo.setParamDateType(INPUT_DOUBLE_4_DIG_TYPE);
        paramInfo.setConceptUUID(Params.TEMPERATURE.conceptId);
        paramInfo.setEachEncounterField(true);
        /*paramInfo.setFifteenMinField(true);
        paramInfo.setHalfHourField(true);
        paramInfo.setOnlyOneHourField(true);*/
        womanList.add(paramInfo);

        // Respiratory Rate
        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName("Woman Monitoring");
        paramInfo.setParamName(Params.RESPIRATORY_RATE_MOTHER.value);
        paramInfo.setParamDateType(INPUT_INT_3_DIG_TYPE);
        paramInfo.setConceptUUID(Params.RESPIRATORY_RATE_MOTHER.conceptId);
        paramInfo.setEachEncounterField(true);
        /*paramInfo.setFifteenMinField(true);
        paramInfo.setHalfHourField(true);
        paramInfo.setOnlyOneHourField(true);*/
        womanList.add(paramInfo);

        // Blood Loss
        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName("Woman Monitoring");
        paramInfo.setParamName(Params.BLOOD_LOSS_MOTHER.value);
        paramInfo.setParamDateType(INPUT_INT_4_DIG_TYPE);
        paramInfo.setConceptUUID(Params.BLOOD_LOSS_MOTHER.conceptId);
        paramInfo.setEachEncounterField(true);
        /*paramInfo.setFifteenMinField(true);
        paramInfo.setHalfHourField(true);
        paramInfo.setOnlyOneHourField(true);*/
        womanList.add(paramInfo);

        // Uterus Contracted
        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName("Woman Monitoring");
        paramInfo.setParamName(Params.UTERUS_CONTRACTED_MOTHER.value);
        paramInfo.setParamDateType(RADIO_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"Yes", "No"});
        paramInfo.setValues(new String[]{"Y", "N"});
        paramInfo.setConceptUUID(Params.UTERUS_CONTRACTED_MOTHER.conceptId);
        paramInfo.setEachEncounterField(true);
        /*paramInfo.setFifteenMinField(true);
        paramInfo.setHalfHourField(true);
        paramInfo.setOnlyOneHourField(true);*/
        womanList.add(paramInfo);

        // Urine Passed
        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName("Woman Monitoring");
        paramInfo.setParamName(Params.URINE_PASSED_MOTHER.value);
        paramInfo.setParamDateType(RADIO_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"Yes", "No"});
        paramInfo.setConceptUUID(Params.URINE_PASSED_MOTHER.conceptId);
        paramInfo.setEachEncounterField(true);
        /*paramInfo.setFifteenMinField(true);
        paramInfo.setHalfHourField(true);
        paramInfo.setOnlyOneHourField(true);*/
        womanList.add(paramInfo);

        // Hematoma
        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName("Woman Monitoring");
        paramInfo.setParamName(Params.HEMATOMA_MOTHER.value);
        paramInfo.setParamDateType(RADIO_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"Yes", "No"});
        paramInfo.setValues(new String[]{"Y", "N"});
        paramInfo.setConceptUUID(Params.HEMATOMA_MOTHER.conceptId);
        paramInfo.setEachEncounterField(true);
      /*  paramInfo.setFifteenMinField(true);
        paramInfo.setHalfHourField(true);
        paramInfo.setOnlyOneHourField(true);*/
        womanList.add(paramInfo);

        // Ongoing Complications (Mother)
        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName("Woman Monitoring");
        paramInfo.setParamName(Params.ONGOING_COMPLICATIONS_MOTHER.value);
        paramInfo.setParamDateType(RADIO_SELECT_TYPE);
        paramInfo.setOptions(new String[]{"Yes", "No"});
        paramInfo.setValues(new String[]{"Y", "N"});
      /*  paramInfo.setParamDateType(DROPDOWN_MULTI_SELECT_TYPE);
        paramInfo.setOptions(new String[]{
                "Vaginal bleeding (≥300 ml)",
                "Fever (Temp ≥99.5 °F)",
                "High blood pressure",
                "Moderate pallor",
                "Severe pallor",
                AppConstants.OTHER_OPTION
        });*/
        paramInfo.setConceptUUID(Params.ONGOING_COMPLICATIONS_MOTHER.conceptId);
        paramInfo.setEachEncounterField(true);
       /* paramInfo.setFifteenMinField(true);
        paramInfo.setHalfHourField(true);
        paramInfo.setOnlyOneHourField(true);*/
        womanList.add(paramInfo);

        // Assessment (Mother)
        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName("Woman Monitoring");
        paramInfo.setParamName(Params.ASSESSMENT_MOTHER.value);
        paramInfo.setParamDateType(INPUT_TXT_TYPE);
        paramInfo.setConceptUUID(Params.ASSESSMENT_MOTHER.conceptId);
        paramInfo.setEachEncounterField(true);
       /* paramInfo.setFifteenMinField(true);
        paramInfo.setHalfHourField(true);
        paramInfo.setOnlyOneHourField(true);*/
        womanList.add(paramInfo);

        // Plan (Mother)
        paramInfo = new ParamInfo();
        paramInfo.setParamSectionName("Woman Monitoring");
        paramInfo.setParamName(Params.PLAN_MOTHER.value);
        paramInfo.setParamDateType(INPUT_TXT_TYPE);
        paramInfo.setConceptUUID(Params.PLAN_MOTHER.conceptId);
        paramInfo.setEachEncounterField(true);
       /* paramInfo.setFifteenMinField(true);
        paramInfo.setHalfHourField(true);
        paramInfo.setOnlyOneHourField(true);*/
        womanList.add(paramInfo);

        sectionMap.put("Woman Monitoring", womanList);

        // ─── SECTION 2: NEWBORN MONITORING ─────────────────────────────────────────

        List<ParamInfo> newbornList = new ArrayList<>();
        ParamInfo newbornParam;

        String[] yesNo = {"Yes", "No"};

        // 1 Grunting
        newbornParam = new ParamInfo();
        newbornParam.setParamSectionName("Newborn Monitoring");
        newbornParam.setParamName(Params.GRUNTING_NEWBORN.value);
        newbornParam.setParamDateType(RADIO_SELECT_TYPE);
        newbornParam.setOptions(new String[]{"Yes", "No"});
        newbornParam.setValues(new String[]{"Y", "N"});
        newbornParam.setConceptUUID(Params.GRUNTING_NEWBORN.conceptId);
        newbornParam.setEachEncounterField(true);
       /* //newbornParam.setFifteenMinField(true);
        newbornParam.setHalfHourField(true);
        newbornParam.setOnlyOneHourField(true);*/
        newbornList.add(newbornParam);

        // 2 Chest Indrawing
        newbornParam = new ParamInfo();
        newbornParam.setParamSectionName("Newborn Monitoring");
        newbornParam.setParamName(Params.CHEST_INDRAWING_NEWBORN.value);
        newbornParam.setParamDateType(RADIO_SELECT_TYPE);
        newbornParam.setOptions(new String[]{"Yes", "No"});
        newbornParam.setValues(new String[]{"Y", "N"});
        newbornParam.setConceptUUID(Params.CHEST_INDRAWING_NEWBORN.conceptId);
        newbornParam.setEachEncounterField(true);
        /*//newbornParam.setFifteenMinField(true);
        newbornParam.setHalfHourField(true);
        newbornParam.setOnlyOneHourField(true);*/
        newbornList.add(newbornParam);

        //3 Fast Breathing
        newbornParam = new ParamInfo();
        newbornParam.setParamSectionName("Newborn Monitoring");
        newbornParam.setParamName(Params.FAST_BREATHING_NEWBORN.value);
        newbornParam.setParamDateType(RADIO_SELECT_TYPE);
        newbornParam.setOptions(new String[]{"Yes", "No"});
        newbornParam.setValues(new String[]{"Y", "N"});
        newbornParam.setConceptUUID(Params.FAST_BREATHING_NEWBORN.conceptId);
        newbornParam.setEachEncounterField(true);
       /* //newbornParam.setFifteenMinField(true);
        //newbornParam.setHalfHourField(true);
        //newbornParam.setOnlyOneHourField(true);*/
        newbornList.add(newbornParam);

        //4 Respiratory Rate (per min)
        newbornParam = new ParamInfo();
        newbornParam.setParamSectionName("Newborn Monitoring");
        newbornParam.setParamName(Params.RESPIRATORY_RATE_NEWBORN.value);
        newbornParam.setParamDateType(INPUT_INT_3_DIG_TYPE);
        newbornParam.setConceptUUID(Params.RESPIRATORY_RATE_NEWBORN.conceptId);
        newbornParam.setEachEncounterField(true);
        /*//newbornParam.setFifteenMinField(true);
        newbornParam.setHalfHourField(true);
        newbornParam.setOnlyOneHourField(true);*/
        newbornList.add(newbornParam);

        //5 SPO2 (%)

        newbornParam = new ParamInfo();
        newbornParam.setParamSectionName("Newborn Monitoring");
        newbornParam.setParamName(Params.SPO2_NEWBORN.value);
        newbornParam.setParamDateType(INPUT_INT_3_DIG_TYPE);
        newbornParam.setConceptUUID(Params.SPO2_NEWBORN.conceptId);
        newbornParam.setEachEncounterField(true);
        /*//newbornParam.setFifteenMinField(true);
        newbornParam.setHalfHourField(true);
        newbornParam.setOnlyOneHourField(true);*/
        newbornList.add(newbornParam);

        //6 Feet Warm
        newbornParam = new ParamInfo();
        newbornParam.setParamSectionName("Newborn Monitoring");
        newbornParam.setParamName(Params.FEET_WARM_NEWBORN.value);
        newbornParam.setParamDateType(RADIO_SELECT_TYPE);
        newbornParam.setOptions(new String[]{"Yes", "No"});
        newbornParam.setValues(new String[]{"Y", "N"});
        newbornParam.setConceptUUID(Params.FEET_WARM_NEWBORN.conceptId);
        newbornParam.setEachEncounterField(true);
        /*//newbornParam.setFifteenMinField(true);
        newbornParam.setHalfHourField(true);
        newbornParam.setOnlyOneHourField(true);*/
        newbornList.add(newbornParam);

        // 7 Temperature (°F)
        newbornParam = new ParamInfo();
        newbornParam.setParamSectionName("Newborn Monitoring");
        newbornParam.setParamName(Params.TEMPERATURE_NEWBORN.value);
        newbornParam.setParamDateType(INPUT_DOUBLE_4_DIG_TYPE);
        newbornParam.setConceptUUID(Params.TEMPERATURE_NEWBORN.conceptId);
        newbornParam.setEachEncounterField(true);
       /* //newbornParam.setFifteenMinField(true);
        newbornParam.setHalfHourField(true);
        newbornParam.setOnlyOneHourField(true);*/
        newbornList.add(newbornParam);

        //8 Colour of the skin (Cyanosed)
        newbornParam = new ParamInfo();
        newbornParam.setParamSectionName("Newborn Monitoring");
        newbornParam.setParamName(Params.SKIN_COLOR_NEWBORN.value);
        newbornParam.setParamDateType(RADIO_SELECT_TYPE);
        newbornParam.setOptions(new String[]{"Yes", "No"});
        newbornParam.setValues(new String[]{"Y", "N"});
        newbornParam.setConceptUUID(Params.SKIN_COLOR_NEWBORN.conceptId);
        newbornParam.setEachEncounterField(true);
       /* //newbornParam.setFifteenMinField(true);
        newbornParam.setHalfHourField(true);
        newbornParam.setOnlyOneHourField(true);*/
        newbornList.add(newbornParam);

        //9 Umbilical Cord Oozing
        newbornParam = new ParamInfo();
        newbornParam.setParamSectionName("Newborn Monitoring");
        newbornParam.setParamName(Params.UC_OOZING_NEWBORN.value);
        newbornParam.setParamDateType(RADIO_SELECT_TYPE);
        newbornParam.setOptions(new String[]{"Yes", "No"});
        newbornParam.setValues(new String[]{"Y", "N"});
        newbornParam.setConceptUUID(Params.UC_OOZING_NEWBORN.conceptId);
        newbornParam.setEachEncounterField(true);
      /*  //newbornParam.setFifteenMinField(true);
        newbornParam.setHalfHourField(true);
        newbornParam.setOnlyOneHourField(true);*/
        newbornList.add(newbornParam);

        //10 Suckling & Feeding
        newbornParam = new ParamInfo();
        newbornParam.setParamSectionName("Newborn Monitoring");
        newbornParam.setParamName(Params.SUCKING_FEEDING_NEWBORN.value);
        newbornParam.setParamDateType(DROPDOWN_SINGLE_SELECT_TYPE);
        newbornParam.setOptions(new String[]{"Not yet breastfed", "Yes", "No"});
        newbornParam.setValues(new String[]{"Not yet breastfed", "Y", "N"});
        newbornParam.setConceptUUID(Params.SUCKING_FEEDING_NEWBORN.conceptId);
        newbornParam.setEachEncounterField(true);
        /*//newbornParam.setFifteenMinField(true);
        newbornParam.setHalfHourField(true);
        newbornParam.setOnlyOneHourField(true);*/
        newbornList.add(newbornParam);

        //11 Any signs of ongoing complications
        newbornParam = new ParamInfo();
        newbornParam.setParamSectionName("Newborn Monitoring");
        newbornParam.setParamName(Params.ONGOING_COMPLICATIONS_NEWBORN.value);
        newbornParam.setParamDateType(RADIO_SELECT_TYPE);
        newbornParam.setOptions(new String[]{"Yes", "No"});
        newbornParam.setValues(new String[]{"Y", "N"});
         /*newbornParam.setParamDateType(DROPDOWN_MULTI_SELECT_TYPE);
        newbornParam.setOptions(new String[]{
                "Respiratory distress",
                "Hypothermia",
                "Sepsis",
                "Poor feeding",
                AppConstants.OTHER_OPTION
        });*/
        newbornParam.setConceptUUID(Params.ONGOING_COMPLICATIONS_NEWBORN.conceptId);
        newbornParam.setEachEncounterField(true);
       /* newbornParam.setFifteenMinField(true);
        newbornParam.setHalfHourField(true);
        newbornParam.setOnlyOneHourField(true);*/
        newbornList.add(newbornParam);

        //12 Assessment (Newborn)
        newbornParam = new ParamInfo();
        newbornParam.setParamSectionName("Newborn Monitoring");
        newbornParam.setParamName(Params.ASSESSMENT_NEWBORN.value);
        newbornParam.setParamDateType(INPUT_TXT_TYPE);
        newbornParam.setConceptUUID(Params.ASSESSMENT_NEWBORN.conceptId);
        newbornParam.setEachEncounterField(true);
       /* //newbornParam.setFifteenMinField(true);
        newbornParam.setHalfHourField(true);
        newbornParam.setOnlyOneHourField(true);*/
        newbornList.add(newbornParam);

        //13 Plan (Newborn)
        newbornParam = new ParamInfo();
        newbornParam.setParamSectionName("Newborn Monitoring");
        newbornParam.setParamName(Params.PLAN_NEWBORN.value);
        newbornParam.setParamDateType(INPUT_TXT_TYPE);
        newbornParam.setConceptUUID(Params.PLAN_NEWBORN.conceptId);
        newbornParam.setEachEncounterField(true);
        //newbornParam.setFifteenMinField(true);
        // newbornParam.setHalfHourField(true);
        //newbornParam.setOnlyOneHourField(true);
        newbornList.add(newbornParam);


        sectionMap.put("Newborn Monitoring", newbornList);

        return sectionMap;
    }

}
