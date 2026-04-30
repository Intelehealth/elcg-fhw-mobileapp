package org.intelehealth.ezazi.stage3.Utils

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.databinding.SelectAllDialogItemHeaderBinding

class GenericMultiChoiceViewHolder(
    private val binding: SelectAllDialogItemHeaderBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(text: String) {
        binding.setHeader(text)
        binding.cbSelectAll.layoutDirection = View.LAYOUT_DIRECTION_LTR
    }

    fun setClickListener(listener: View.OnClickListener) {
        binding.cbSelectAll.tag = binding.cbSelectAll
        binding.cbSelectAll.setTag(R.id.cbSelectAll, adapterPosition)
        binding.cbSelectAll.setOnClickListener(listener)
    }

    fun setCheckedItem(isChecked: Boolean) {
        binding.cbSelectAll.isChecked = isChecked
    }
}