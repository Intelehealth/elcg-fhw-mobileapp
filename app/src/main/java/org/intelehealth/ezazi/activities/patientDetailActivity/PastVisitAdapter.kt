package org.intelehealth.ezazi.activities.patientDetailActivity

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.databinding.ItemPastVisitBinding

/**
 * Past visit cards. Every row shows an em dash when it has no value rather than hiding, so the
 * card's shape is the same for a delivery, a referral and a self-discharge.
 */
class PastVisitAdapter(
    private val items: List<PastVisitDetails>,
    private val reportListener: OnReportClickListener
) : RecyclerView.Adapter<PastVisitAdapter.PastVisitViewHolder>() {

    interface OnReportClickListener {
        fun onReportClick(details: PastVisitDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PastVisitViewHolder(
        ItemPastVisitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: PastVisitViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class PastVisitViewHolder(private val binding: ItemPastVisitBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(details: PastVisitDetails) {
            binding.tvPvAdmissionDate.text = orDash(details.admissionDate)
            binding.tvPvBed.text = orDash(details.bedNumber)
            binding.tvPvActiveLabour.text = orDash(details.activeLabourDiagnosed)
            binding.tvPvDelivery.text = orDash(details.deliveryDate)
            binding.tvPvRisk.text = orDash(details.riskFactors)
            binding.tvPvParity.text = orDash(details.parity)
            binding.tvPvMode.text = orDash(details.modeOfDelivery)
            binding.tvPvBaby.text = orDash(details.babyStatus)
            binding.tvPvMother.text = orDash(details.motherStatus)
            bindReport(details)

            applyExpansion(details.expanded)
            binding.llPvHeader.setOnClickListener {
                details.expanded = !details.expanded
                applyExpansion(details.expanded)
            }
        }

        /** A visit with no Stage 3 report keeps the row and shows a dash, never a dead link. */
        private fun bindReport(details: PastVisitDetails) {
            val report = binding.tvPvReport
            if (details.hasReport) {
                report.text = report.context.getString(R.string.pv_view_report)
                report.paintFlags = report.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                report.setTextColor(ContextCompat.getColor(report.context, R.color.colorPrimary))
                report.setOnClickListener { reportListener.onReportClick(details) }
            } else {
                report.text = DASH
                report.paintFlags = report.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
                report.setTextColor(ContextCompat.getColor(report.context, R.color.font_black_0))
                report.setOnClickListener(null)
                report.isClickable = false
            }
        }

        private fun applyExpansion(expanded: Boolean) {
            binding.tlPvBody.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.ivPvChevron.setImageResource(
                if (expanded) R.drawable.ic_past_visit_chevron_up else R.drawable.ic_past_visit_chevron
            )
        }
    }

    companion object {
        private const val DASH = "—"

        private fun orDash(value: String?) = if (value.isNullOrBlank()) DASH else value
    }
}
