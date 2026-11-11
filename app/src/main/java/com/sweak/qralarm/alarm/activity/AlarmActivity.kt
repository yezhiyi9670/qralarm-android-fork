package com.sweak.qralarm.alarm.activity

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.sweak.qralarm.alarm.service.AlarmService
import com.sweak.qralarm.app.activity.MainActivity
import com.sweak.qralarm.core.designsystem.theme.QRAlarmTheme
import com.sweak.qralarm.core.domain.alarm.AlarmsRepository
import com.sweak.qralarm.core.domain.user.UserDataRepository
import com.sweak.qralarm.core.domain.user.model.Theme
import com.sweak.qralarm.core.navigation.Navigator
import com.sweak.qralarm.core.navigation.rememberNavigationState
import com.sweak.qralarm.core.navigation.routes.AlarmRoute
import com.sweak.qralarm.core.navigation.routes.DisableAlarmScannerRoute
import com.sweak.qralarm.core.navigation.routes.EmergencyRoute
import com.sweak.qralarm.core.navigation.toEntries
import com.sweak.qralarm.features.alarm.AlarmScreen
import com.sweak.qralarm.features.disable_alarm_scanner.DisableAlarmScannerScreen
import com.sweak.qralarm.features.emergency.task.EmergencyScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class AlarmActivity : FragmentActivity() {

    @Inject
    lateinit var alarmsRepository: AlarmsRepository

    @Inject
    lateinit var userDataRepository: UserDataRepository

    private var isLaunchedFromMainActivity: Boolean = false
    private val alarmIdState = mutableStateOf(0L)
    private var isStoppingAlarm: Boolean = false
    private var lastNavigateUpTime: Long = 0L

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.apply {
                addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
                addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            }
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        alarmIdState.value = intent.extras?.getLong(EXTRA_ALARM_ID) ?: 0L
        isLaunchedFromMainActivity =
            intent.extras?.getBoolean(EXTRA_LAUNCHED_FROM_MAIN_ACTIVITY) == true

        // The service is a singleton, so this screen has to follow the alarm that is actually
        // ringing - a new alarm may take over without its intent ever reaching this instance:
        lifecycleScope.launch {
            AlarmService.ringingAlarmId.collect { ringingAlarmId ->
                if (isStoppingAlarm) return@collect

                if (ringingAlarmId == null) {
                    // Nothing rings anymore, so a screen opened by the alarm itself has nothing
                    // left to show. The one opened from the main activity may present a snoozed
                    // alarm, so it is left to its opener:
                    if (!isLaunchedFromMainActivity) {
                        finish()
                    }
                } else if (ringingAlarmId != alarmIdState.value) {
                    alarmIdState.value = ringingAlarmId
                }
            }
        }

        setContent {
            val theme by userDataRepository.theme.collectAsStateWithLifecycle(Theme.Default)
            val alarmId by alarmIdState

            QRAlarmTheme(theme = theme) {
                key(alarmId) {
                    val startRoute = AlarmRoute(idOfAlarm = alarmId)

                    val navigationState = rememberNavigationState(
                        startRoute = startRoute,
                        topLevelRoutes = setOf(startRoute)
                    )
                    val navigator = remember {
                        Navigator(navigationState)
                    }

                    val alarmEntryProvider = entryProvider {
                        entry<AlarmRoute> { route ->
                            AlarmScreen(
                                idOfAlarm = route.idOfAlarm,
                                isTransient = !isLaunchedFromMainActivity,
                                onStopAlarm = {
                                    lifecycleScope.launch {
                                        stopService(alarmId = alarmId)
                                        finish()

                                        if (isLaunchedFromMainActivity) {
                                            startActivity(
                                                Intent(
                                                    this@AlarmActivity,
                                                    MainActivity::class.java
                                                )
                                            )
                                        }
                                    }
                                },
                                onRequestCodeScan = {
                                    navigator.navigate(
                                        DisableAlarmScannerRoute(
                                            idOfAlarm = alarmId,
                                            isDisablingBeforeAlarmFired = false
                                        )
                                    )
                                },
                                onSnoozeAlarm = {
                                    lifecycleScope.launch {
                                        stopService(alarmId = alarmId)

                                        if (!isLaunchedFromMainActivity) {
                                            delay(ALARM_CONFIRMATION_DISPLAY_DURATION)
                                            finish()
                                        }
                                    }
                                },
                                onEmergencyClicked = {
                                    navigator.navigate(
                                        EmergencyRoute(
                                            idOfAlarmToCancel = alarmId
                                        )
                                    )
                                }
                            )
                        }

                        entry<DisableAlarmScannerRoute> { route ->
                            DisableAlarmScannerScreen(
                                idOfAlarm = route.idOfAlarm,
                                isDisablingBeforeAlarmFired = route.isDisablingBeforeAlarmFired,
                                onAlarmDisabled = { uriStringToTryToOpen ->
                                    if (uriStringToTryToOpen == null) {
                                        lifecycleScope.launch {
                                            stopService(alarmId)

                                            navigator.goBack()
                                            delay(ALARM_CONFIRMATION_DISPLAY_DURATION)
                                            finish()

                                            if (isLaunchedFromMainActivity) {
                                                startActivity(
                                                    Intent(
                                                        this@AlarmActivity,
                                                        MainActivity::class.java
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        var uri = uriStringToTryToOpen

                                        if (!uriStringToTryToOpen.startsWith("http://") &&
                                            !uriStringToTryToOpen.startsWith("https://")
                                        ) {
                                            uri = "http://$uri"
                                        }

                                        val keyguardManager =
                                            getSystemService(KEYGUARD_SERVICE) as KeyguardManager

                                        if (keyguardManager.isKeyguardSecure &&
                                            keyguardManager.isDeviceLocked
                                        ) {
                                            keyguardManager.requestDismissKeyguard(
                                                this@AlarmActivity,
                                                object :
                                                    KeyguardManager.KeyguardDismissCallback() {
                                                    override fun onDismissSucceeded() {
                                                        super.onDismissSucceeded()

                                                        lifecycleScope.launch {
                                                            stopService(alarmId = alarmId)
                                                            finish()

                                                            try {
                                                                startActivity(
                                                                    Intent(
                                                                        Intent.ACTION_VIEW,
                                                                        uri.toUri()
                                                                    )
                                                                )
                                                            } catch (_: Exception) {
                                                                if (isLaunchedFromMainActivity) {
                                                                    startActivity(
                                                                        Intent(
                                                                            this@AlarmActivity,
                                                                            MainActivity::class.java
                                                                        )
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    override fun onDismissCancelled() {
                                                        navigator.goBack()
                                                    }
                                                }
                                            )
                                        } else {
                                            lifecycleScope.launch {
                                                stopService(alarmId = alarmId)
                                                finish()

                                                try {
                                                    startActivity(
                                                        Intent(
                                                            Intent.ACTION_VIEW,
                                                            uri.toUri()
                                                        )
                                                    )
                                                } catch (_: Exception) {
                                                    if (isLaunchedFromMainActivity) {
                                                        startActivity(
                                                            Intent(
                                                                this@AlarmActivity,
                                                                MainActivity::class.java
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                onCloseClicked = {
                                    val currentTimeMillis = System.currentTimeMillis()

                                    if (lastNavigateUpTime + 3000L <= currentTimeMillis) {
                                        lastNavigateUpTime = currentTimeMillis
                                        navigator.goBack()
                                    }
                                }
                            )
                        }

                        entry<EmergencyRoute> { route ->
                            EmergencyScreen(
                                idOfAlarmToCancel = route.idOfAlarmToCancel,
                                onCloseClicked = {
                                    val currentTimeMillis = System.currentTimeMillis()

                                    if (lastNavigateUpTime + 3000L <= currentTimeMillis) {
                                        lastNavigateUpTime = currentTimeMillis
                                        navigator.goBack()
                                    }
                                },
                                onEmergencyTaskCompleted = {
                                    lifecycleScope.launch {
                                        stopService(alarmId = alarmId)
                                        finish()

                                        if (isLaunchedFromMainActivity) {
                                            startActivity(
                                                Intent(
                                                    this@AlarmActivity,
                                                    MainActivity::class.java
                                                )
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }

                    NavDisplay(
                        entries = navigationState.toEntries(
                            entryProvider = { alarmEntryProvider(it) }
                        ),
                        onBack = { navigator.goBack() },
                        transitionSpec = { fadeIn() togetherWith ExitTransition.None },
                        popTransitionSpec = { EnterTransition.None togetherWith fadeOut() },
                        predictivePopTransitionSpec = { EnterTransition.None togetherWith fadeOut() }
                    )
                }
            }
        }
    }

    private suspend fun stopService(alarmId: Long) {
        // The screen is closed by the caller, so the missing alarm below is expected:
        isStoppingAlarm = true

        alarmsRepository.setAlarmRunning(
            alarmId = alarmId,
            running = false
        )
        stopService(
            Intent(this@AlarmActivity, AlarmService::class.java)
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        isLaunchedFromMainActivity =
            intent.extras?.getBoolean(EXTRA_LAUNCHED_FROM_MAIN_ACTIVITY) == true

        val newAlarmId = intent.extras?.getLong(EXTRA_ALARM_ID) ?: 0L

        // launchMode is singleTop, so this instance is reused for another alarm, while the whole
        // navigation was built for the previous one:
        if (newAlarmId != alarmIdState.value) {
            setIntent(intent)
            alarmIdState.value = newAlarmId
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarmId"
        const val EXTRA_LAUNCHED_FROM_MAIN_ACTIVITY = "launchedFromMainActivity"

        private val ALARM_CONFIRMATION_DISPLAY_DURATION = 3.seconds
    }
}