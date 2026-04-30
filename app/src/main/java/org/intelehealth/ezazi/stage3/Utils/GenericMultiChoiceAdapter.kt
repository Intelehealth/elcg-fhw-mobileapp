package org.intelehealth.ezazi.stage3.Utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import org.intelehealth.ezazi.databinding.SelectAllDialogItemHeaderBinding
import org.intelehealth.ezazi.ui.dialog.adapter.MultiChoiceAdapter

class GenericMultiChoiceAdapter(
    context: Context,
    objectsList: ArrayList<String>,
    private val noneOptionText: String? = null
) : MultiChoiceAdapter<String, RecyclerView.ViewHolder>(context, objectsList) {

    override fun searchableValue(position: Int): String {
        return searchableList[position]
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).hashCode().toLong()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        val binding = SelectAllDialogItemHeaderBinding
            .inflate(inflater, parent, false)

        return GenericMultiChoiceViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val itemHolder = holder as GenericMultiChoiceViewHolder
        itemHolder.bind(getItem(position))
        itemHolder.setCheckedItem(isItemSelected(position))
        itemHolder.setClickListener(this)
    }

    override fun onClick(view: View) {
        val checkBox = view.tag as? CheckBox ?: return
        val checkedPosition = view.getTag(view.id) as? Int ?: return

        val clickedText = checkBox.text.toString()

        if (!noneOptionText.isNullOrEmpty() &&
            clickedText.equals(noneOptionText, ignoreCase = true)
        ) {
            // If "None" clicked → clear everything and select only it
            clearSelection()
            selectItem(checkedPosition)
        } else {
            // If any other clicked → remove "None" if selected
            if (!noneOptionText.isNullOrEmpty()) {
                val noneIndex = searchableList.indexOfFirst {
                    it.equals(noneOptionText, ignoreCase = true)
                }

                if (noneIndex != -1) {
                    removeSelection(getItem(noneIndex))
                }
            }

            selectItem(checkedPosition)
        }

        notifyDataSetChanged()
        super.onClick(view)
    }
}