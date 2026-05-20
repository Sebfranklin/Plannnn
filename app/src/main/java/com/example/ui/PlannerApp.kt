package com.example.ui

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CalendarEvent
import com.example.data.model.Goal
import com.example.data.model.Task
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GlowCard(
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bentoBg = when (color) {
        NeonRed -> BentoCardPurple
        NeonBlue -> BentoCardGray
        NeonYellow -> BentoCardPink
        else -> {
            if (color != Color.Unspecified) color.copy(alpha = 0.12f) else BentoCardPink
        }
    }
    
    val bentoBorderColor = when (color) {
        NeonRed -> BentoCardPurpleBorder
        NeonBlue -> BentoCardGrayBorder
        NeonYellow -> BentoCardPurpleBorder.copy(alpha = 0.3f)
        else -> {
            if (color != Color.Unspecified) color.copy(alpha = 0.35f) else BentoCardGrayBorder
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bentoBg),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, bentoBorderColor),
        modifier = modifier.padding(vertical = 4.dp, horizontal = 2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.padding(4.dp)) {
            content()
        }
    }
}

@Composable
fun NeonDivider(modifier: Modifier = Modifier, color: Color = NeonBlue) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BentoCardGrayBorder)
    )
}

@Composable
fun PuckStatusIndicator(
    modifier: Modifier = Modifier,
    color: Color = NeonYellow,
    text: String = "ACTIVE"
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(color, radius = 4.dp.toPx(), center = center)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = BentoTextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        )
    }
}

@Composable
fun PlannerApp(viewModel: PlannerViewModel) {
    val context = LocalContext.current
    val currentTab = remember { mutableStateOf(0) } // 0: Schedule, 1: Tasks, 2: Goals, 3: AI Advisor

    val selectedDateMs by viewModel.selectedDateMs.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }

    // Toast-handler for local actions
    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearSyncMessage()
        }
    }

    // Permission launcher for accessing local system calendar
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.syncCalendar(context)
            } else {
                Toast.makeText(context, "System Calendar Permission Denied.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
                NeonDivider(color = BentoCardGrayBorder)
                NavigationBar(
                    containerColor = BentoNavBackground,
                    modifier = Modifier.navigationBarsPadding(),
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab.value == 0,
                        onClick = { currentTab.value = 0 },
                        icon = { Icon(Icons.Filled.DateRange, "Schedule") },
                        label = { Text("Schedule") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BentoTextPurple,
                            unselectedIconColor = BentoTextSecondary.copy(alpha = 0.6f),
                            selectedTextColor = BentoTextPurple,
                            unselectedTextColor = BentoTextSecondary.copy(alpha = 0.6f),
                            indicatorColor = BentoNavActivePill
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab.value == 1,
                        onClick = { currentTab.value = 1 },
                        icon = { Icon(Icons.Filled.List, "Tasks") },
                        label = { Text("Tasks") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BentoTextPurple,
                            unselectedIconColor = BentoTextSecondary.copy(alpha = 0.6f),
                            selectedTextColor = BentoTextPurple,
                            unselectedTextColor = BentoTextSecondary.copy(alpha = 0.6f),
                            indicatorColor = BentoNavActivePill
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab.value == 2,
                        onClick = { currentTab.value = 2 },
                        icon = { Icon(Icons.Filled.Star, "Goals") },
                        label = { Text("Goals") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BentoTextPurple,
                            unselectedIconColor = BentoTextSecondary.copy(alpha = 0.6f),
                            selectedTextColor = BentoTextPurple,
                            unselectedTextColor = BentoTextSecondary.copy(alpha = 0.6f),
                            indicatorColor = BentoNavActivePill
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab.value == 3,
                        onClick = { currentTab.value = 3 },
                        icon = { Icon(Icons.Filled.Face, "Aura AI") },
                        label = { Text("Aura AI") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonRed,
                            unselectedIconColor = BentoTextSecondary.copy(alpha = 0.6f),
                            selectedTextColor = NeonRed,
                            unselectedTextColor = BentoTextSecondary.copy(alpha = 0.6f),
                            indicatorColor = BentoNavActivePill
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            var expandedFab by remember { mutableStateOf(false) }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                if (expandedFab) {
                    FloatingActionButton(
                        onClick = {
                            showAddEventDialog = true
                            expandedFab = false
                        },
                        containerColor = BentoCardPink,
                        contentColor = BentoTextPurple,
                        modifier = Modifier
                            .testTag("fab_add_event")
                            .border(1.dp, BentoCardPurpleBorder, RoundedCornerShape(16.dp)),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Add, "Event")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Event", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    FloatingActionButton(
                        onClick = {
                            showAddTaskDialog = true
                            expandedFab = false
                        },
                        containerColor = BentoCardPink,
                        contentColor = BentoTextPurple,
                        modifier = Modifier
                            .testTag("fab_add_task")
                            .border(1.dp, BentoCardPurpleBorder, RoundedCornerShape(16.dp)),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, "Task")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Task", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    FloatingActionButton(
                        onClick = {
                            showAddGoalDialog = true
                            expandedFab = false
                        },
                        containerColor = BentoCardPink,
                        contentColor = BentoTextPurple,
                        modifier = Modifier
                            .testTag("fab_add_goal")
                            .border(1.dp, BentoCardPurpleBorder, RoundedCornerShape(16.dp)),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, "Goal")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Goal", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { expandedFab = !expandedFab },
                    containerColor = BentoDeepPurple,
                    contentColor = BentoWhite,
                    modifier = Modifier.testTag("main_add_fab"),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (expandedFab) Icons.Filled.Close else Icons.Filled.Add,
                        contentDescription = "Expand Creation Actions",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            color = DarkBackground,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Area with Neon Banner
                HeaderArea(
                    onSyncRequested = {
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    },
                    onDemoRequested = {
                        viewModel.loadDemoDataset()
                    }
                )

                // Shared Dynamic Calendar Row
                HorizontalDatePicker(
                    selectedDateMs = selectedDateMs,
                    onDateSelected = { viewModel.selectDate(it) }
                )

                // Main Workspace Layout with animated content shifts
                Box(
                    modifier = Modifier
                        .fillPanel()
                        .weight(1f)
                ) {
                    AnimatedContent(
                        targetState = currentTab.value,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
                        },
                        label = "MainContentTransition"
                    ) { targetTab ->
                        when (targetTab) {
                            0 -> ScheduleTab(viewModel)
                            1 -> TasksTab(viewModel)
                            2 -> GoalsTab(viewModel)
                            3 -> AiAdvisorTab(viewModel)
                        }
                    }
                }
            }
        }
    }

    // Modal Create Dialogs
    if (showAddEventDialog) {
        CreateEventDialog(
            selectedDateMs = selectedDateMs,
            onDismiss = { showAddEventDialog = false },
            onConfirm = { title, desc, startMs, endMs, loc, col ->
                viewModel.addEvent(title, desc, selectedDateMs, startMs, endMs, loc, col)
                showAddEventDialog = false
            }
        )
    }

    if (showAddTaskDialog) {
        CreateTaskDialog(
            selectedDateMs = selectedDateMs,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, desc, time, priority, cat ->
                viewModel.addTask(title, desc, selectedDateMs, time, priority, cat)
                showAddTaskDialog = false
            }
        )
    }

    if (showAddGoalDialog) {
        CreateGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { title, desc, target, unit, cat, deadlineMs ->
                viewModel.addGoal(title, desc, target, unit, cat, deadlineMs)
                showAddGoalDialog = false
            }
        )
    }
}

private fun Modifier.fillPanel() = this.fillMaxWidth().fillMaxHeight()

@Composable
fun HeaderArea(
    onSyncRequested: () -> Unit,
    onDemoRequested: () -> Unit
) {
    val todayMs = System.currentTimeMillis()
    val dayFormat = remember { SimpleDateFormat("EEEE", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val dayName = dayFormat.format(Date(todayMs))
    val dateStr = dateFormat.format(Date(todayMs))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = BentoTextSecondary,
                        letterSpacing = 1.5.sp
                    )
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary
                    )
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Sync / Demo buttons
                IconButton(
                    onClick = onSyncRequested,
                    modifier = Modifier
                        .testTag("sync_calendar_button")
                        .background(BentoCardBlue, CircleShape)
                        .border(1.dp, BentoCardBlueBorder, CircleShape)
                        .size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Sync Devices Calendar",
                        tint = BentoTextBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDemoRequested,
                    modifier = Modifier
                        .testTag("load_demo_button")
                        .background(BentoCardGray, CircleShape)
                        .border(1.dp, BentoCardGrayBorder, CircleShape)
                        .size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Load Beautiful Demo Dataset",
                        tint = BentoDeepPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Custom circular initials avatar "SF" to represent Sebastian Franklin
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .background(BentoNavActivePill, CircleShape)
                        .border(1.5.dp, BentoWhite, CircleShape)
                ) {
                    Text(
                        text = "SF",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoTextPurple
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun HorizontalDatePicker(
    selectedDateMs: Long,
    onDateSelected: (Long) -> Unit
) {
    val dateList = remember {
        val list = mutableListOf<Long>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        
        // Populate 8 consecutive days starting today
        for (i in 0..7) {
            list.add(cal.timeInMillis)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val dayFormat = SimpleDateFormat("EE", Locale.getDefault())
    val numFormat = SimpleDateFormat("dd", Locale.getDefault())

    Column {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(dateList) { timestamp ->
                val isSelected = timestamp == selectedDateMs
                val dayStr = dayFormat.format(Date(timestamp))
                val numStr = numFormat.format(Date(timestamp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) BentoNavActivePill else BentoCardGray
                    ),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) BentoDeepPurple else BentoCardGrayBorder
                    ),
                    modifier = Modifier
                        .width(56.dp)
                        .height(72.dp)
                        .clickable { onDateSelected(timestamp) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayStr.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) BentoTextPurple else BentoTextSecondary.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = numStr,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = if (isSelected) BentoTextPurple else BentoTextPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        NeonDivider(color = BentoCardGrayBorder)
    }
}

// --- TAB 1: SCHEDULE VIEW ---
@Composable
fun ScheduleTab(viewModel: PlannerViewModel) {
    val events by viewModel.selectedDayEvents.collectAsStateWithLifecycle()
    val tasks by viewModel.selectedDayTasks.collectAsStateWithLifecycle()

    if (events.isEmpty() && tasks.isEmpty()) {
        EmptyStateDisplay(
            title = "Agenda Clear",
            description = "Click stars to load the demo dataset or tap the main addition FAB to outline code standups, sprints, or workouts."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Interactive visual mini ice-rink schema easter egg for gaming/sports references.
            // Displays tasks as speed pucks floating into goal zone!
            item {
                CyberRinkWidget(tasks = tasks)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 TIMELINE AGENDA",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = BentoTextPurple,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    )
                    PuckStatusIndicator(color = BentoDeepPurple, text = "LIVE TRACKING")
                }
            }

            items(events) { event ->
                val startStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.startTime))
                val endStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.endTime))

                GlowCard(color = Color(event.color)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(listOf(Color(event.color), Color.Transparent)),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = startStr,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = BentoTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "to",
                                tint = Color(event.color).copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = endStr,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BentoTextSecondary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (event.location != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Icon(Icons.Filled.LocationOn, "Location", tint = BentoDeepPurple, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = event.location,
                                        style = MaterialTheme.typography.labelSmall.copy(color = BentoDeepPurple),
                                        maxLines = 1
                                    )
                                }
                            }
                            if (event.description.isNotEmpty()) {
                                Text(
                                    text = event.description,
                                    style = MaterialTheme.typography.bodySmall.copy(color = BentoTextSecondary),
                                    modifier = Modifier.padding(top = 4.dp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.deleteEvent(event.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Event",
                                tint = NeonRed.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Elegant Easter Egg widget showing glowing Air Hockey stylized Rink with progress tracking
@Composable
fun CyberRinkWidget(tasks: List<Task>) {
    val totalCount = tasks.size
    val completedCount = tasks.count { it.isCompleted }
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    GlowCard(color = NeonBlue) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌌 CRITICAL RANGE RINK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BentoTextPurple,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "$completedCount/$totalCount GOAL SHOTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BentoDeepPurple,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rink Stadium canvas drawing!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BentoCardGray)
                    .border(1.dp, BentoCardGrayBorder, RoundedCornerShape(12.dp))
            ) {
                // Background markings of the arena
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Center Line
                    drawLine(
                        color = BentoDeepPurple.copy(alpha = 0.2f),
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                    // Center Circle Goal Rim
                    drawCircle(
                        color = BentoDeepPurple.copy(alpha = 0.15f),
                        radius = 24.dp.toPx(),
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())
                    )
                    // Left Goal Zone (Player Mallet blue/purple)
                    drawArc(
                        color = BentoDeepPurple.copy(alpha = 0.2f),
                        startAngle = -90f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(-16.dp.toPx(), size.height / 2 - 16.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(32.dp.toPx(), 32.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())
                    )
                    // Right Goal Zone (AI Mallet red)
                    drawArc(
                        color = NeonRed.copy(alpha = 0.2f),
                        startAngle = 90f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(size.width - 16.dp.toPx(), size.height / 2 - 16.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(32.dp.toPx(), 32.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())
                    )
                }

                // Player Mallet (Bento Deep Purple) on left, AI Mallet (Neon Red) on right
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 14.dp)
                        .size(24.dp)
                        .background(BentoDeepPurple.copy(alpha = 0.2f), CircleShape)
                        .border(1.5.dp, BentoDeepPurple, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(10.dp)
                            .background(Color.White, CircleShape)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 14.dp)
                        .size(24.dp)
                        .background(NeonRed.copy(alpha = 0.2f), CircleShape)
                        .border(1.5.dp, NeonRed, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(10.dp)
                            .background(Color.White, CircleShape)
                    )
                }

                // Sliding Puck status (progress) representation! Animates beautifully
                val puckOffsetFraction by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "puckAnimation"
                )

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val containerWidth = maxWidth
                    val startOffset = 44.dp
                    val endOffset = containerWidth - 44.dp - 20.dp
                    val targetX = startOffset + (endOffset - startOffset) * puckOffsetFraction

                    Box(
                        modifier = Modifier
                            .offset(x = targetX, y = 38.dp)
                            .size(20.dp)
                    ) {
                        // Glowing puck representation with trailing motion blur gradient shadows
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(BentoDeepPurple.copy(alpha = 0.2f), radius = 14.dp.toPx())
                            drawCircle(Color.White, radius = 6.dp.toPx())
                            drawCircle(BentoDeepPurple, radius = 9.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Track daily progress! Completing task checklist items slides the dynamic focus puck into the AI Goal Zone.",
                style = MaterialTheme.typography.bodySmall.copy(color = BentoTextSecondary, fontSize = 11.sp),
                lineHeight = 15.sp
            )
        }
    }
}

// --- TAB 2: TASKS CHECKLIST VIEW ---
@Composable
fun TasksTab(viewModel: PlannerViewModel) {
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val selectedDateMs by viewModel.selectedDateMs.collectAsStateWithLifecycle()

    val currentFilter = remember { mutableStateOf("All") }
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())

    val filteredTasks = tasks.filter {
        val matchesCategory = currentFilter.value == "All" || it.category.equals(currentFilter.value, ignoreCase = true)
        val matchesDate = it.dueDate == selectedDateMs
        matchesCategory && matchesDate
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Category Row Filters
        CategoryFilterRow(
            selectedFilter = currentFilter.value,
            onFilterSelected = { currentFilter.value = it }
        )

        if (filteredTasks.isEmpty()) {
            EmptyStateDisplay(
                title = "No Pending Tasks",
                description = "Tap the bottom + FAB to create specific high-priority Work or Personal todo items, and check them off to score goal-shots on the stadium rink tracker!"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            items(filteredTasks) { task ->
                GlowCard(color = if (task.isCompleted) NeonYellow else if (task.priority == "High") NeonRed else NeonBlue) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom neon checkbox
                        IconButton(
                            onClick = { viewModel.toggleTaskCompletion(task) },
                            modifier = Modifier
                                .testTag("task_checkbox_${task.id}")
                                .size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.CheckCircle,
                                contentDescription = "Toggle Complete",
                                tint = if (task.isCompleted) BentoDeepPurple else BentoTextSecondary.copy(alpha = 0.2f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (task.isCompleted) BentoTextSecondary else BentoTextPrimary,
                                    textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                )
                            )
                            Row(
                                modifier = Modifier.padding(top = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Tags
                                Text(
                                    text = task.category.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BentoDeepPurple,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp,
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier
                                        .background(BentoDeepPurple.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                )

                                val priorityColor = if (task.priority == "High") NeonRed else if (task.priority == "Medium") NeonYellow else BentoTextSecondary.copy(alpha = 0.5f)
                                Text(
                                    text = task.priority.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = priorityColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                )

                                if (task.dueTime != null) {
                                    Text(
                                        text = "🕒 ${task.dueTime}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = BentoTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                            if (task.description.isNotEmpty()) {
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.bodySmall.copy(color = BentoTextSecondary),
                                    modifier = Modifier.padding(top = 4.dp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.deleteTask(task.id) },
                            modifier = Modifier
                                .testTag("delete_task_${task.id}")
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Task",
                                    tint = NeonRed.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryFilterRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("All", "Work", "Personal", "Fitness", "Study")
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            val isSelected = filter == selectedFilter
            Surface(
                color = if (isSelected) NeonBlue.copy(alpha = 0.15f) else SurfaceNavy,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (isSelected) NeonBlue else CosmicWhite.copy(alpha = 0.15f)),
                modifier = Modifier
                    .height(36.dp)
                    .clickable { onFilterSelected(filter) }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = filter,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (isSelected) NeonBlue else CosmicWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

// --- TAB 3: TARGET PROGRESSIVE GOALS VIEW ---
@Composable
fun GoalsTab(viewModel: PlannerViewModel) {
    val goals by viewModel.allGoals.collectAsStateWithLifecycle()

    if (goals.isEmpty()) {
        EmptyStateDisplay(
            title = "Zero Goal-Shots Configured",
            description = "Map specific, dynamic metrics like 'Optimal Hydration' (Glasses) or 'Exercise Cardio' (Hours), log micro progress regularly, and visualize completion with neon dials!"
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯 ACTIVE GOALS & PROGRESS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = BentoTextPurple,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                    )
                    PuckStatusIndicator(color = BentoDeepPurple, text = "GOAL TRACKER")
                }
            }

            items(goals) { goal ->
                val progressFraction = (goal.currentValue / goal.targetValue).coerceIn(0f, 1f)
                val isCompleted = progressFraction >= 1f

                GlowCard(color = if (isCompleted) NeonYellow else NeonBlue) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = goal.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BentoTextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = goal.category.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isCompleted) BentoTextPurple else BentoDeepPurple,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            IconButton(
                                onClick = { viewModel.deleteGoal(goal.id) },
                                modifier = Modifier
                                    .testTag("delete_goal_${goal.id}")
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete Goal",
                                    tint = NeonRed.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (goal.description.isNotEmpty()) {
                            Text(
                                text = goal.description,
                                style = MaterialTheme.typography.bodySmall.copy(color = BentoTextSecondary),
                                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Custom neon horizontal progress bar representing distance shot
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE6E1E5))
                                .border(1.dp, BentoCardGrayBorder, RoundedCornerShape(8.dp))
                        ) {
                            val barBrush = Brush.horizontalGradient(
                                colors = if (isCompleted) {
                                    listOf(BentoDeepPurple.copy(alpha = 0.8f), BentoDeepPurple)
                                } else {
                                    listOf(BentoDeepPurple.copy(alpha = 0.5f), BentoDeepPurple.copy(alpha = 0.9f))
                                }
                            )

                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(maxWidth * progressFraction)
                                        .background(barBrush, RoundedCornerShape(8.dp))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                  text = "${goal.currentValue} / ${goal.targetValue} ${goal.unit}",
                                  style = MaterialTheme.typography.labelLarge.copy(
                                      color = if (isCompleted) BentoTextPurple else BentoTextPrimary,
                                      fontWeight = FontWeight.Bold
                                  )
                            )

                            // Logging progress increments inside the Card row reactively
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.logGoalProgress(goal, -1f) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BentoTextPrimary),
                                    border = BorderStroke(1.dp, BentoCardGrayBorder),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .testTag("goal_decrement_${goal.id}")
                                        .size(34.dp)
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowDown, "-1")
                                }

                                Button(
                                    onClick = { viewModel.logGoalProgress(goal, 1f) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BentoDeepPurple,
                                        contentColor = BentoWhite
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .testTag("goal_increment_${goal.id}")
                                        .size(34.dp)
                                ) {
                                    Icon(Icons.Filled.Add, "+1")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 4: AURA DYNAMIC AI ADVISOR VIEW ---
@Composable
fun AiAdvisorTab(viewModel: PlannerViewModel) {
    val isGeneratingPlan by viewModel.isGeneratingPlan.collectAsStateWithLifecycle()
    val aiPlanAdvice by viewModel.aiPlanAdvice.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🧠 AURA AI COACH ROOM",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = BentoTextPurple,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                )
            )
            PuckStatusIndicator(color = BentoDeepPurple, text = "INTELLIGENCE")
        }

        GlowCard(color = NeonRed) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Face,
                    contentDescription = "Aura AI Coach",
                    tint = BentoDeepPurple,
                    modifier = Modifier.size(38.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "AURA HIGH-PERFORMANCE COACH",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoTextPrimary
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Let Gemini analyze your current calendar events, checklists, and progressive goals to assemble a friction-free, customized schedule and coaching tips.",
                    style = MaterialTheme.typography.bodySmall.copy(color = BentoTextSecondary),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic API Configuration Check Alerts
        if (!viewModel.isGeminiKeyConfigured) {
            Surface(
                color = BentoCardPink,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BentoCardPurpleBorder.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, "api warn", tint = BentoTextPurple, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Gemini API key is not configured in Secrets Panel yet. You can still utilize the offline calendar timeline, log progressive goals, and slide the stadium puck, but system advice is paused.",
                        style = MaterialTheme.typography.bodySmall.copy(color = BentoTextPrimary),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Trigger advisor button in striking neon red
        Button(
            onClick = { viewModel.generateDynamicAiPlan() },
            colors = ButtonDefaults.buttonColors(containerColor = BentoDeepPurple, contentColor = BentoWhite),
            shape = RoundedCornerShape(16.dp),
            enabled = !isGeneratingPlan && viewModel.isGeminiKeyConfigured,
            modifier = Modifier
                .testTag("ai_generate_button")
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isGeneratingPlan) {
                CircularProgressIndicator(color = BentoWhite, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Analyzing daily metrics...")
            } else {
                Icon(Icons.Filled.PlayArrow, "Start AI planner")
                Spacer(modifier = Modifier.width(8.dp))
                Text("ASSEMBLE DYNAMIC AI PLAN", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Terminal Output Interface Scroll Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(BentoCardGray)
                .border(1.dp, BentoCardGrayBorder, RoundedCornerShape(20.dp))
                .padding(14.dp)
        ) {
            if (aiPlanAdvice != null) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = ">>> AURA OPTIMIZATION OUTPUT:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BentoTextPurple,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = aiPlanAdvice!!,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = BentoTextPrimary,
                            lineHeight = 21.sp
                        )
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "idle terminal",
                        tint = BentoTextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Coaching output terminal",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = BentoTextSecondary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}

// --- POPUP DIALOGS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventDialog(
    selectedDateMs: Long,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Long, String?, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startHour by remember { mutableStateOf("09") }
    var startMin by remember { mutableStateOf("00") }
    var endHour by remember { mutableStateOf("10") }
    var endMin by remember { mutableStateOf("00") }
    var location by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf(NeonBlue.toArgb().toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BentoCardPurpleBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "CREATE CALENDAR EVENT",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = BentoTextPrimary,
                        fontWeight = FontWeight.Black
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        focusedLabelColor = NeonBlue,
                        cursorColor = NeonBlue
                    ),
                    modifier = Modifier
                        .testTag("dialog_event_title")
                        .fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        focusedLabelColor = NeonBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location (e.g. Zoom)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("START TIME (HH:MM)", style = MaterialTheme.typography.labelSmall.copy(color = NeonBlue))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startHour,
                        onValueChange = { startHour = it.take(2) },
                        label = { Text("Hour") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = startMin,
                        onValueChange = { startMin = it.take(2) },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("END TIME (HH:MM)", style = MaterialTheme.typography.labelSmall.copy(color = NeonBlue))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = endHour,
                        onValueChange = { endHour = it.take(2) },
                        label = { Text("Hour") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endMin,
                        onValueChange = { endMin = it.take(2) },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("CANCEL", color = CosmicWhite.copy(alpha = 0.5f))
                    }
                    Button(
                        onClick = {
                            if (title.isNotEmpty()) {
                                val sHour = startHour.toIntOrNull() ?: 9
                                val sMin = startMin.toIntOrNull() ?: 0
                                val eHour = endHour.toIntOrNull() ?: 10
                                val eMin = endMin.toIntOrNull() ?: 0

                                val cal = Calendar.getInstance()
                                cal.timeInMillis = selectedDateMs
                                cal.set(Calendar.HOUR_OF_DAY, sHour)
                                cal.set(Calendar.MINUTE, sMin)
                                val finalStart = cal.timeInMillis

                                cal.set(Calendar.HOUR_OF_DAY, eHour)
                                cal.set(Calendar.MINUTE, eMin)
                                val finalEnd = cal.timeInMillis

                                onConfirm(
                                    title,
                                    description,
                                    finalStart,
                                    finalEnd,
                                    if (location.isEmpty()) null else location,
                                    0xFF00E5FF.toInt() // NeonBlue defaults
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = DarkBackground),
                        modifier = Modifier
                            .testTag("dialog_confirm_event")
                            .weight(1f)
                    ) {
                        Text("CREATE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateTaskDialog(
    selectedDateMs: Long,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Work") }
    var priority by remember { mutableStateOf("Medium") }
    var dueTimeHour by remember { mutableStateOf("") }
    var dueTimeMin by remember { mutableStateOf("") }

    val priorities = listOf("High", "Medium", "Low")
    val categories = listOf("Work", "Personal", "Fitness", "Study")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BentoCardPurpleBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "CREATE TODO TASK",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = BentoTextPrimary,
                        fontWeight = FontWeight.Black
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        focusedLabelColor = NeonBlue
                    ),
                    modifier = Modifier
                        .testTag("dialog_task_title")
                        .fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Task Description") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("PRIORITY", style = MaterialTheme.typography.labelSmall.copy(color = NeonBlue))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    priorities.forEach { p ->
                        val isSelected = p == priority
                        Surface(
                            color = if (isSelected) NeonBlue.copy(alpha = 0.15f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) NeonBlue else CosmicWhite.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clickable { priority = p }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(p, style = MaterialTheme.typography.labelMedium.copy(color = CosmicWhite))
                            }
                        }
                    }
                }

                Text("CATEGORY", style = MaterialTheme.typography.labelSmall.copy(color = NeonBlue))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat == category
                        Surface(
                            color = if (isSelected) NeonBlue.copy(alpha = 0.15f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) NeonBlue else CosmicWhite.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clickable { category = cat }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(cat, style = MaterialTheme.typography.labelSmall.copy(color = CosmicWhite, fontSize = 10.sp))
                            }
                        }
                    }
                }

                Text("OPTIONAL TIME SPECIFICATION (HH:MM)", style = MaterialTheme.typography.labelSmall.copy(color = NeonBlue))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dueTimeHour,
                        onValueChange = { dueTimeHour = it.take(2) },
                        label = { Text("Hour") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueTimeMin,
                        onValueChange = { dueTimeMin = it.take(2) },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("CANCEL", color = CosmicWhite.copy(alpha = 0.5f))
                    }
                    Button(
                        onClick = {
                            if (title.isNotEmpty()) {
                                val timeStr = if (dueTimeHour.isNotEmpty()) {
                                    val hr = dueTimeHour.padStart(2, '0')
                                    val mn = dueTimeMin.padStart(2, '0').ifEmpty { "00" }
                                    "$hr:$mn"
                                } else null
                                onConfirm(title, description, timeStr, priority, category)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = DarkBackground),
                        modifier = Modifier
                            .testTag("dialog_confirm_task")
                            .weight(1f)
                    ) {
                        Text("CREATE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Float, String, String, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetVal by remember { mutableStateOf("8") }
    var unit by remember { mutableStateOf("glasses") }
    var category by remember { mutableStateOf("Wellness") }

    val categories = listOf("Wellness", "Productivity", "Learning", "Fitness")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BentoCardPurpleBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "CREATE TARGET GOAL",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = BentoTextPrimary,
                        fontWeight = FontWeight.Black
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title (e.g. Drink Water)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        focusedLabelColor = NeonBlue
                    ),
                    modifier = Modifier
                        .testTag("dialog_goal_title")
                        .fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short Description") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = targetVal,
                        onValueChange = { targetVal = it },
                        label = { Text("Target Metric") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (e.g. glasses)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("GOAL CATEGORY", style = MaterialTheme.typography.labelSmall.copy(color = NeonBlue))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat == category
                        Surface(
                            color = if (isSelected) NeonBlue.copy(alpha = 0.15f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) NeonBlue else CosmicWhite.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clickable { category = cat }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(cat, style = MaterialTheme.typography.labelSmall.copy(color = CosmicWhite, fontSize = 9.sp))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("CANCEL", color = CosmicWhite.copy(alpha = 0.5f))
                    }
                    Button(
                        onClick = {
                            if (title.isNotEmpty()) {
                                val targetNum = targetVal.toFloatOrNull() ?: 5.0f
                                // Give a default goal deadline 7 days from now
                                val deadline = System.currentTimeMillis() + 7 * 24 * 3600000L
                                onConfirm(title, description, targetNum, unit, category, deadline)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = DarkBackground),
                        modifier = Modifier
                            .testTag("dialog_confirm_goal")
                            .weight(1f)
                    ) {
                        Text("CREATE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateDisplay(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.DateRange,
            contentDescription = "No data",
            tint = CosmicWhite.copy(alpha = 0.15f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = CosmicWhite.copy(alpha = 0.6f)
            ),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}
