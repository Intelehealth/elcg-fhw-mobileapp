package org.intelehealth.ezazi.activities.patientDetailActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.intelehealth.ezazi.R;

import java.util.List;

/**
 * Renders the patient's past (closed) visits as read-only Past Visit Details cards.
 */
public class PastVisitAdapter extends RecyclerView.Adapter<PastVisitAdapter.PastVisitViewHolder> {

    private final List<PastVisitDetails> items;

    public PastVisitAdapter(List<PastVisitDetails> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public PastVisitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_past_visit_details, parent, false);
        return new PastVisitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PastVisitViewHolder holder, int position) {
        PastVisitDetails details = items.get(position);
        holder.visitDate.setText(details.visitDate);
        holder.activeLabour.setText(orDash(details.activeLabourDiagnosed));
        holder.delivery.setText(orDash(details.deliveryDate));
        holder.risk.setText(orDash(details.riskFactors));
        holder.parity.setText(orDash(details.parity));
        holder.mode.setText(orDash(details.modeOfDelivery));
        holder.baby.setText(orDash(details.babyStatus));
        holder.mother.setText(orDash(details.motherStatus));
        holder.report.setText(orDash(details.finalOutcomeReport));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String orDash(String value) {
        return (value == null || value.trim().isEmpty()) ? "—" : value;
    }

    static class PastVisitViewHolder extends RecyclerView.ViewHolder {
        final TextView visitDate, activeLabour, delivery, risk, parity, mode, baby, mother, report;

        PastVisitViewHolder(View view) {
            super(view);
            visitDate = view.findViewById(R.id.tv_pv_visit_date);
            activeLabour = view.findViewById(R.id.tv_pv_active_labour);
            delivery = view.findViewById(R.id.tv_pv_delivery);
            risk = view.findViewById(R.id.tv_pv_risk);
            parity = view.findViewById(R.id.tv_pv_parity);
            mode = view.findViewById(R.id.tv_pv_mode);
            baby = view.findViewById(R.id.tv_pv_baby);
            mother = view.findViewById(R.id.tv_pv_mother);
            report = view.findViewById(R.id.tv_pv_report);
        }
    }
}
