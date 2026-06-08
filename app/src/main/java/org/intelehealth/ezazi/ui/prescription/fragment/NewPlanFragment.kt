package org.intelehealth.ezazi.ui.prescription.fragment

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.app.AppConstants
import org.intelehealth.ezazi.database.dao.ProviderDAO
import org.intelehealth.ezazi.databinding.FragmentNewPlanBinding
import org.intelehealth.ezazi.models.dto.ObsDTO
import org.intelehealth.ezazi.ui.prescription.adapter.LcgAlertsAdapter
import org.intelehealth.ezazi.ui.prescription.listener.TitleChangeListener
import org.intelehealth.ezazi.ui.prescription.viewmodel.PrescriptionViewModel
import org.intelehealth.ezazi.ui.shared.TextChangeListener
import org.intelehealth.ezazi.ui.validation.FirstLetterUpperCaseInputFilter
import org.intelehealth.ezazi.utilities.SessionManager
import org.intelehealth.klivekit.utils.DateTimeUtils

/**
 * Created by Vaghela Mithun R. on 22-02-2024 - 18:11.
 * Email : mithun@intelehealth.org
 * Mob   : +919727206702
 **/

class NewPlanFragment : Fragment(R.layout.fragment_new_plan) {

    private lateinit var binding: FragmentNewPlanBinding
    private lateinit var titleChangeListener: TitleChangeListener

    private val viewModel: PrescriptionViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            ViewModelProvider.Factory.from(PrescriptionViewModel.initializer)
        )[PrescriptionViewModel::class.java]
    }

    // Adapter for the LCG alert rows
    private val lcgAlertsAdapter = LcgAlertsAdapter()

    // Panel expanded/collapsed state — defaults to expanded per acceptance criteria
    private var isLcgPanelExpanded = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNewPlanBinding.bind(view)

        setActionClickListener()
        setInputFilter(binding.etNewPlan)
        binding.etNewPlan.addTextChangedListener(listener)
        setData()
        setupLcgRisksPanel()
        viewModel.prescriptionArg?.visitId?.let { visitId ->
            viewModel.loadBreachedLcgAlerts(visitId)
        }
        observeLcgAlerts()
        if (::titleChangeListener.isInitialized) {
            titleChangeListener.changeScreenTitle(getScreenTitle())
        }
    }

    private fun applyPanelState(animated: Boolean) {
        val body = binding.llLcgRisksBody
        val chevron = binding.ivLcgRisksChevron

        if (animated) {
            val anim = if (isLcgPanelExpanded) {
                body.visibility = View.VISIBLE
                body.alpha = 0f
                body.animate().alpha(1f).setDuration(200).withEndAction(null)
            } else {
                body.animate().alpha(0f).setDuration(200).withEndAction {
                    body.visibility = View.GONE
                }
            }
        } else {
            body.visibility = if (isLcgPanelExpanded) View.VISIBLE else View.GONE
            body.alpha = if (isLcgPanelExpanded) 1f else 0f
        }

        // Rotate chevron: 0° = pointing up (expanded), 180° = pointing down (collapsed)
        val targetRotation = if (isLcgPanelExpanded) 0f else 180f
        chevron.animate().rotation(targetRotation).setDuration(200).start()
    }

    // ─── Existing logic (unchanged) ─────────────────────────────────────────

    private fun setData() {
        arguments?.let {
            NewPlanFragmentArgs.fromBundle(it).plan.let { plan ->
                binding.plan = plan
                binding.updatePosition = -1
            }
        }
    }

    private fun setActionClickListener() {
        binding.btnAddPlanAdd.setOnClickListener { addPlan() }
        binding.btnAddPlanCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setInputFilter(editText: TextInputEditText) {
        editText.filters = arrayOf<InputFilter>(FirstLetterUpperCaseInputFilter())
    }

    private val listener: TextChangeListener = object : TextChangeListener() {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            validatePlanFormInput()
        }
    }

    private fun validatePlanFormInput(): ObsDTO {
        val plan = ObsDTO()
        plan.value = binding.etNewPlan.text.toString()
        binding.btnAddPlanAdd.isEnabled = plan.isValidPlan
        return plan
    }

    private fun clearAddNewMedicineForm() {
        binding.updatePosition = -1
        binding.etNewPlan.setText("")
        validatePlanFormInput()
    }

    private fun addPlan() {
        val plan = validatePlanFormInput()
        val providerId = SessionManager(requireContext()).providerID
        plan.name = ProviderDAO().getCreatorGivenName(providerId)
        plan.setCreatedDate(DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT))
        if (plan.isValidPlan) {
            var updated = -1
            if (binding.updatePosition != null) {
                updated = binding.updatePosition ?: -1
                plan.uuid = binding.plan?.uuid
            }
            if (updated > -1) viewModel.updateItem(updated, plan)
            else viewModel.addItem(plan)

            clearAddNewMedicineForm()
            findNavController().popBackStack(R.id.fragmentAdministered, false)
        }
    }

    private fun getScreenTitle(): Int {
        return viewModel.prescriptionArg?.let {
            return when (it.prescriptionType) {
                PrescriptionFragment.PrescriptionType.PLAN -> {
                    binding.etNewPlan.hint = getString(R.string.hint_add_new_plan)
                    R.string.lbl_add_new_plan
                }
                PrescriptionFragment.PrescriptionType.ASSESSMENT -> {
                    binding.etNewPlan.hint = getString(R.string.hint_add_new_assessment)
                    R.string.lbl_add_new_assessment
                }
                else -> R.string.lbl_add
            }
        } ?: R.string.lbl_add
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is TitleChangeListener) titleChangeListener = context
    }

    private fun observeLcgAlerts() {

        viewModel.breachedLcgAlerts.observe(viewLifecycleOwner) { alerts ->

            lcgAlertsAdapter.submitList(alerts)

            val hasAlerts = alerts.isNotEmpty()

            binding.rvLcgAlerts.visibility = if (hasAlerts) View.VISIBLE else View.GONE

            binding.tvLcgNoAlerts.visibility = if (hasAlerts) View.GONE else View.VISIBLE

          /*  val iconRes =
                if (hasAlerts) R.drawable.ic_high_alert
                else R.drawable.ic_normal_alert
                binding.ivLcgRisksIcon.setImageResource(iconRes)*/
        }
    }
    private fun setupLcgRisksPanel() {

        binding.rvLcgAlerts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = lcgAlertsAdapter
            isNestedScrollingEnabled = false
        }

        fun updateRecyclerViewHeight() {

            binding.rvLcgAlerts.post {

                // Reset first so RecyclerView can measure actual content size
                binding.rvLcgAlerts.layoutParams =
                    binding.rvLcgAlerts.layoutParams.apply {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }

                binding.rvLcgAlerts.requestLayout()

                binding.rvLcgAlerts.post {

                    val maxHeight =
                        (resources.displayMetrics.heightPixels * 0.35f).toInt()

                    val contentHeight = binding.rvLcgAlerts.measuredHeight

                    binding.rvLcgAlerts.layoutParams =
                        binding.rvLcgAlerts.layoutParams.apply {
                            height = minOf(contentHeight, maxHeight)
                        }

                    binding.rvLcgAlerts.isNestedScrollingEnabled =
                        contentHeight > maxHeight

                    binding.rvLcgAlerts.requestLayout()
                }
            }
        }

        lcgAlertsAdapter.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {

                override fun onChanged() {
                    updateRecyclerViewHeight()
                }

                override fun onItemRangeInserted(
                    positionStart: Int,
                    itemCount: Int
                ) {
                    updateRecyclerViewHeight()
                }

                override fun onItemRangeRemoved(
                    positionStart: Int,
                    itemCount: Int
                ) {
                    updateRecyclerViewHeight()
                }
            }
        )

        updateRecyclerViewHeight()

        binding.llLcgRisksBody.visibility = View.VISIBLE
        binding.ivLcgRisksChevron.rotation = -90f

        binding.llLcgRisksHeader.setOnClickListener {

            val isExpanded =
                binding.llLcgRisksBody.visibility == View.VISIBLE

            if (isExpanded) {

                binding.llLcgRisksBody.visibility = View.GONE

                binding.ivLcgRisksChevron.animate()
                    .rotation(0f)
                    .setDuration(200)
                    .start()

            } else {

                binding.llLcgRisksBody.visibility = View.VISIBLE

                binding.ivLcgRisksChevron.animate()
                    .rotation(-90f)
                    .setDuration(200)
                    .start()

                updateRecyclerViewHeight()
            }
        }
    }
}
