package org.intelehealth.ezazi.activities.visitCreation;

import java.io.Serializable;

/**
 * In-memory holder for the obstetric intake collected on the visit-creation screen.
 * Persisting these values as visit attributes and creating the visit is handled
 * separately in a later step of the visit flow.
 */
public class ObstetricVisitData implements Serializable {
    public String gravida = "";
    public String admissionDate = "";
    public String admissionTime = "";
    public String totalBirths = "";
    public String totalMiscarriages = "";
    public String laborOnset = "";
    public String lastMenstrualPeriod = "";
    public String estimatedDeliveryDate = "";
    public String activeLaborDiagnosedDate = "";
    public String activeLaborDiagnosedTime = "";
    public String membraneRupturedDate = "";
    public String membraneRupturedTime = "";
    public boolean membraneRupturedUnknown = false;
    public String riskFactors = "";
    public String hospitalMaternity = "";
    public String hospitalOther = "";
    public String hospitalId = "";
    public String primaryDoctorUuid = "";
    public String primaryDoctorName = "";
    public String secondaryDoctorUuid = "";
    public String secondaryDoctorName = "";
    public String bedNumber = "";
}
