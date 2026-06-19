package org.intelehealth.ezazi.ui.prescription.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.databinding.ItemLcgAlertBinding
import org.intelehealth.ezazi.ui.prescription.model.LcgAlertItem

class LcgAlertsAdapter : ListAdapter<LcgAlertItem, LcgAlertsAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLcgAlertBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemLcgAlertBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LcgAlertItem) {
            binding.tvParamName.text = item.parameterName
            // e.g. "168 bpm"
            binding.tvCurrentValue.text = item.currentValueFormatted
            // e.g. "LCG alert value:\n<110 or >160 bpm"
            binding.tvAlertMessage.text = binding.root.context.getString(R.string.lcg_alert_ref_format, item.alertThresholdFormatted)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<LcgAlertItem>() {
            override fun areItemsTheSame(old: LcgAlertItem, new: LcgAlertItem) =
                old.conceptId == new.conceptId
            override fun areContentsTheSame(old: LcgAlertItem, new: LcgAlertItem) =
                old == new
        }
    }
}