package org.intelehealth.ezazi.ui.prescription.fragment

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

    private val lcgAlertsAdapter = LcgAlertsAdapter()

    // RecyclerView never exceeds 38% of screen height; scrolls internally beyond that
    private val FRAC_RV_MAX = 0.35f

    private val screenHeight: Int
        get() = requireActivity().window.decorView.height
            .takeIf { it > 0 } ?: resources.displayMetrics.heightPixels

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNewPlanBinding.bind(view)

        setupKeyboardHandling()
        setActionClickListener()
        setInputFilter(binding.etNewPlan)
        binding.etNewPlan.addTextChangedListener(textWatcher)
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

    // ─── Keyboard handling ──────────────────────────────────────────────────

    /**
     * Two-part fix:
     * 1. adjustResize (Manifest) shrinks the NestedScrollView's window when
     *    keyboard opens — this makes the scroll container shorter so its
     *    content (which is taller, height=match_parent of the ORIGINAL
     *    screen height... see note below) becomes scrollable.
     * 2. On EditText focus, scroll it to the bottom of the scroll view so
     *    it sits just above the keyboard / above the buttons.
     */
    private fun setupKeyboardHandling() {
        binding.etNewPlan.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.nestedScrollView.post {
                    binding.nestedScrollView.smoothScrollTo(
                        0,
                        binding.layoutButtonsAdd.bottom
                    )
                }
            }
        }

        // Also re-scroll on every IME inset change (covers rotation / multiwindow)
        ViewCompat.setOnApplyWindowInsetsListener(binding.nestedScrollView) { v: View?, insets: WindowInsetsCompat ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (imeVisible && binding.etNewPlan.hasFocus()) {
                binding.nestedScrollView.post {
                    binding.nestedScrollView.smoothScrollTo(
                        0,
                        binding.layoutButtonsAdd.bottom
                    )
                }
            }
            insets
        }
    }

    // ─── RV height cap ──────────────────────────────────────────────────────

    private fun capRecyclerViewHeight() {
        val rv = binding.rvLcgAlerts
        val cap = (screenHeight * FRAC_RV_MAX).toInt()

        rv.layoutParams = rv.layoutParams.apply { height = ViewGroup.LayoutParams.WRAP_CONTENT }
        rv.requestLayout()

        rv.post {
            val contentH = rv.measuredHeight
            val finalH = if (contentH > cap) cap else contentH

            rv.layoutParams = rv.layoutParams.apply { height = finalH }
            rv.isNestedScrollingEnabled = contentH > cap
            rv.requestLayout()
        }
    }

    // ─── LCG panel ──────────────────────────────────────────────────────────

    private fun setupLcgRisksPanel() {
        binding.rvLcgAlerts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = lcgAlertsAdapter
            isNestedScrollingEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        lcgAlertsAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() { capRecyclerViewHeight() }
            override fun onItemRangeInserted(p: Int, n: Int) { capRecyclerViewHeight() }
            override fun onItemRangeRemoved(p: Int, n: Int) { capRecyclerViewHeight() }
        })

        binding.llLcgRisksBody.visibility = View.VISIBLE
        binding.ivLcgRisksChevron.rotation = -90f

        binding.llLcgRisksHeader.setOnClickListener {
            if (binding.llLcgRisksBody.visibility == View.VISIBLE) collapsePanel()
            else expandPanel()
        }
    }

    private fun collapsePanel() {
        binding.llLcgRisksBody.animate()
            .alpha(0f).setDuration(200)
            .withEndAction {
                binding.llLcgRisksBody.visibility = View.GONE
                binding.llLcgRisksBody.alpha = 1f
                // LinearLayout weight redistributes — EditText fills freed space
            }.start()
        binding.ivLcgRisksChevron.animate().rotation(0f).setDuration(200).start()
    }

    private fun expandPanel() {
        binding.llLcgRisksBody.alpha = 0f
        binding.llLcgRisksBody.visibility = View.VISIBLE
        binding.llLcgRisksBody.animate().alpha(1f).setDuration(200).start()
        binding.ivLcgRisksChevron.animate().rotation(-90f).setDuration(200).start()
        capRecyclerViewHeight()
    }

    private fun observeLcgAlerts() {
        viewModel.breachedLcgAlerts.observe(viewLifecycleOwner) { alerts ->
            lcgAlertsAdapter.submitList(alerts)
            val hasAlerts = alerts.isNotEmpty()
            binding.rvLcgAlerts.visibility       = if (hasAlerts) View.VISIBLE else View.GONE
            binding.tvLcgNoAlerts.visibility     = if (hasAlerts) View.GONE    else View.VISIBLE
            binding.ivLcgRisksChevron.visibility = if (hasAlerts) View.VISIBLE else View.GONE
            binding.llLcgRisksHeader.isClickable = hasAlerts
        }
    }

    // ─── Existing logic ──────────────────────────────────────────────────────

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
        binding.btnAddPlanCancel.setOnClickListener { findNavController().popBackStack() }
    }

    private fun setInputFilter(editText: TextInputEditText) {
        editText.filters = arrayOf<InputFilter>(FirstLetterUpperCaseInputFilter())
    }

    private val textWatcher: TextChangeListener = object : TextChangeListener() {
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
}

