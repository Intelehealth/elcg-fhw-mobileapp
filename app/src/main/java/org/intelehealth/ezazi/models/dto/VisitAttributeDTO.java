package org.intelehealth.ezazi.models.dto;

import java.util.UUID;

public class VisitAttributeDTO {
    private String uuid;
    private String visit_uuid;
    private String value;
    private String visit_attribute_type_uuid;
    private int voided;

    private String sync = "false";

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVisitUuid() {
        return visit_uuid;
    }

    public void setVisit_uuid(String visit_uuid) {
        this.visit_uuid = visit_uuid;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getVisitAttributeTypeUuid() {
        return visit_attribute_type_uuid;
    }

    public void setVisit_attribute_type_uuid(String visit_attribute_type_uuid) {
        this.visit_attribute_type_uuid = visit_attribute_type_uuid;
    }

    public int getVoided() {
        return voided;
    }

    public void setVoided(int voided) {
        this.voided = voided;
    }

    public void setSync(String sync) {
        this.sync = sync;
    }

    public String getSync() {
        return sync;
    }

    public static VisitAttributeDTO generateNew(String visitId, String value, String typeId) {
        VisitAttributeDTO attribute = new VisitAttributeDTO();
        attribute.setUuid(UUID.randomUUID().toString());
        attribute.setVoided(0);
        attribute.setVisit_attribute_type_uuid(typeId);
        attribute.setValue(value);
        attribute.setVisit_uuid(visitId);
        attribute.setSync("0");
        return attribute;
    }

    /**
     * Visit-attribute type UUIDs for the obstetric intake collected in the visit flow.
     * These are fixed server-defined ids (there is no local visit-attribute-type master).
     */
    public enum Columns {
        GRAVIDA("d970452f-eb7a-47c7-b011-ba9af33b3f9d"),
        ADMISSION_DATE("5a30d1f7-75b2-4bac-87c2-7da0aab70a88"),
        ADMISSION_TIME("b05023c9-00d0-465d-b665-d62833689f75"),
        PARITY("cc09f7b9-fcb4-4b5f-8e71-c7c0e0e4e0f4"),
        LABOR_ONSET("8b54e92e-2213-4b3e-8dd3-a8bc57141e25"),
        ACTIVE_LABOR_DIAGNOSED("43069207-3e32-4e18-920f-2a33e7a83fda"),
        MEMBRANE_RUPTURED_TIMESTAMP("fad90749-ed23-4efb-b6ed-f1b68a60ba2a"),
        RISK_FACTORS("2b09cbf9-dbba-4b4e-a442-411841dc1fd3"),
        HOSPITAL_MATERNITY("0d2aac3a-4358-4987-b653-85536d83d723"),
        PRIMARY_DOCTOR("f62de70b-15ae-42e7-b653-2c7f23cd2317"),
        SECONDARY_DOCTOR("5ae1ae98-6733-4c0d-b1bc-398ae5fea0ac"),
        BED_NUMBER("ca48020c-e459-4523-8a89-72c6e2db713f"),
        LAST_MENSTRUAL_PERIOD("8ef57af0-9ca2-44e9-b4b3-f990fe28c456"),
        ESTIMATED_DELIVERY_DATE("ebe6635d-8bae-4f3c-b40a-f9dddb7ebe49"),
        HOSPITAL_ID("88a3aff6-93a7-4772-916e-e42d86840770");

        public final String uuid;

        Columns(String uuid) {
            this.uuid = uuid;
        }
    }
}
