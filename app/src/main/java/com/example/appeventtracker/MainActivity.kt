package com.example.appeventtracker

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.appeventtracker.data.EventEnqueueCoordinator
import com.example.appeventtracker.databinding.ActivityMainBinding
import com.example.appeventtracker.ui.queue.QueueFragment
import com.example.appeventtracker.ui.stastics.StatisticsFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var enqueueCoordinator: EventEnqueueCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("AppDebug", "MainActivity onCreate executed")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // The toolbar title is already displayed in XML.
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        enqueueInstallEventOnce()
        setupBottomNavigation()

        // Show Queue by default on the first launch.
        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.navigation_queue
            showQueueScreen()
        }
    }


    private fun enqueueInstallEventOnce() {
        lifecycleScope.launch {
            val prefs = getSharedPreferences(
                "install_tracker",
                MODE_PRIVATE
            )

            val installId = prefs.getString("install_id", null)
                ?: UUID.randomUUID().toString().also {
                    prefs.edit()
                        .putString("install_id", it)
                        .commit()
                }

            if (prefs.getBoolean("install_event_submitted", false)) {
                Log.d("InstallTracker", "INSTALL already submitted: $installId")
                return@launch
            }

            val installJson = """
            {
                "eventType": "INSTALL",
                "payload": {
                    "installId": "$installId"
                }
            }
        """.trimIndent()

            try {
                val results = enqueueCoordinator.enqueueJson(installJson)

                // Mark as submitted only if the event was inserted
                // or was already present because of deduplication.
                val accepted = results.any {
                    it is com.example.appeventtracker.data.local.EnqueueResult.Enqueued ||
                            it is com.example.appeventtracker.data.local.EnqueueResult.Duplicate
                }

                if (accepted) {
                    prefs.edit()
                        .putBoolean("install_event_submitted", true)
                        .commit()

                    Log.d("InstallTracker", "INSTALL already recorded: $installId")
                }
            } catch (exception: Exception) {
                Log.e("InstallTracker", "Failed to enqueue INSTALL", exception)
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_queue -> {
                    showQueueScreen()
                    true
                }

                R.id.navigation_statistics -> {
                    showStatisticsScreen()
                    true
                }

                else -> false
            }
        }
    }

    private fun showQueueScreen() {
        // Avoid recreating the same Fragment unnecessarily.
        if (supportFragmentManager.findFragmentByTag(QueueFragment.TAG) != null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                show(
                    supportFragmentManager.findFragmentByTag(QueueFragment.TAG)!!
                )
                supportFragmentManager.findFragmentByTag(
                    StatisticsFragment.TAG
                )?.let { hide(it) }
            }
            return
        }

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            supportFragmentManager.findFragmentByTag(
                StatisticsFragment.TAG
            )?.let { hide(it) }

            replace(
                R.id.fragmentContainer,
                QueueFragment(),
                QueueFragment.TAG
            )
        }
    }

    private fun showStatisticsScreen() {
        val statisticsFragment =
            supportFragmentManager.findFragmentByTag(StatisticsFragment.TAG)

        if (statisticsFragment != null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                supportFragmentManager.findFragmentByTag(
                    QueueFragment.TAG
                )?.let { hide(it) }
                show(statisticsFragment)
            }
        } else {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                supportFragmentManager.findFragmentByTag(
                    QueueFragment.TAG
                )?.let { hide(it) }

                replace(
                    R.id.fragmentContainer,
                    StatisticsFragment(),
                    StatisticsFragment.TAG
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val isNewSession = sessionManager.startOrResumeSession()

        if (isNewSession) {
            enqueueVisitEvent()
        }
    }

    override fun onStop() {
        sessionManager.markAppBackgrounded()
        super.onStop()
    }

    private fun enqueueVisitEvent() {
        lifecycleScope.launch {
            try {
                val sessionId = sessionManager.getCurrentSessionId()

                val visitJson = """
                {
                    "eventType": "VISIT",
                    "payload": {
                        "source": "app_session_start",
                        "sessionId": "$sessionId"
                    }
                }
            """.trimIndent()

                enqueueCoordinator.enqueueJson(visitJson)

                Log.d(
                    "VisitTracker",
                    "VISIT enqueued for session: $sessionId"
                )
            } catch (exception: Exception) {
                Log.e("VisitTracker", "Unable to enqueue VISIT", exception)
            }
        }
    }
}