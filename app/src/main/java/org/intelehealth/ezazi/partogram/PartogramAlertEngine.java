package org.intelehealth.ezazi.partogram;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import org.intelehealth.ezazi.partogram.model.ParamInfo;

public class PartogramAlertEngine {
    public static String getAlertName(ParamInfo paramInfo) {
        if (paramInfo.getCapturedValue() == null || paramInfo.getCapturedValue().isEmpty()) {
            return "";
        }
        String alert = "G";
        if (paramInfo.getParamSectionName().equalsIgnoreCase("Supportive care")) {
            if (paramInfo.getParamName().equalsIgnoreCase("Companion")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("Y") || paramInfo.getCapturedValue().equalsIgnoreCase("D")) {
                    alert = "G";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("N")) {
                    alert = "Y";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Pain relief")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("Y")) {
                    alert = "G";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("N")) {
                    alert = "Y";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Oral fluid")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("Y")) {
                    alert = "G";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("N")) {
                    alert = "R";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Posture")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("MO")) {
                    alert = "G";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("SP")) {
                    alert = "Y";
                }
            }
        } else if (paramInfo.getParamSectionName().equalsIgnoreCase("BABY")) {

            if (paramInfo.getParamName().equalsIgnoreCase("Baseline FHR")) {
                int val = Integer.parseInt(paramInfo.getCapturedValue());
                if (val >= 160) {
                    alert = "R";
                } else if (val < 110) {
                    alert = "R";
                } else {
                    alert = "G";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("FHR deceleration")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("N") || paramInfo.getCapturedValue().equalsIgnoreCase("E")) {
                    alert = "G";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("V")) {
                    alert = "Y";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("L")) {
                    alert = "R";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Amniotic fluid meconium")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("I") || paramInfo.getCapturedValue().equalsIgnoreCase("C")) {
                    alert = "G";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("M+") || paramInfo.getCapturedValue().equalsIgnoreCase("M++")) {
                    alert = "Y";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("M+++") || paramInfo.getCapturedValue().equalsIgnoreCase("B")) {
                    alert = "R";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Fetal position")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("A")) {
                    alert = "G";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("P") || paramInfo.getCapturedValue().equalsIgnoreCase("T")) {
                    alert = "R";
                }
//                else if (paramInfo.getCapturedValue().equalsIgnoreCase("M+++") || paramInfo.getCapturedValue().equalsIgnoreCase("B")) {
//                    alert = "R";
//                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Caput")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("N") || paramInfo.getCapturedValue().equalsIgnoreCase("+")) {
                    alert = "G";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("++")) {
                    alert = "Y";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("+++")) {
                    alert = "R";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Moulding")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("N")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("+")) {
                    alert = "G";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("++")) {
                    alert = "Y";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("+++")) {
                    alert = "R";
                }
            }
        } else if (paramInfo.getParamSectionName().equalsIgnoreCase("Woman")) {
            if (paramInfo.getParamName().equalsIgnoreCase("Pulse")) {
                int val = Integer.parseInt(paramInfo.getCapturedValue());
                if (val < 60) {
                    alert = "R";
                } else if (val >= 120) {
                    alert = "R";
                } else {
                    alert = "G";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Systolic BP")) {
                int val = Integer.parseInt(paramInfo.getCapturedValue());
                if (val < 80 || val >= 140) {
                    alert = "R";
                } else {
                    alert = "G";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Diastolic BP")) {
                int val = Integer.parseInt(paramInfo.getCapturedValue());
                if (val >= 90) {
                    alert = "R";
                } else {
                    alert = "G";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Temperature(C)")) {
                double val = Double.parseDouble(paramInfo.getCapturedValue());
                if (val < 35) {
                    alert = "R";
                } else if (val >= 37.5) {
                    alert = "R";
                } else {
                    alert = "G";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Urine protein")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("P3+")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("P4+")) {
                    alert = "R";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("P2+")) {
                    alert = "R";
                } else alert = "G";
//                if (paramInfo.getCapturedValue().equalsIgnoreCase("P-")
//                        || paramInfo.getCapturedValue().equalsIgnoreCase("P")
//                        || paramInfo.getCapturedValue().equalsIgnoreCase("P1")) {
//                    alert = "G";
//                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("P2+")) {
//                    alert = "Y";
//                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("P3+")
//                        || paramInfo.getCapturedValue().equalsIgnoreCase("P4+")) {
//                    alert = "R";
//                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Urine Acetone")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("A3+")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("A4+")) {
                    alert = "R";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("A2+")) {
                    alert = "R";
                } else alert = "G";
//                if (paramInfo.getCapturedValue().equalsIgnoreCase("A-") || paramInfo.getCapturedValue().equalsIgnoreCase("A")
//                        || paramInfo.getCapturedValue().equalsIgnoreCase("A1")) {
//                    alert = "G";
//                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("A2")) {
//                    alert = "Y";
//                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("A3")) {
//                    alert = "R";
//                }
            }
        } else if (paramInfo.getParamSectionName().equalsIgnoreCase("Labour Progress")) {
            if (paramInfo.getParamName().equalsIgnoreCase("Contractions per 10 min")) {
                int val = Integer.parseInt(paramInfo.getCapturedValue());
                if (val <= 2 || val > 5) {
                    alert = "R";
                } else {
                    alert = "G";
                }
                /*if (val > 5) {
                    alert = "R";
                } else if (val == 1 || val == 2) {
                    alert = "Y";
                } else {
                    alert = "G";
                }*/
            } else if (paramInfo.getParamName().equalsIgnoreCase("Duration of contractions")) {
                int val = Integer.parseInt(paramInfo.getCapturedValue());
                if (val > 60) {
                    alert = "R";
                } else if (val < 20) {
                    alert = "R";
                } else {
                    alert = "G";
                }
            }
        } else if (paramInfo.getParamSectionName().equalsIgnoreCase("Medication")) {

        } else if (paramInfo.getParamSectionName().equalsIgnoreCase("Shared Decision Making")) {

        } else if (paramInfo.getParamSectionName().equalsIgnoreCase("Initials")) {

        }
        return alert;
    }


    //ticket: EZ-753
    public static String getAlertNameUpdated(ParamInfo paramInfo) {

        if (paramInfo.getCapturedValue() == null || paramInfo.getCapturedValue().isEmpty()) {
            return "";
        }
        String alert = "G";
        if (paramInfo.getParamSectionName().equalsIgnoreCase("Supportive care")) {
            if (paramInfo.getParamName().equalsIgnoreCase("Companion")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("Y")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("D")) {
                    alert = "G";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("N")) {
                    alert = "R";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Pain relief")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("Y")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("D")) {
                    alert = "G";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("N")) {
                    alert = "R";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Oral fluid")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("Y")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("D")) {
                    alert = "G";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("N")) {
                    alert = "R";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Posture")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("MO")) {
                    alert = "G";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("SP")) {
                    alert = "R";
                }
            }
        } else if (paramInfo.getParamSectionName().equalsIgnoreCase("BABY")) {

            if (paramInfo.getParamName().equalsIgnoreCase("Baseline FHR")) {
                int val = Integer.parseInt(paramInfo.getCapturedValue());
                if (val >= 160) {
                    alert = "R";
                } else if (val < 110) {
                    alert = "R";
                } else {
                    alert = "G";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("FHR deceleration")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("N")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("E")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("V")) {
                    alert = "G";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("L")) {
                    alert = "R";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Amniotic fluid meconium")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("I")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("C")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("M+")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("M++")) {
                    alert = "G";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("M+++")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("B")) {
                    alert = "R";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Fetal position")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("A")) {
                    alert = "G";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("P")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("T")) {
                    alert = "R";
                }
//                else if (paramInfo.getCapturedValue().equalsIgnoreCase("M+++") || paramInfo.getCapturedValue().equalsIgnoreCase("B")) {
//                    alert = "R";
//                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Caput")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("N")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("+")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("++")) {
                    alert = "G";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("+++")) {
                    alert = "R";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Moulding")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("N")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("+")
                || paramInfo.getCapturedValue().equalsIgnoreCase("++")) {
                    alert = "G";
                }else if (paramInfo.getCapturedValue().equalsIgnoreCase("+++")) {
                    alert = "R";
                }
            }
        } else if (paramInfo.getParamSectionName().equalsIgnoreCase("Woman")) {
            if (paramInfo.getParamName().equalsIgnoreCase("Pulse")) {
                int val = Integer.parseInt(paramInfo.getCapturedValue());
                if (val < 60) {
                    alert = "R";
                } else if (val >= 120) {
                    alert = "R";
                } else {
                    alert = "G";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Systolic BP")) {
                int val = Integer.parseInt(paramInfo.getCapturedValue());
                if (val < 80 || val >= 140) {
                    alert = "R";
                } else {
                    alert = "G";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Diastolic BP")) {
                int val = Integer.parseInt(paramInfo.getCapturedValue());
                if (val >= 90) {
                    alert = "R";
                } else {
                    alert = "G";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Temperature(C)")) {
                double val = Double.parseDouble(paramInfo.getCapturedValue());
                if (val < 35) {
                    alert = "R";
                } else if (val >= 37.5) {
                    alert = "R";
                } else {
                    alert = "G";
                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Urine protein")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("P3+")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("P4+")) {
                    alert = "R";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("P2+")) {
                    alert = "R";
                } else alert = "G";
//                if (paramInfo.getCapturedValue().equalsIgnoreCase("P-")
//                        || paramInfo.getCapturedValue().equalsIgnoreCase("P")
//                        || paramInfo.getCapturedValue().equalsIgnoreCase("P1")) {
//                    alert = "G";
//                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("P2+")) {
//                    alert = "Y";
//                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("P3+")
//                        || paramInfo.getCapturedValue().equalsIgnoreCase("P4+")) {
//                    alert = "R";
//                }
            } else if (paramInfo.getParamName().equalsIgnoreCase("Urine Acetone")) {
                if (paramInfo.getCapturedValue().equalsIgnoreCase("A3+")
                        || paramInfo.getCapturedValue().equalsIgnoreCase("A4+")) {
                    alert = "R";
                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("A2+")) {
                    alert = "R";
                } else alert = "G";
//                if (paramInfo.getCapturedValue().equalsIgnoreCase("A-") || paramInfo.getCapturedValue().equalsIgnoreCase("A")
//                        || paramInfo.getCapturedValue().equalsIgnoreCase("A1")) {
//                    alert = "G";
//                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("A2")) {
//                    alert = "Y";
//                } else if (paramInfo.getCapturedValue().equalsIgnoreCase("A3")) {
//                    alert = "R";
//                }
            }
        } else if (paramInfo.getParamSectionName().equalsIgnoreCase("Labour Progress")) {
            if (paramInfo.getParamName().equalsIgnoreCase("Contractions per 10 min")) {
                int val = Integer.parseInt(paramInfo.getCapturedValue());
                if (val <= 2 || val > 5) {
                    alert = "R";
                } else {
                    alert = "G";
                }
                /*if (val > 5) {
                    alert = "R";
                } else if (val == 1 || val == 2) {
                    alert = "Y";
                } else {
                    alert = "G";
                }*/
            } else if (paramInfo.getParamName().equalsIgnoreCase("Duration of contractions")) {
                int val = Integer.parseInt(paramInfo.getCapturedValue());
                if (val > 60) {
                    alert = "R";
                } else if (val < 20) {
                    alert = "R";
                } else {
                    alert = "G";
                }
            }
        } else if (paramInfo.getParamSectionName().equalsIgnoreCase("Medication")) {

        } else if (paramInfo.getParamSectionName().equalsIgnoreCase("Shared Decision Making")) {

        } else if (paramInfo.getParamSectionName().equalsIgnoreCase("Initials")) {

        }
        return alert;
    }
    public static String getStage3AlertName(ParamInfo paramInfo) {
        Log.d("kaveri", "getStage3AlertName:paramInfo :  "+new Gson().toJson(paramInfo));
        String alert = "G";
        try {
            // EARLY POSTPARTUM – WOMAN
            if (paramInfo.getParamSectionName().equalsIgnoreCase("Woman Monitoring")) {

                if (paramInfo.getParamName().equalsIgnoreCase("Pulse")&& isValidCapturedValue(paramInfo.getCapturedValue())) {

                    int val = Integer.parseInt(paramInfo.getCapturedValue());
                    if (val < 60 || val >= 120) alert = "R";


                } else if (paramInfo.getParamName().equalsIgnoreCase("Systolic BP") && isValidCapturedValue(paramInfo.getCapturedValue())) {

                    int val = Integer.parseInt(paramInfo.getCapturedValue());
                    if (val < 80 || val >= 140) alert = "R";

                } else if (paramInfo.getParamName().equalsIgnoreCase("Diastolic BP")) {

                    int val = Integer.parseInt(paramInfo.getCapturedValue());
                    if (val >= 90) alert = "R";

                } else if (paramInfo.getParamName().equalsIgnoreCase("Temperature (°F)")) {

                    double val = Double.parseDouble(paramInfo.getCapturedValue());
                    if (val < 95 || val >= 99.5) alert = "R";

                } else if (paramInfo.getParamName().equalsIgnoreCase("Respiratory Rate (per min)")) {

                    int val = Integer.parseInt(paramInfo.getCapturedValue());
                    if (val > 30) alert = "R";

                } else if (paramInfo.getParamName().equalsIgnoreCase("Blood loss (ml)")) {

                    int val = Integer.parseInt(paramInfo.getCapturedValue());
                    if (val >= 500) alert = "R";

                } else if (paramInfo.getParamName().equalsIgnoreCase("Uterus contracted")) {

                    if (paramInfo.getCapturedValue().equalsIgnoreCase("N")) alert = "R";

                } else if (paramInfo.getParamName().equalsIgnoreCase("Urine passed in 2 hours")) {

                    if (paramInfo.getCapturedValue().equalsIgnoreCase("N")) alert = "R";

                } else if (paramInfo.getParamName().equalsIgnoreCase("Hematoma")) {

                    if (paramInfo.getCapturedValue().equalsIgnoreCase("Y")) alert = "R";
                } else if (paramInfo.getParamName().equalsIgnoreCase("Any signs of ongoing \ncomplications")) {

                    if (paramInfo.getCapturedValue() !=null && !paramInfo.getCapturedValue().isEmpty() && isOngoingComplicationYes(paramInfo.getCapturedValue())) alert = "R";
                }
            }

            // EARLY POSTPARTUM – NEWBORN
            else if (paramInfo.getParamSectionName().equalsIgnoreCase("Newborn Monitoring")) {

               if (paramInfo.getParamName().equalsIgnoreCase("Grunting")
                        || paramInfo.getParamName().equalsIgnoreCase("Chest indrawing")
                        || paramInfo.getParamName().equalsIgnoreCase("Fast breathing")
                        || paramInfo.getParamName().equalsIgnoreCase("Colour of the skin (Cyanosed)")
                        || paramInfo.getParamName().equalsIgnoreCase("Umbilical cord oozing")) {

                    if (paramInfo.getCapturedValue().equalsIgnoreCase("Y")) {
                        alert = "R";
                    }

                } else if (paramInfo.getParamName().equalsIgnoreCase("Respiratory Rate (per min)")) {

                    int val = Integer.parseInt(paramInfo.getCapturedValue());
                    if (val > 60) alert = "R";

                } else if (paramInfo.getParamName().equalsIgnoreCase("SPO2 (%)")) {

                    int val = Integer.parseInt(paramInfo.getCapturedValue());
                    if (val < 92) alert = "R";

                } else if (paramInfo.getParamName().equalsIgnoreCase("Feet (warm)")) {

                    if (paramInfo.getCapturedValue().equalsIgnoreCase("N")) alert = "R";

                } else if (paramInfo.getParamName().equalsIgnoreCase("Temperature (°F)")) {

                    double val = Double.parseDouble(paramInfo.getCapturedValue());
                    if (val < 97.7 || val > 99.5) alert = "R";

                } else if (paramInfo.getParamName().equalsIgnoreCase("Suckling & Feeding")) {

                    if (paramInfo.getCapturedValue().equalsIgnoreCase("N")) alert = "R";
                }else if (paramInfo.getParamName().equalsIgnoreCase("Any signs of ongoing \ncomplications")) {

                    if (paramInfo.getCapturedValue() !=null && !paramInfo.getCapturedValue().isEmpty() && isOngoingComplicationYes(paramInfo.getCapturedValue())) alert = "R";
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return alert;
    }
    public static boolean isValidCapturedValue(String capturedValue) {
        return capturedValue != null && !capturedValue.trim().isEmpty();
    }
    private static boolean isOngoingComplicationYes(String value) {
        if (TextUtils.isEmpty(value)) return false;
        try {
            org.json.JSONObject json = new org.json.JSONObject(value);
            String yesNo = json.optString("any ongoing complication", "");

            return "yes".equalsIgnoreCase(yesNo.trim());

        } catch (org.json.JSONException e) {
            return false;
        }
    }
}
