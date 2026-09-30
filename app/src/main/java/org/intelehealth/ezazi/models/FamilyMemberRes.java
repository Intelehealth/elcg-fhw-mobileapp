package org.intelehealth.ezazi.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.intelehealth.ezazi.ui.dialog.model.MultiChoiceItem;

import java.io.Serializable;

public class FamilyMemberRes implements MultiChoiceItem, Serializable {

    @SerializedName("openMRSID")
    private String openMRSID;
    @SerializedName("name")
    private String name;

    // Added by Mithun Vaghela
    private String visitUuid;

    private String bedNo;

    public String getOpenMRSID() {
        return openMRSID;
    }

    public void setOpenMRSID(String openMRSID) {
        this.openMRSID = openMRSID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVisitUuid() {
        return visitUuid;
    }

    public void setVisitUuid(String visitUuid) {
        this.visitUuid = visitUuid;
    }

    @Override
    public boolean isHeader() {
        return false;
    }

    public void setBedNo(String bedNo) {
        this.bedNo = bedNo;
    }

    /** Never null: a missing bed reads NA, which is what the write side stores for a blank one. */
    public String getBedNo() {
        return (bedNo == null || bedNo.trim().isEmpty())
                ? org.intelehealth.ezazi.app.AppConstants.NOT_APPLICABLE : bedNo;
    }
}
