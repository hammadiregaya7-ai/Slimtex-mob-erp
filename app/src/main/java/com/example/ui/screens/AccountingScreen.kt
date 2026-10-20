package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.Account
import com.example.data.entity.AccountType
import com.example.data.entity.CommercialOrder
import com.example.data.entity.CommercialOrderItem
import com.example.data.entity.CommercialStatus
import com.example.data.entity.CommercialType
import com.example.data.entity.Customer
import com.example.data.entity.CustomerAccountType
import com.example.data.entity.InventoryItem
import com.example.data.entity.ItemCategory
import com.example.data.entity.JournalEntry
import com.example.data.entity.JournalLine
import com.example.data.entity.NormalBalance
import com.example.ui.theme.AmberDark
import com.example.ui.theme.AmberLight
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.viewmodel.ErpViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingScreen(
    viewModel: ErpViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val journalEntries by viewModel.journalEntries.collectAsStateWithLifecycle()
    val commercialOrders by viewModel.commercialOrders.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0: COA, 1: Clients & Customers, 2: Sales & Purchasing, 3: General Ledger

    // Dialog controllers
    var showManualJournalDialog by remember { mutableStateOf(false) }
    var showCreateCommercialDialog by remember { mutableStateOf(false) }
    var preselectedCustomerForInvoice by remember { mutableStateOf<Customer?>(null) }
    var orderToRecordPayment by remember { mutableStateOf<CommercialOrder?>(null) }

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var customerForDirectPayment by remember { mutableStateOf<Customer?>(null) }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("accounting_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Accounting & Finance",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Chart of Accounts, Clients Directory, Invoicing, and General Ledger",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (selectedTab) {
                    1 -> {
                        Button(
                            onClick = {
                                customerToEdit = null
                                showAddCustomerDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                            modifier = Modifier.testTag("btn_new_customer")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New Client")
                        }
                    }
                    2 -> {
                        Button(
                            onClick = {
                                preselectedCustomerForInvoice = null
                                showCreateCommercialDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                            modifier = Modifier.testTag("btn_new_commercial_order")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New Order/Invoice")
                        }
                    }
                    3 -> {
                        Button(
                            onClick = { showManualJournalDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("btn_new_journal_entry")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Post Entry")
                        }
                    }
                }
            }
        }

        // Module Tabs (COA, Clients, Invoices/POs, General Ledger)
        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Accounts (${accounts.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Clients (${customers.size})") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Invoices & POs (${commercialOrders.size})") }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("General Ledger (${journalEntries.size})") }
            )
        }

        // Content Body
        when (selectedTab) {
            0 -> ChartOfAccountsView(accounts = accounts, currencyFormat = currencyFormat)
            1 -> ClientsView(
                customers = customers,
                currencyFormat = currencyFormat,
                onAddClient = {
                    customerToEdit = null
                    showAddCustomerDialog = true
                },
                onEditClient = {
                    customerToEdit = it
                    showAddCustomerDialog = true
                },
                onDeleteClient = { viewModel.deleteCustomer(it.id) },
                onIssueInvoice = { client ->
                    preselectedCustomerForInvoice = client
                    showCreateCommercialDialog = true
                },
                onRecordPayment = { client ->
                    customerForDirectPayment = client
                }
            )
            2 -> CommercialOrdersView(
                orders = commercialOrders,
                currencyFormat = currencyFormat,
                onRecordPayment = { orderToRecordPayment = it }
            )
            3 -> GeneralLedgerView(entries = journalEntries, currencyFormat = currencyFormat)
        }
    }

    // Add / Edit Customer Dialog
    if (showAddCustomerDialog) {
        AddEditCustomerDialog(
            customer = customerToEdit,
            onDismiss = {
                showAddCustomerDialog = false
                customerToEdit = null
            },
            onSave = { code, name, accountType, contactPerson, email, phone, address, creditLimit, terms, notes ->
                viewModel.saveCustomer(
                    id = customerToEdit?.id ?: 0L,
                    code = code,
                    name = name,
                    accountType = accountType,
                    contactPerson = contactPerson,
                    email = email,
                    phone = phone,
                    address = address,
                    creditLimit = creditLimit,
                    paymentTerms = terms,
                    notes = notes
                )
                showAddCustomerDialog = false
                customerToEdit = null
            }
        )
    }

    // Direct Customer Payment Settlement Dialog
    if (customerForDirectPayment != null) {
        CustomerDirectPaymentDialog(
            customer = customerForDirectPayment!!,
            currencyFormat = currencyFormat,
            onDismiss = { customerForDirectPayment = null },
            onConfirm = { amount, memo ->
                viewModel.recordCustomerDirectPayment(customerForDirectPayment!!.id, amount, memo)
                customerForDirectPayment = null
            }
        )
    }

    // Manual Journal Entry Dialog
    if (showManualJournalDialog) {
        ManualJournalEntryDialog(
            accounts = accounts,
            onDismiss = { showManualJournalDialog = false },
            onConfirm = { memo, ref, lines ->
                viewModel.postManualJournalEntry(
                    memo = memo,
                    sourceRef = ref,
                    lines = lines,
                    onSuccess = { showManualJournalDialog = false }
                )
            }
        )
    }

    // Create Invoice / PO Dialog with Client Autocomplete
    if (showCreateCommercialDialog) {
        CreateCommercialOrderDialog(
            inventoryItems = inventoryItems,
            customers = customers,
            initialCustomer = preselectedCustomerForInvoice,
            onDismiss = {
                showCreateCommercialDialog = false
                preselectedCustomerForInvoice = null
            },
            onConfirm = { orderNumber, type, partyName, notes, items, customerId ->
                viewModel.createCommercialOrder(
                    orderNumber = orderNumber,
                    type = type,
                    partyName = partyName,
                    notes = notes,
                    items = items,
                    customerId = customerId
                )
                showCreateCommercialDialog = false
                preselectedCustomerForInvoice = null
            }
        )
    }

    // Payment Settlement Dialog for Orders
    if (orderToRecordPayment != null) {
        RecordPaymentDialog(
            order = orderToRecordPayment!!,
            currencyFormat = currencyFormat,
            onDismiss = { orderToRecordPayment = null },
            onConfirm = { amount ->
                viewModel.recordCommercialPayment(orderToRecordPayment!!.id, amount)
                orderToRecordPayment = null
            }
        )
    }
}

@Composable
fun ClientsView(
    customers: List<Customer>,
    currencyFormat: NumberFormat,
    onAddClient: () -> Unit,
    onEditClient: (Customer) -> Unit,
    onDeleteClient: (Customer) -> Unit,
    onIssueInvoice: (Customer) -> Unit,
    onRecordPayment: (Customer) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterWithBalanceOnly by remember { mutableStateOf(false) }
    var selectedAccountType by remember { mutableStateOf<CustomerAccountType?>(null) }

    val filteredCustomers = customers.filter { cust ->
        val matchesSearch = cust.name.contains(searchQuery, ignoreCase = true) ||
                cust.code.contains(searchQuery, ignoreCase = true) ||
                cust.contactPerson.contains(searchQuery, ignoreCase = true) ||
                cust.email.contains(searchQuery, ignoreCase = true)
        val matchesBalance = !filterWithBalanceOnly || cust.currentBalance > 0.01
        val matchesType = selectedAccountType == null || cust.accountType == selectedAccountType
        matchesSearch && matchesBalance && matchesType
    }

    val totalReceivables = customers.sumOf { it.currentBalance }
    val totalCreditLimit = customers.sumOf { it.creditLimit }
    val availableCredit = (totalCreditLimit - totalReceivables).coerceAtLeast(0.0)

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("clients_list")
    ) {
        // Summary KPI Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Client Receivables",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currencyFormat.format(totalReceivables),
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (totalReceivables > 0) Color(0xFFEF4444) else EmeraldDark
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Active Clients",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${customers.size} Accounts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Credit Facility",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currencyFormat.format(totalCreditLimit),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldDark
                        )
                    }
                }
            }
        }

        // Search & Filter Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search clients by name, code, contact, email...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_customer_search")
                    )

                    FilterChip(
                        selected = filterWithBalanceOnly,
                        onClick = { filterWithBalanceOnly = !filterWithBalanceOnly },
                        label = { Text("Due Only", fontSize = 11.sp) },
                        leadingIcon = if (filterWithBalanceOnly) {
                            { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }

                // Account Type Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedAccountType == null,
                            onClick = { selectedAccountType = null },
                            label = { Text("All (${customers.size})", fontSize = 11.sp) }
                        )
                    }
                    items(CustomerAccountType.values()) { type ->
                        val count = customers.count { it.accountType == type }
                        val isSelected = selectedAccountType == type
                        val label = when (type) {
                            CustomerAccountType.WHOLESALE -> "Wholesale"
                            CustomerAccountType.RETAIL_BOUTIQUE -> "Boutique"
                            CustomerAccountType.CORPORATE -> "Corporate"
                            CustomerAccountType.EXPORT -> "Export"
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAccountType = if (isSelected) null else type },
                            label = { Text("$label ($count)", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Customer Cards
        if (filteredCustomers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No clients match your filter",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredCustomers, key = { it.id }) { customer ->
                CustomerCard(
                    customer = customer,
                    currencyFormat = currencyFormat,
                    onIssueInvoice = { onIssueInvoice(customer) },
                    onRecordPayment = { onRecordPayment(customer) },
                    onEdit = { onEditClient(customer) },
                    onDelete = { onDeleteClient(customer) }
                )
            }
        }
    }
}

@Composable
fun CustomerCard(
    customer: Customer,
    currencyFormat: NumberFormat,
    onIssueInvoice: () -> Unit,
    onRecordPayment: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val creditUtilization = if (customer.creditLimit > 0) {
        (customer.currentBalance / customer.creditLimit).toFloat().coerceIn(0f, 1f)
    } else 0f
    val isNearLimit = creditUtilization >= 0.8f
    val hasBalance = customer.currentBalance > 0.01

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_card_${customer.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Code, Name, Terms
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = customer.code,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val typeLabel = when (customer.accountType) {
                        CustomerAccountType.WHOLESALE -> "Wholesale"
                        CustomerAccountType.RETAIL_BOUTIQUE -> "Boutique"
                        CustomerAccountType.CORPORATE -> "Corporate"
                        CustomerAccountType.EXPORT -> "Export"
                    }
                    val typeColor = when (customer.accountType) {
                        CustomerAccountType.WHOLESALE -> Color(0xFF2563EB)
                        CustomerAccountType.RETAIL_BOUTIQUE -> Color(0xFF8B5CF6)
                        CustomerAccountType.CORPORATE -> Color(0xFF0D9488)
                        CustomerAccountType.EXPORT -> Color(0xFFD97706)
                    }
                    Surface(
                        color = typeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            color = typeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        color = EmeraldLight,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = customer.paymentTerms,
                            color = EmeraldDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Contact & Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (customer.contactPerson.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(customer.contactPerson, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (customer.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (customer.email.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(customer.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Credit & AR Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Outstanding Balance: ${currencyFormat.format(customer.currentBalance)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasBalance) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Credit Limit: ${currencyFormat.format(customer.creditLimit)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LinearProgressIndicator(
                    progress = { creditUtilization },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isNearLimit) AmberWarning else EmeraldDark,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onIssueInvoice,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("+ Invoice", fontSize = 11.sp)
                    }

                    if (hasBalance) {
                        OutlinedButton(
                            onClick = onRecordPayment,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Settle Payment", fontSize = 11.sp)
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Client", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Client", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditCustomerDialog(
    customer: Customer?,
    onDismiss: () -> Unit,
    onSave: (
        code: String,
        name: String,
        accountType: CustomerAccountType,
        contactPerson: String,
        email: String,
        phone: String,
        address: String,
        creditLimit: Double,
        paymentTerms: String,
        notes: String
    ) -> Unit
) {
    var code by remember { mutableStateOf(customer?.code ?: "CUST-${(100..999).random()}") }
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var accountType by remember { mutableStateOf(customer?.accountType ?: CustomerAccountType.WHOLESALE) }
    var contactPerson by remember { mutableStateOf(customer?.contactPerson ?: "") }
    var email by remember { mutableStateOf(customer?.email ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var creditLimitText by remember { mutableStateOf(customer?.creditLimit?.toString() ?: "25000") }
    var paymentTerms by remember { mutableStateOf(customer?.paymentTerms ?: "Net 30") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }

    val termsOptions = listOf("Net 15", "Net 30", "Net 60", "Due on Receipt", "50% Advance", "Letter of Credit")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (customer == null) "New Client / Customer Profile" else "Edit Client Profile") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Client Code") },
                            modifier = Modifier.weight(0.4f).testTag("input_customer_code")
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Company / Client Name") },
                            modifier = Modifier.weight(0.6f).testTag("input_customer_name")
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Account Category", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            CustomerAccountType.values().forEach { type ->
                                val isSelected = accountType == type
                                val label = when (type) {
                                    CustomerAccountType.WHOLESALE -> "Wholesale"
                                    CustomerAccountType.RETAIL_BOUTIQUE -> "Boutique"
                                    CustomerAccountType.CORPORATE -> "Corporate"
                                    CustomerAccountType.EXPORT -> "Export"
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { accountType = type },
                                    label = { Text(label, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = contactPerson,
                        onValueChange = { contactPerson = it },
                        label = { Text("Contact Person / Manager") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Delivery / Shipping Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = creditLimitText,
                            onValueChange = { creditLimitText = it },
                            label = { Text("Credit Facility ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("input_customer_credit")
                        )
                        OutlinedTextField(
                            value = paymentTerms,
                            onValueChange = { paymentTerms = it },
                            label = { Text("Payment Terms (e.g. Net 30)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(termsOptions) { term ->
                            FilterChip(
                                selected = paymentTerms == term,
                                onClick = { paymentTerms = term },
                                label = { Text(term, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Internal Account Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = creditLimitText.toDoubleOrNull() ?: 25000.0
                    onSave(code, name, accountType, contactPerson, email, phone, address, limit, paymentTerms, notes)
                },
                enabled = name.isNotBlank() && code.isNotBlank()
            ) {
                Text("Save Client")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CustomerDirectPaymentDialog(
    customer: Customer,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, memo: String) -> Unit
) {
    var amountText by remember { mutableStateOf(customer.currentBalance.toString()) }
    var memoText by remember { mutableStateOf("Client remittance on account") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment from ${customer.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Outstanding AR Balance: ${currencyFormat.format(customer.currentBalance)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Remittance Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("input_client_payment_amount")
                )

                OutlinedTextField(
                    value = memoText,
                    onValueChange = { memoText = it },
                    label = { Text("Payment Reference / Check # / Wire") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) onConfirm(amount, memoText)
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Confirm Remittance")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ChartOfAccountsView(
    accounts: List<Account>,
    currencyFormat: NumberFormat
) {
    val grouped = accounts.groupBy { it.type }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().testTag("coa_list")
    ) {
        AccountType.values().forEach { type ->
            val list = grouped[type] ?: emptyList()
            if (list.isNotEmpty()) {
                item {
                    Text(
                        text = "${type.name} ACCOUNTS",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(list, key = { it.accountCode }) { acc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = acc.accountCode,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = acc.accountName,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Normal: ${acc.normalBalance.name} • ${acc.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Text(
                                text = currencyFormat.format(acc.currentBalance),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (acc.type == AccountType.REVENUE || acc.type == AccountType.ASSET) EmeraldDark else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeneralLedgerView(
    entries: List<JournalEntry>,
    currencyFormat: NumberFormat
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    if (entries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No journal entries recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize().testTag("journal_entries_list")
        ) {
            items(entries, key = { it.id }) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = entry.entryNumber,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (entry.sourceReference.isNotBlank()) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Ref: ${entry.sourceReference}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = dateFormat.format(Date(entry.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = entry.memo,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Balanced Total: ${currencyFormat.format(entry.totalAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommercialOrdersView(
    orders: List<CommercialOrder>,
    currencyFormat: NumberFormat,
    onRecordPayment: (CommercialOrder) -> Unit
) {
    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No commercial invoices or purchase orders found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize().testTag("commercial_orders_list")
        ) {
            items(orders, key = { it.id }) { order ->
                val isInvoice = order.type == CommercialType.COMMERCIAL_INVOICE
                val balanceDue = order.totalAmount - order.paidAmount
                val isPaid = balanceDue <= 0.01

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (isInvoice) EmeraldLight else Color(0xFFDBEAFE),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isInvoice) "CLIENT INVOICE" else "PURCHASE ORDER",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isInvoice) EmeraldDark else Color(0xFF1D4ED8),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = order.orderNumber,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                color = if (isPaid) EmeraldLight else AmberLight,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isPaid) "PAID" else "BALANCE DUE",
                                    color = if (isPaid) EmeraldDark else AmberDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = order.partyName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (order.notes.isNotBlank()) {
                            Text(
                                text = order.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Amount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currencyFormat.format(order.totalAmount),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Paid",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currencyFormat.format(order.paidAmount),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = EmeraldDark
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Balance Due",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currencyFormat.format(balanceDue),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (balanceDue > 0) Color(0xFFEF4444) else EmeraldDark
                                )
                            }
                        }

                        if (!isPaid) {
                            Spacer(Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(
                                    onClick = { onRecordPayment(order) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Record Payment", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordPaymentDialog(
    order: CommercialOrder,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit
) {
    val remainingBalance = order.totalAmount - order.paidAmount
    var paymentAmountText by remember { mutableStateOf(remainingBalance.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment for ${order.orderNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Party: ${order.partyName}",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Remaining Balance: ${currencyFormat.format(remainingBalance)}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = paymentAmountText,
                    onValueChange = { paymentAmountText = it },
                    label = { Text("Payment Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("input_payment_amount")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = paymentAmountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) onConfirm(amount)
                },
                enabled = (paymentAmountText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCommercialOrderDialog(
    inventoryItems: List<InventoryItem>,
    customers: List<Customer> = emptyList(),
    initialCustomer: Customer? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        orderNumber: String,
        type: CommercialType,
        partyName: String,
        notes: String,
        items: List<CommercialOrderItem>,
        customerId: Long
    ) -> Unit
) {
    var type by remember { mutableStateOf(if (initialCustomer != null) CommercialType.COMMERCIAL_INVOICE else CommercialType.COMMERCIAL_INVOICE) }
    var orderNumber by remember {
        mutableStateOf(
            if (type == CommercialType.COMMERCIAL_INVOICE) "INV-${(1000..9999).random()}" else "PO-${(1000..9999).random()}"
        )
    }
    var selectedCustomer by remember { mutableStateOf(initialCustomer) }
    var partyName by remember { mutableStateOf(initialCustomer?.name ?: "") }
    var notes by remember { mutableStateOf(if (initialCustomer != null) "Payment terms: ${initialCustomer.paymentTerms}" else "") }

    var customerDropdownExpanded by remember { mutableStateOf(false) }

    // Item selection
    val candidateItems = if (type == CommercialType.COMMERCIAL_INVOICE) {
        inventoryItems.filter { it.category == ItemCategory.FINISHED_GOOD }
    } else {
        inventoryItems.filter { it.category != ItemCategory.FINISHED_GOOD }
    }
    var selectedItem by remember { mutableStateOf(candidateItems.firstOrNull()) }
    var quantityText by remember { mutableStateOf("50") }
    var unitPriceText by remember { mutableStateOf(selectedItem?.sellingPrice?.toString() ?: "35.0") }
    var itemDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == CommercialType.COMMERCIAL_INVOICE) "New Client Commercial Invoice" else "New Supplier Purchase Order") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                type = CommercialType.COMMERCIAL_INVOICE
                                orderNumber = "INV-${(1000..9999).random()}"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == CommercialType.COMMERCIAL_INVOICE) EmeraldDark else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (type == CommercialType.COMMERCIAL_INVOICE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sales Invoice")
                        }

                        Button(
                            onClick = {
                                type = CommercialType.PURCHASE_ORDER
                                orderNumber = "PO-${(1000..9999).random()}"
                                selectedCustomer = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == CommercialType.PURCHASE_ORDER) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (type == CommercialType.PURCHASE_ORDER) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Purchase Order")
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = orderNumber,
                        onValueChange = { orderNumber = it },
                        label = { Text("Order / Invoice #") },
                        modifier = Modifier.fillMaxWidth().testTag("input_order_number")
                    )
                }

                // If Sales Invoice, allow picking from Customer Directory!
                if (type == CommercialType.COMMERCIAL_INVOICE && customers.isNotEmpty()) {
                    item {
                        ExposedDropdownMenuBox(
                            expanded = customerDropdownExpanded,
                            onExpandedChange = { customerDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCustomer?.name ?: if (partyName.isNotBlank()) partyName else "Select Registered Client",
                                onValueChange = { partyName = it },
                                label = { Text("Select Registered Client / Buyer") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = customerDropdownExpanded,
                                onDismissRequest = { customerDropdownExpanded = false }
                            ) {
                                customers.forEach { cust ->
                                    DropdownMenuItem(
                                        text = { Text("${cust.code} - ${cust.name} (${cust.paymentTerms})") },
                                        onClick = {
                                            selectedCustomer = cust
                                            partyName = cust.name
                                            notes = "Payment terms: ${cust.paymentTerms}"
                                            customerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = partyName,
                        onValueChange = { partyName = it },
                        label = { Text(if (type == CommercialType.COMMERCIAL_INVOICE) "Buyer / Client Company Name" else "Supplier / Mill Name") },
                        modifier = Modifier.fillMaxWidth().testTag("input_party_name")
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = itemDropdownExpanded,
                        onExpandedChange = { itemDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedItem?.name ?: "Select Item",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Product / Material") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = itemDropdownExpanded,
                            onDismissRequest = { itemDropdownExpanded = false }
                        ) {
                            candidateItems.forEach { itm ->
                                DropdownMenuItem(
                                    text = { Text("${itm.sku} - ${itm.name}") },
                                    onClick = {
                                        selectedItem = itm
                                        unitPriceText = if (type == CommercialType.COMMERCIAL_INVOICE) itm.sellingPrice.toString() else itm.averageCost.toString()
                                        itemDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            label = { Text("Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unitPriceText,
                            onValueChange = { unitPriceText = it },
                            label = { Text("Unit Price ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Payment Terms / Delivery Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val itm = selectedItem ?: return@Button
                    val qty = quantityText.toDoubleOrNull() ?: 0.0
                    val price = unitPriceText.toDoubleOrNull() ?: 0.0
                    val items = listOf(
                        CommercialOrderItem(
                            orderId = 0L,
                            itemId = itm.id,
                            itemName = itm.name,
                            unit = itm.unit,
                            quantity = qty,
                            unitPrice = price,
                            lineTotal = qty * price
                        )
                    )
                    onConfirm(orderNumber, type, partyName, notes, items, selectedCustomer?.id ?: 0L)
                },
                enabled = partyName.isNotBlank() && selectedItem != null && (quantityText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualJournalEntryDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (memo: String, ref: String, lines: List<JournalLine>) -> Unit
) {
    var memo by remember { mutableStateOf("Manual journal entry") }
    var refNumber by remember { mutableStateOf("JE-${System.currentTimeMillis() % 10000}") }

    var debitAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var creditAccount by remember { mutableStateOf(accounts.getOrNull(1)) }
    var amountText by remember { mutableStateOf("500") }

    var debitDropdownExpanded by remember { mutableStateOf(false) }
    var creditDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post Balanced Journal Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("Transaction Memo / Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Balanced Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("input_journal_amount")
                )

                // Debit Account
                ExposedDropdownMenuBox(
                    expanded = debitDropdownExpanded,
                    onExpandedChange = { debitDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${debitAccount?.accountCode ?: ""} - ${debitAccount?.accountName ?: "Select Debit Account"}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("DEBIT Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = debitDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = debitDropdownExpanded,
                        onDismissRequest = { debitDropdownExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.accountCode} - ${acc.accountName} (${acc.type.name})") },
                                onClick = {
                                    debitAccount = acc
                                    debitDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Credit Account
                ExposedDropdownMenuBox(
                    expanded = creditDropdownExpanded,
                    onExpandedChange = { creditDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${creditAccount?.accountCode ?: ""} - ${creditAccount?.accountName ?: "Select Credit Account"}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("CREDIT Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = creditDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = creditDropdownExpanded,
                        onDismissRequest = { creditDropdownExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.accountCode} - ${acc.accountName} (${acc.type.name})") },
                                onClick = {
                                    creditAccount = acc
                                    creditDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val debitAcc = debitAccount ?: return@Button
                    val creditAcc = creditAccount ?: return@Button
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val lines = listOf(
                        JournalLine(0L, 0L, debitAcc.accountCode, debitAcc.accountName, debit = amount, credit = 0.0),
                        JournalLine(0L, 0L, creditAcc.accountCode, creditAcc.accountName, debit = 0.0, credit = amount)
                    )
                    onConfirm(memo, refNumber, lines)
                },
                enabled = debitAccount != null && creditAccount != null && (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Post Balanced Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
