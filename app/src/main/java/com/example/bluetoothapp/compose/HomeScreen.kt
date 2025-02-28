package com.example.bluetoothapp.compose

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bluetoothapp.R
import com.example.bluetoothapp.bluetooth.ConnectGATT
import com.example.bluetoothapp.viewmodels.CrashAlertViewModel
import kotlinx.coroutines.launch


enum class AppPage(
    @StringRes val titleResId: Int,
    @DrawableRes val drawableResId: Int
) {
    DEVICES(R.string.devices_title, R.drawable.baseline_devices_other_24),
    BLUETOOTH(R.string.bluetooth, R.drawable.baseline_bluetooth_24),
    CRASH_ALERTS(R.string.crash_alerts, R.drawable.baseline_warning_24)
}

@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    pages: Array<AppPage> = AppPage.values(),
    crashAlertViewModel: CrashAlertViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val context = LocalContext.current
    
    // Check for intent extras that might direct us to the crash alerts tab
    val intent = (context as? androidx.activity.ComponentActivity)?.intent
    val openCrashTab = intent?.getBooleanExtra("OPEN_CRASH_TAB", false) ?: false
    
    // If directed to crash tab from notification, scroll there
    if (openCrashTab) {
        val crashAlertId = intent?.getStringExtra("CRASH_ALERT_ID")
        if (crashAlertId != null) {
            // Auto-acknowledge the alert that triggered the notification
            crashAlertViewModel.acknowledgeCrashAlert(crashAlertId)
        }
        
        // Find the crash alerts tab index
        val crashTabIndex = pages.indexOfFirst { it == AppPage.CRASH_ALERTS }
        if (crashTabIndex >= 0) {
            rememberCoroutineScope().launch {
                pagerState.animateScrollToPage(crashTabIndex)
            }
        }
    }

    HomePagerScreen(
        pagerState = pagerState,
        pages = pages,
        crashAlertViewModel = crashAlertViewModel
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePagerScreen(
    pagerState: PagerState,
    pages: Array<AppPage>,
    modifier: Modifier = Modifier,
    crashAlertViewModel: CrashAlertViewModel = hiltViewModel()
) {
    val hasUnacknowledgedAlerts by crashAlertViewModel.hasUnacknowledgedAlerts.collectAsState()
    val unacknowledgedAlertsCount by crashAlertViewModel.unacknowledgedAlerts.collectAsState()
    
    Column(modifier) {
        val coroutineScope = rememberCoroutineScope()

        // Tab Row
        TabRow(
            selectedTabIndex = pagerState.currentPage
        ) {
            pages.forEachIndexed { index, page ->
                val title = stringResource(id = page.titleResId)
                
                if (page == AppPage.CRASH_ALERTS && hasUnacknowledgedAlerts) {
                    // Add badge for unacknowledged crash alerts
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(text = title) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = Color.Red
                                    ) {
                                        Text(text = unacknowledgedAlertsCount.size.toString())
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = page.drawableResId),
                                    contentDescription = title
                                )
                            }
                        },
                        unselectedContentColor = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    // Regular tab without badge
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(text = title) },
                        icon = {
                            Icon(
                                painter = painterResource(id = page.drawableResId),
                                contentDescription = title
                            )
                        },
                        unselectedContentColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Pages
        HorizontalPager(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .testTag("horizontalPagerTag"),
            state = pagerState,
            verticalAlignment = Alignment.Top,
        ) { index ->
            when (pages[index]) {
                AppPage.DEVICES -> {
                    DevicesScreen()
                }
                
                AppPage.BLUETOOTH -> {
                    ConnectGATT()
                }
                
                AppPage.CRASH_ALERTS -> {
                    CrashAlertsScreen()
                }
            }
        }
    }
}

