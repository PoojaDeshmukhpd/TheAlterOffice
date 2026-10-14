package com.example.appeventtracker.ui.stastics

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.appeventtracker.R
import com.example.appeventtracker.databinding.FragmentStatisticsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = checkNotNull(_binding)

    private val viewModel: StatisticsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatisticsBinding.bind(view)

        observeStatistics()
    }

    private fun observeStatistics() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    // Summary cards
                    binding.tvTotalEvents.text =
                        state.totalProcessedEvents.toString()

                    binding.tvTotalVisits.text =
                        state.totalUniqueVisits.toString()

                    // Event count cards
                    binding.tvInstallCount.text = state.installCount.toString()
                    binding.tvVisitCount.text = state.visitCount.toString()
                    binding.tvAddToCartCount.text = state.addToCartCount.toString()
                    binding.tvPurchaseCount.text = state.purchaseCount.toString()

                    // Breakdown table
                    state.breakdown.forEach { item ->
                        val countText = item.count.toString()
                        val percentageText = String.format(
                            Locale.getDefault(),
                            "%.1f%%",
                            item.percentage
                        )

                        when (item.eventType) {
                            "INSTALL" -> {
                                binding.tvInstallBreakdownCount.text = countText
                                binding.tvInstallBreakdownPercentage.text = percentageText
                            }

                            "VISIT" -> {
                                binding.tvVisitBreakdownCount.text = countText
                                binding.tvVisitBreakdownPercentage.text = percentageText
                            }

                            "ADD_TO_CART" -> {
                                binding.tvAddToCartBreakdownCount.text = countText
                                binding.tvAddToCartBreakdownPercentage.text = percentageText
                            }

                            "PURCHASE" -> {
                                binding.tvPurchaseBreakdownCount.text = countText
                                binding.tvPurchaseBreakdownPercentage.text = percentageText
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "StatisticsFragment"
    }
}