package com.example.budgetapp

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.budgetapp.ui.theme.BudgetAppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Top-level app destinations shown in bottom navigation. */
enum class AppScreen {
    Overview, ThisMonth, Account, Setting
}

/** Primary colors and shape constants shared across screens. */
private val AppBlue = Color(0xFF4A9DD4)
private val CardWhite = Color(0xFFF8F8F8)
private val SoftText = Color(0xFF2D2D4F)
private val SurfaceRadius = 30.dp
private val TileRadius = 18.dp
private val PillRadius = 30.dp

/**
 * Reusable outlined text field styled with black text/labels
 * to improve readability across forms.
 */
@Composable
private fun BlackTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label, color = Color.Black) },
        singleLine = singleLine,
        readOnly = readOnly,
        textStyle = TextStyle(color = Color.Black),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedLabelColor = Color.Black,
            unfocusedLabelColor = Color.Black,
            cursorColor = Color.Black
        )
    )
}

/** Main activity entry point that wires Compose with the ViewModel. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dao = BudgetDatabase.get(this).dao()
        setContent {
            BudgetAppTheme {
                val vm: BudgetViewModel = viewModel(factory = BudgetViewModel.factory(dao))
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BudgetAppRoot(
                        vm = vm,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/** Switches between auth flow and main app flow based on login state. */
@Composable
fun BudgetAppRoot(vm: BudgetViewModel, modifier: Modifier = Modifier) {
    val nullL = null
    if (vm.currentUser == nullL) {
        AuthScreen(vm = vm, modifier = modifier)
    } else {
        MainScreen(vm = vm, modifier = modifier)
    }
}

/** Login/register screen used before a user account is active. */
@Composable
fun AuthScreen(vm: BudgetViewModel, modifier: Modifier = Modifier) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var registerMode by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBlue)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "Welcome to BalancedBuddy!",
                    style = MaterialTheme.typography.titleLarge,
                    color = SoftText,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (registerMode) "Sign up" else "Login",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SoftText
                )
                Spacer(Modifier.height(14.dp))
                BlackTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Username",
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                BlackTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Password",
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (registerMode) vm.register(username, password) else vm.login(username, password)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (registerMode) "Create account" else "Log in")
                }
                TextButton(onClick = { registerMode = !registerMode }) {
                    Text(if (registerMode) "Already have an account? Log in" else "Need an account? Sign up")
                }
                if (vm.errorMessage.isNotBlank()) {
                    Text(vm.errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/** Main app scaffold with shared bottom navigation. */
@Composable
fun MainScreen(vm: BudgetViewModel, modifier: Modifier = Modifier) {
    var selectedScreen by remember { mutableStateOf(AppScreen.Overview) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppBlue,
        bottomBar = {
            Card(
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomItem("Overview", Icons.Outlined.Home, selectedScreen == AppScreen.Overview) {
                        selectedScreen = AppScreen.Overview
                    }
                    BottomItem("This Month", Icons.Outlined.DateRange, selectedScreen == AppScreen.ThisMonth) {
                        selectedScreen = AppScreen.ThisMonth
                    }
                    BottomItem("Account", Icons.Outlined.AccountCircle, selectedScreen == AppScreen.Account) {
                        selectedScreen = AppScreen.Account
                    }
                    BottomItem("Setting", Icons.Outlined.Settings, selectedScreen == AppScreen.Setting) {
                        selectedScreen = AppScreen.Setting
                    }
                }
            }
        }
    ) { padding ->
        when (selectedScreen) {
            AppScreen.Overview -> OverviewScreen(vm, Modifier.padding(padding))
            AppScreen.ThisMonth -> ThisMonthScreen(vm, Modifier.padding(padding))
            AppScreen.Account -> AccountScreen(vm, Modifier.padding(padding))
            AppScreen.Setting -> SettingsScreen(vm, Modifier.padding(padding))
        }
    }
}

/** Bottom navigation item used by MainScreen. */
@Composable
fun BottomItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) AppBlue else Color(0xFFADB3BC),
            modifier = Modifier.size(22.dp)
        )
        Text(
            label,
            color = if (selected) AppBlue else Color(0xFFADB3BC),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * Shared page shell:
 * - top blue header with title
 * - rounded white content surface
 */
@Composable
fun ScreenShell(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(AppBlue)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
        Card(
            shape = RoundedCornerShape(topStart = SurfaceRadius, topEnd = SurfaceRadius),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                content()
            }
        }
    }
}

/** Home/overview screen showing budget summary and logout action. */
@Composable
fun OverviewScreen(vm: BudgetViewModel, modifier: Modifier = Modifier) {
    ScreenShell("Home", modifier) {
        Text("Welcome", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 50.sp), color = SoftText, fontWeight = FontWeight.ExtraBold)
        Text(vm.currentUsername.ifBlank { "User" }, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 34.sp), color = SoftText)
        Spacer(Modifier.height(14.dp))
        Card(
            shape = RoundedCornerShape(TileRadius),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Expenses", color = Color.Gray, fontSize = 14.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Left to spend\nR${"%,.0f".format((vm.maxMonthlyGoal - vm.monthlySpent).coerceAtLeast(0.0))}", fontSize = 16.sp, color = SoftText, fontWeight = FontWeight.Medium)
                    Text("Monthly budget\nR${"%,.0f".format(vm.maxMonthlyGoal)}", fontSize = 16.sp, color = SoftText, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = { vm.logout() }) { Text("Log out") }
    }
}

/** "This Month" screen for period filters and expense/payment tabs. */
@Composable
fun ThisMonthScreen(vm: BudgetViewModel, modifier: Modifier = Modifier) {
    var tab by remember { mutableStateOf(0) }
    var fromDate by remember { mutableStateOf(currentMonthStartString()) }
    var toDate by remember { mutableStateOf(currentDateString()) }
    ScreenShell("This Month", modifier) {
        Text("Selected period", style = MaterialTheme.typography.titleMedium, color = SoftText)
        Spacer(Modifier.height(8.dp))
        BlackTextField(
            value = fromDate,
            onValueChange = { fromDate = it },
            modifier = Modifier.fillMaxWidth(),
            label = "From yyyy-MM-dd"
        )
        Spacer(Modifier.height(8.dp))
        BlackTextField(
            value = toDate,
            onValueChange = { toDate = it },
            modifier = Modifier.fillMaxWidth(),
            label = "To yyyy-MM-dd"
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { vm.loadPeriodData(fromDate, toDate) },
            shape = RoundedCornerShape(PillRadius),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Load period expenses") }
        Spacer(Modifier.height(10.dp))
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Expenses") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Payments") })
        }
        Spacer(Modifier.height(10.dp))
        when (tab) {
            0 -> ExpenseScreen(vm)
            else -> PaymentsSummary(vm)
        }
    }
}

/** Expense entry form and period expense listing. */
@Composable
fun ExpenseScreen(vm: BudgetViewModel) {
    var amount by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(currentDateString()) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableLongStateOf(0L) }
    var pickedPhoto by remember { mutableStateOf<Uri?>(null) }
    // Launches Android photo picker and stores selected image URI.
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> pickedPhoto = uri }
    )

    Text("R${"%,.0f".format(vm.monthlySpent)}", style = MaterialTheme.typography.displaySmall.copy(fontSize = 52.sp), color = SoftText, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    BlackTextField(value = amount, onValueChange = { amount = it }, modifier = Modifier.fillMaxWidth(), label = "Amount")
    BlackTextField(value = dateText, onValueChange = { dateText = it }, modifier = Modifier.fillMaxWidth(), label = "Date yyyy-MM-dd")
    BlackTextField(value = startTime, onValueChange = { startTime = it }, modifier = Modifier.fillMaxWidth(), label = "Start HH:mm")
    BlackTextField(value = endTime, onValueChange = { endTime = it }, modifier = Modifier.fillMaxWidth(), label = "End HH:mm")
    BlackTextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth(), label = "Description")
    CategoryDropdown(
        categories = vm.categories,
        selectedCategoryId = selectedCategoryId,
        onSelect = { selectedCategoryId = it }
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }, shape = RoundedCornerShape(PillRadius), modifier = Modifier.weight(1f)) { Text(if (pickedPhoto == null) "Attach photo" else "Photo ready") }
        Button(onClick = {
            vm.addExpense(
                amount = amount,
                dateText = dateText,
                startTime = startTime,
                endTime = endTime,
                description = description,
                categoryId = selectedCategoryId,
                photoUri = pickedPhoto?.toString()
            )
            amount = ""
            description = ""
            pickedPhoto = null
        }, shape = RoundedCornerShape(PillRadius), modifier = Modifier.weight(1f)) { Text("Save") }
    }
    if (vm.errorMessage.isNotBlank()) {
        Text(vm.errorMessage, color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(10.dp))
    // Shows category totals for the selected date range.
    vm.categoryTotals.forEach { ct ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(ct.categoryName, color = SoftText, fontWeight = FontWeight.Medium)
                Text("R${"%,.2f".format(ct.total)}", color = SoftText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Text("Expenses in selected period", style = MaterialTheme.typography.titleMedium, color = SoftText)
    Spacer(Modifier.height(6.dp))
    // Shows each expense entry in the selected period, including photo preview.
    vm.expensesInRange.forEach { expense ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(expense.description, color = SoftText, fontWeight = FontWeight.SemiBold)
                Text("${formatDate(expense.dateMillis)} • ${expense.categoryName}", color = Color.Gray)
                Text("R${"%,.2f".format(expense.amount)}", color = SoftText, fontWeight = FontWeight.Medium)
                if (!expense.photoUri.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    AsyncImage(
                        model = expense.photoUri,
                        contentDescription = "Expense photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
            }
        }
    }
}

/** Payments summary list for expenses in current selected range. */
@Composable
fun PaymentsSummary(vm: BudgetViewModel) {
    val total = vm.categoryTotals.sumOf { it.total }
    Text("Your monthly payments", color = Color.Gray, fontSize = 14.sp)
    Text("R${"%,.2f".format(total)}", style = MaterialTheme.typography.displaySmall.copy(fontSize = 54.sp), color = SoftText, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    vm.expensesInRange.take(8).forEach {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(it.description, color = SoftText)
                Text(it.categoryName, color = Color.Gray)
            }
            Text("R${"%,.2f".format(it.amount)}", color = SoftText)
        }
        HorizontalDivider()
    }
}

/** Legacy savings summary section (currently not surfaced by tabs). */
@Composable
fun SavingsSummary(vm: BudgetViewModel) {
    val saved = (vm.maxMonthlyGoal - vm.monthlySpent).coerceAtLeast(0.0)
    Text("Savings", color = Color.Gray)
    Text("R${"%,.2f".format(saved)}", style = MaterialTheme.typography.displaySmall, color = SoftText)
    Spacer(Modifier.height(10.dp))
    Button(onClick = {}, shape = RoundedCornerShape(PillRadius), modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Text("Transfer to Main Account")
    }
    Spacer(Modifier.height(14.dp))
    Text("Savings history", style = MaterialTheme.typography.titleLarge, color = SoftText)
    Spacer(Modifier.height(8.dp))
    Card(
        border = BorderStroke(1.dp, Color(0xFFE6E6E6)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            listOf("January 2026", "November 2025", "August 2025", "June 2025").forEach {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(it, color = SoftText)
                    Text("+ R${"%,.0f".format(saved / 4.0)}", color = SoftText)
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

/** Account page with edit details, history, and delete-account actions. */
@Composable
fun AccountScreen(vm: BudgetViewModel, modifier: Modifier = Modifier) {
    var showEditDetails by remember { mutableStateOf(false) }
    var showAccountHistory by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var updatedName by remember { mutableStateOf(vm.currentUsername) }
    var updatedPassword by remember { mutableStateOf("") }

    ScreenShell("Account", modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE5E5E5))
            ) {
                Icon(
                    Icons.Outlined.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(116.dp),
                    tint = SoftText
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(vm.currentUsername.ifBlank { "User Account" }, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 38.sp), color = SoftText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            AccountAction("Edit Details") {
                updatedName = vm.currentUsername
                updatedPassword = ""
                showEditDetails = !showEditDetails
                if (showEditDetails) {
                    showAccountHistory = false
                    showDeleteConfirm = false
                }
            }
            // Inline edit form for username/password updates.
            if (showEditDetails) {
                Spacer(Modifier.height(6.dp))
                BlackTextField(
                    value = updatedName,
                    onValueChange = { updatedName = it },
                    label = "New username",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                BlackTextField(
                    value = updatedPassword,
                    onValueChange = { updatedPassword = it },
                    label = "New password",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.updateAccountDetails(updatedName, updatedPassword) },
                    shape = RoundedCornerShape(PillRadius),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save details")
                }
            }
            AccountAction("Account History") {
                showAccountHistory = !showAccountHistory
                if (showAccountHistory) {
                    showEditDetails = false
                    showDeleteConfirm = false
                }
            }
            // Displays basic account metadata.
            if (showAccountHistory) {
                val createdText = if (vm.accountCreatedAtMillis > 0L) {
                    formatDate(vm.accountCreatedAtMillis)
                } else {
                    "Not available"
                }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    border = BorderStroke(1.dp, Color(0xFFE7E7E7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Account created on", color = Color.Gray)
                        Text(createdText, color = SoftText, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            AccountAction("Delete Account") {
                showDeleteConfirm = !showDeleteConfirm
                if (showDeleteConfirm) {
                    showEditDetails = false
                    showAccountHistory = false
                }
            }
            // Destructive action confirmation UI.
            if (showDeleteConfirm) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    border = BorderStroke(1.dp, Color(0xFFE7E7E7)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "Are you sure you want to delete your account?",
                            color = SoftText,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showDeleteConfirm = false },
                                shape = RoundedCornerShape(PillRadius),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("No")
                            }
                            Button(
                                onClick = {
                                    vm.deleteCurrentAccount()
                                    showDeleteConfirm = false
                                },
                                shape = RoundedCornerShape(PillRadius),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Yes")
                            }
                        }
                    }
                }
            }
            if (vm.errorMessage.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(vm.errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** Reusable clickable row used in account actions list. */
@Composable
fun AccountAction(text: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(PillRadius),
        border = BorderStroke(1.dp, Color(0xFFE7E7E7)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = SoftText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = ">",
                color = SoftText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

/** Settings page for category/goal management and badge display. */
@Composable
fun SettingsScreen(vm: BudgetViewModel, modifier: Modifier = Modifier) {
    var categoryName by remember { mutableStateOf("") }
    var minGoal by remember { mutableStateOf(vm.minMonthlyText) }
    var maxGoal by remember { mutableStateOf(vm.maxMonthlyText) }
    ScreenShell("Setting", modifier) {
        Text("Categories", style = MaterialTheme.typography.titleLarge)
        BlackTextField(
            value = categoryName,
            onValueChange = { categoryName = it },
            modifier = Modifier.fillMaxWidth(),
            label = "New category"
        )
        Button(onClick = {
            vm.addCategory(categoryName)
            categoryName = ""
        }) { Text("Add Category") }
        Spacer(Modifier.height(12.dp))
        Text("Monthly Goal", style = MaterialTheme.typography.titleLarge)
        BlackTextField(value = minGoal, onValueChange = { minGoal = it }, modifier = Modifier.fillMaxWidth(), label = "Min")
        BlackTextField(value = maxGoal, onValueChange = { maxGoal = it }, modifier = Modifier.fillMaxWidth(), label = "Max")
        Button(onClick = { vm.saveMonthlyGoal(minGoal, maxGoal) }) { Text("Save Goal") }
        Spacer(Modifier.height(12.dp))
        Text("Badges", style = MaterialTheme.typography.titleLarge)
        vm.badges.take(5).forEach {
            Text("- ${it.name} (${formatDate(it.earnedAtMillis)})")
        }
        TextButton(onClick = { vm.logout() }) { Text("Logout") }
    }
}

/** Lightweight custom category picker used by expense form. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Select category"
    Column {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            label = { Text("Category", color = Color.Black) },
            textStyle = TextStyle(color = Color.Black),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedLabelColor = Color.Black,
                unfocusedLabelColor = Color.Black,
                cursorColor = Color.Black
            )
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { expanded = !expanded }) { Text("Choose Category") }
        if (expanded) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F6F6))
            ) {
                categories.forEach {
                    Text(
                        text = it.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(it.id)
                                expanded = false
                            }
                            .padding(10.dp)
                    )
                }
            }
        }
    }
}

/** Stacked daily spending chart by category (not currently placed on a screen). */
@Composable
fun DailySpendingChart(expenses: List<ExpenseWithCategory>, categories: List<CategoryEntity>) {
    val perDay = expenses.groupBy { formatDate(it.dateMillis) }
    val days = perDay.keys.sorted()
    if (days.isEmpty()) {
        Text("No data to plot")
        return
    }
    val colors = listOf(Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFF4511E), Color(0xFF8E24AA), Color(0xFFFFB300))
    val maxTotal = days.maxOf { day -> perDay[day]?.sumOf { it.amount } ?: 0.0 }.coerceAtLeast(1.0)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(vertical = 8.dp)
            .background(Color(0xFFF4F4F4))
    ) {
        val barWidth = size.width / (days.size * 1.3f)
        days.forEachIndexed { index, day ->
            val dayExpenses = perDay[day].orEmpty()
            var usedHeight = 0f
            val x = (index + 1) * barWidth * 1.3f
            categories.forEachIndexed { catIndex, cat ->
                val categoryAmount = dayExpenses.filter { it.categoryName == cat.name }.sumOf { it.amount }
                if (categoryAmount > 0) {
                    val h = (categoryAmount / maxTotal).toFloat() * size.height * 0.9f
                    drawLine(
                        color = colors[catIndex % colors.size],
                        start = Offset(x, size.height - usedHeight),
                        end = Offset(x, size.height - usedHeight - h),
                        strokeWidth = barWidth,
                        cap = StrokeCap.Butt
                    )
                    usedHeight += h
                }
            }
        }
    }
}


/**
 * App ViewModel:
 * - holds UI state
 * - validates user input
 * - coordinates database operations
 */
class BudgetViewModel(private val dao: BudgetDao) : ViewModel() {
    var currentUser by mutableStateOf<Long?>(null)
        private set
    var currentUsername by mutableStateOf("")
        private set
    var accountCreatedAtMillis by mutableStateOf(0L)
        private set
    var categories = mutableStateListOf<CategoryEntity>()
        private set
    var expensesInRange = mutableStateListOf<ExpenseWithCategory>()
        private set
    var categoryTotals = mutableStateListOf<CategoryTotal>()
        private set
    var badges = mutableStateListOf<BadgeEntity>()
        private set

    var errorMessage by mutableStateOf("")
        private set
    var minMonthlyGoal by mutableStateOf(0.0)
        private set
    var maxMonthlyGoal by mutableStateOf(0.0)
        private set
    var minMonthlyText by mutableStateOf("0")
        private set
    var maxMonthlyText by mutableStateOf("0")
        private set
    var monthlySpent by mutableStateOf(0.0)
        private set
    var categoryBudgetsDisplay = mutableStateListOf<String>()
        private set
    var categoryLimitMap by mutableStateOf<Map<String, Double>>(emptyMap())
        private set

    /** Registers a new user and logs in immediately on success. */
    fun register(username: String, password: String) {
        val u = username.trim()
        if (u.length < 3 || password.length < 4) {
            errorMessage = "Username or password is too short."
            return
        }
        viewModelScope.launch {
            val existing = dao.getUserByUsername(u)
            if (existing != null) {
                errorMessage = "Username already exists."
                return@launch
            }
            val id = dao.insertUser(
                UserEntity(
                    username = u,
                    passwordHash = hashPassword(password),
                    createdAtMillis = System.currentTimeMillis()
                )
            )
            loginById(id, u)
            errorMessage = ""
        }
    }

    /** Authenticates existing user credentials. */
    fun login(username: String, password: String) {
        val u = username.trim()
        if (u.isBlank() || password.isBlank()) {
            errorMessage = "Username and password are required."
            return
        }
        viewModelScope.launch {
            val user = dao.getUserByUsername(u)
            if (user == null || user.passwordHash != hashPassword(password)) {
                errorMessage = "Invalid credentials."
                return@launch
            }
            loginById(user.id, user.username)
            errorMessage = ""
        }
    }

    /** Clears all in-memory user/session-specific state. */
    fun logout() {
        currentUser = null
        currentUsername = ""
        accountCreatedAtMillis = 0L
        categories.clear()
        expensesInRange.clear()
        categoryTotals.clear()
        badges.clear()
        categoryBudgetsDisplay.clear()
        categoryLimitMap = emptyMap()
        monthlySpent = 0.0
        minMonthlyGoal = 0.0
        maxMonthlyGoal = 0.0
    }

    /** Updates username/password from Account -> Edit Details. */
    fun updateAccountDetails(username: String, password: String) {
        val userId = currentUser ?: return
        val updatedUsername = username.trim()
        if (updatedUsername.length < 3 || password.length < 4) {
            errorMessage = "Username or password is too short."
            return
        }
        viewModelScope.launch {
            val existing = dao.getUserByUsername(updatedUsername)
            if (existing != null && existing.id != userId) {
                errorMessage = "Username already exists."
                return@launch
            }
            dao.updateUserCredentials(userId, updatedUsername, hashPassword(password))
            currentUsername = updatedUsername
            errorMessage = ""
        }
    }

    /** Deletes current user account and logs out afterwards. */
    fun deleteCurrentAccount() {
        val userId = currentUser ?: return
        viewModelScope.launch {
            dao.deleteUserById(userId)
            logout()
            errorMessage = ""
        }
    }

    /** Adds a new expense category for the current user. */
    fun addCategory(name: String) {
        val userId = currentUser ?: return
        val n = name.trim()
        if (n.isBlank()) {
            errorMessage = "Category name cannot be empty."
            return
        }
        viewModelScope.launch {
            try {
                dao.insertCategory(CategoryEntity(userId = userId, name = n))
                refreshAll()
                errorMessage = ""
            } catch (_: Exception) {
                errorMessage = "Could not add category."
            }
        }
    }

    /** Validates and saves a new expense entry. */
    fun addExpense(
        amount: String,
        dateText: String,
        startTime: String,
        endTime: String,
        description: String,
        categoryId: Long,
        photoUri: String?
    ) {
        val userId = currentUser ?: return
        val amt = amount.toDoubleOrNull()
        if (amt == null || amt <= 0) {
            errorMessage = "Amount must be a positive number."
            return
        }
        val dateMillis = parseDate(dateText)
        if (dateMillis == null) {
            errorMessage = "Date must be yyyy-MM-dd."
            return
        }
        if (!isTime(startTime) || !isTime(endTime)) {
            errorMessage = "Time must be HH:mm."
            return
        }
        if (description.trim().isBlank()) {
            errorMessage = "Description is required."
            return
        }
        if (categoryId <= 0) {
            errorMessage = "Choose a category."
            return
        }
        viewModelScope.launch {
            dao.insertExpense(
                ExpenseEntity(
                    userId = userId,
                    categoryId = categoryId,
                    amount = amt,
                    dateMillis = dateMillis,
                    startTime = startTime,
                    endTime = endTime,
                    description = description.trim(),
                    photoUri = photoUri
                )
            )
            evaluateBadges(userId)
            refreshAll()
            errorMessage = ""
        }
    }

    /** Saves min/max monthly goal values. */
    fun saveMonthlyGoal(minGoal: String, maxGoal: String) {
        val userId = currentUser ?: return
        val min = minGoal.toDoubleOrNull()
        val max = maxGoal.toDoubleOrNull()
        if (min == null || max == null || min < 0 || max <= 0 || min > max) {
            errorMessage = "Enter valid min/max goals."
            return
        }
        viewModelScope.launch {
            dao.upsertBudgetGoal(BudgetGoalEntity(userId = userId, minMonthly = min, maxMonthly = max))
            refreshAll()
            errorMessage = ""
        }
    }

    /** Saves category-specific monthly budget cap. */
    fun saveCategoryBudget(categoryId: Long, maxAmount: String) {
        val userId = currentUser ?: return
        val max = maxAmount.toDoubleOrNull()
        if (categoryId <= 0 || max == null || max <= 0) {
            errorMessage = "Select category and valid amount."
            return
        }
        viewModelScope.launch {
            dao.upsertCategoryBudget(CategoryBudgetEntity(userId = userId, categoryId = categoryId, maxAmount = max))
            refreshAll()
            errorMessage = ""
        }
    }

    /** Loads expenses/totals for a user-selected date period. */
    fun loadPeriodData(fromDate: String, toDate: String) {
        val userId = currentUser ?: return
        val from = parseDate(fromDate)
        val to = parseDate(toDate)?.plus(86_399_999L)
        if (from == null || to == null || from > to) {
            errorMessage = "Invalid date range."
            return
        }
        viewModelScope.launch {
            val expenses = dao.getExpensesInRange(userId, from, to)
            val totals = dao.getCategoryTotals(userId, from, to)
            expensesInRange.clear()
            expensesInRange.addAll(expenses)
            categoryTotals.clear()
            categoryTotals.addAll(totals)
            errorMessage = ""
        }
    }

    /** Applies authenticated user identity and refreshes dashboard data. */
    private suspend fun loginById(userId: Long, username: String) {
        currentUser = userId
        currentUsername = username
        accountCreatedAtMillis = dao.getUserById(userId)?.createdAtMillis ?: 0L
        refreshAll()
    }

    /** Refreshes all dashboard/account state from database. */
    private suspend fun refreshAll() {
        val userId = currentUser ?: return
        val cats = dao.getCategories(userId)
        categories.clear()
        categories.addAll(cats)
        // Build current month time bounds for default dashboard range.
        val now = Calendar.getInstance()
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val monthEnd = now.timeInMillis
        monthlySpent = dao.getTotalSpent(userId, monthStart, monthEnd)
        val goals = dao.getBudgetGoal(userId)
        minMonthlyGoal = goals?.minMonthly ?: 0.0
        maxMonthlyGoal = goals?.maxMonthly ?: 0.0
        minMonthlyText = minMonthlyGoal.toString()
        maxMonthlyText = maxMonthlyGoal.toString()
        val budgets = dao.getCategoryBudgets(userId)
        val catMap = cats.associateBy { it.id }
        categoryBudgetsDisplay.clear()
        categoryBudgetsDisplay.addAll(budgets.mapNotNull { b ->
            val name = catMap[b.categoryId]?.name ?: return@mapNotNull null
            "$name: ${"%.2f".format(b.maxAmount)}"
        })
        categoryLimitMap = budgets.mapNotNull { b ->
            val name = catMap[b.categoryId]?.name ?: return@mapNotNull null
            name to b.maxAmount
        }.toMap()
        badges.clear()
        badges.addAll(dao.getBadges(userId))
        val from = monthStart
        val to = monthEnd
        expensesInRange.clear()
        expensesInRange.addAll(dao.getExpensesInRange(userId, from, to))
        categoryTotals.clear()
        categoryTotals.addAll(dao.getCategoryTotals(userId, from, to))
    }

    /** Awards milestone badges based on spending and logging activity. */
    private suspend fun evaluateBadges(userId: Long) {
        val cal = Calendar.getInstance()
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val monthSpent = dao.getTotalSpent(userId, monthStart, cal.timeInMillis)
        val goal = dao.getBudgetGoal(userId)
        if (goal != null && monthSpent <= goal.maxMonthly) {
            dao.insertBadge(BadgeEntity(userId = userId, name = "On Budget", earnedAtMillis = System.currentTimeMillis()))
        }
        val entries = dao.getExpensesInRange(userId, monthStart, cal.timeInMillis)
        if (entries.size >= 10) {
            dao.insertBadge(BadgeEntity(userId = userId, name = "Consistent Logger", earnedAtMillis = System.currentTimeMillis()))
        }
    }

    companion object {
        /** ViewModel factory to inject DAO into Compose viewModel(). */
        fun factory(dao: BudgetDao): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BudgetViewModel(dao) as T
                }
            }
    }
}

/** Parses yyyy-MM-dd date text into epoch millis. */
fun parseDate(text: String): Long? = try {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(text)?.time
} catch (_: Exception) {
    null
}

/** Returns today's date in yyyy-MM-dd format. */
fun currentDateString(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())

/** Returns first day of current month as yyyy-MM-dd string. */
fun currentMonthStartString(): String {
    val c = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.timeInMillis)
}

/** Formats epoch millis as yyyy-MM-dd for display. */
fun formatDate(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(millis)

/** Validates time string in HH:mm format (24-hour). */
fun isTime(value: String): Boolean = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$").matches(value)

/** Preview placeholder shown in Android Studio Compose preview. */
@Preview(showBackground = true)
@Composable
fun PreviewAppShell() {
    BudgetAppTheme {
        Text("Budget app preview")
    }
}