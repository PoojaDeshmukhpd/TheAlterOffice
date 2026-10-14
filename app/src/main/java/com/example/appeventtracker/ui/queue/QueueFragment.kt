package com.example.appeventtracker.ui.queue

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appeventtracker.R
import com.example.appeventtracker.databinding.FragmentQueueBinding
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QueueFragment : Fragment(R.layout.fragment_queue) {

    private var _binding: FragmentQueueBinding? = null
    private val binding get() = checkNotNull(_binding)

    private val viewModel: QueueViewModel by viewModels()
    private lateinit var eventAdapter: EventAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQueueBinding.bind(view)

        setupRecyclerView()
        setupQueueTabs()
        setupRefresh()
        observeUiState()
        setupAddEvent()
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter()
        binding.rvEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
        }
    }

    private fun setupQueueTabs() {
        binding.queueTabs.addTab(
            binding.queueTabs.newTab().setText("IN PROGRESS")
        )
        binding.queueTabs.addTab(
            binding.queueTabs.newTab().setText("FAILED / RETRYING")
        )

        // Keep the selected tab synchronized with ViewModel state.
        binding.queueTabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    viewModel.selectFilter(
                        if (tab.position == 0) {
                            QueueFilter.IN_PROGRESS
                        } else {
                            QueueFilter.FAILED_RETRYING
                        }
                    )
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit
                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            }
        )
    }

    private fun setupRefresh() {
        binding.btnRefresh.setOnClickListener {
//            viewModel.refresh()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.visibleEvents.collect { events ->
                        eventAdapter.submitList(events)
                    }
                }

                launch {
                    viewModel.isSubmitting.collect { isSubmitting ->
                        // This view may be added back when we restore the JSON input area.
                        // No submit button is present in the current reference layout.
                    }
                }

                launch {
                    viewModel.message.collect { message ->
                        message?.let {
                            android.widget.Toast.makeText(
                                requireContext(),
                                it,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            viewModel.clearMessage()
                        }
                    }
                }
            }
        }
    }


    private fun setupAddEvent() {
        binding.btnAddEvent.setOnClickListener {
            val input = EditText(requireContext()).apply {
                hint = """{"eventType":"INSTALL","payload":{"source":"test"}}"""
                inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = 4
                maxLines = 8
                setPadding(32, 24, 32, 24)
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Submit Events")
                .setMessage("Enter one event JSON object or an array of events or else just click on submit.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Submit", null)
                .create()
                .apply {
                    setOnShowListener {
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val enteredJson = input.text.toString().trim()

                            val json = if (enteredJson.isBlank()) {
                                Log.d(TAG, "Submitting dummy events. or simply click on submit")

                                """
                                    [
                                      {
                                        "eventType": "INSTALL",
                                        "payload": {
                                          "source": "dummy_test"
                                        }
                                      },
                                      {
                                        "eventType": "VISIT",
                                        "payload": {
                                          "screen": "home"
                                        }
                                      },
                                      {
                                        "eventType": "PURCHASE",
                                        "payload": {
                                          "productId": "product_101",
                                          "amount": 499
                                        }
                                      },
                                      {
                                        "eventType": "ADD_TO_CART",
                                        "payload": {
                                          "productId": "product_101",
                                          "quantity": 1
                                        }
                                      }
                                    ]
                                    """.trimIndent()
                            } else {
                                enteredJson
                            }

                            Log.d(TAG, "Submitting events to ViewModel")
                            viewModel.submitEvents(json)
                            dismiss()
                        }
                    }
                    show()
                }
        }
    }


    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "QueueFragment"
    }
}