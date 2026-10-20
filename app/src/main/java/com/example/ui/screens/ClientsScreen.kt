package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Business
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
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CommercialOrder
import com.example.data.entity.CommercialOrderItem
import com.example.data.entity.CommercialStatus
import com.example.data.entity.CommercialType
import com.example.data.entity.Customer
import com.example.data.entity.CustomerAccountType
import com.example.data.entity.InventoryItem
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

enum class ClientSortOption(val label: String) {
    NAME_ASC("Name (A-Z)"),
    BALANCE_DESC("Highest Due AR"),
    CODE_ASC("Client Code")
}

@Composable
fun ClientsScreen(
    viewModel: ErpViewModel,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedAccountTypeFilter by remember { mutableStateOf<CustomerAccountType?>(null) }
    var filterWithBalanceOnly by remember { mutableStateOf(false) }
    var selectedSortOption by remember { mutableStateOf(ClientSortOption.NAME_ASC) }

    // Dialog & Inspection states
    var showAddClientDialog by remember { mutableStateOf(false) }
    var clientToEdit by remember { mutableStateOf<Customer?>(null) }
    var clientToDelete by remember { mutableStateOf<Customer?>(null) }
    var clientToSettlePayment by remember { mutableStateOf<Customer?>(null) }
    var clientToCreateInvoice by remember { mutableStateOf<Customer?>(null) }
    var inspectedClient by remember { mutableStateOf<Customer?>(null) }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    // Filter and Sort Clients
    val filteredCustomers = remember(customers, searchQuery, selectedAccountTypeFilter, filterWithBalanceOnly, selectedSortOption) {
        customers
            .filter { client ->
                val matchesSearch = searchQuery.isBlank() ||
                        client.name.contains(searchQuery, ignoreCase = true) ||
                        client.code.contains(searchQuery, ignoreCase = true) ||
                        client.contactPerson.contains(searchQuery, ignoreCase = true) ||
                        client.email.contains(searchQuery, ignoreCase = true) ||
                        client.phone.contains(searchQuery, ignoreCase = true)

                val matchesAccountType = selectedAccountTypeFilter == null || client.accountType == selectedAccountTypeFilter
                val matchesBalance = !filterWithBalanceOnly || client.currentBalance > 0.01

                matchesSearch && matchesAccountType && matchesBalance
            }
            .sortedWith { a, b ->
                when (selectedSortOption) {
                    ClientSortOption.NAME_ASC -> a.name.compareTo(b.name, ignoreCase = true)
                    ClientSortOption.BALANCE_DESC -> b.currentBalance.compareTo(a.currentBalance)
                    ClientSortOption.CODE_ASC -> a.code.compareTo(b.code, ignoreCase = true)
                }
            }
    }

    // Keep inspectedClient in sync if list updates
    val activeInspectedClient = inspectedClient?.let { current ->
        customers.find { it.id == current.id } ?: current
    }

    val totalReceivables = customers.sumOf { it.currentBalance }
    val totalCreditLimit = customers.sumOf { it.creditLimit }
    val accountsWithBalanceCount = customers.count { it.currentBalance > 0.01 }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("clients_screen")
    ) {
        val isWideScreen = maxWidth >= 760.dp

        if (isWideScreen) {
            // Adaptive Tablet / Two-Column Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Search, Filter Chips, KPI, and Client List
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ClientsHeader(
                        totalCount = customers.size,
                        onAddClient = {
                            clientToEdit = null
                            showAddClientDialog = true
                        }
                    )

                    ClientsSummaryCard(
                        totalReceivables = totalReceivables,
                        totalCreditLimit = totalCreditLimit,
                        totalAccounts = customers.size,
                        dueAccounts = accountsWithBalanceCount,
                        currencyFormat = currencyFormat
                    )

                    ClientsFilterSection(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        allCustomers = customers,
                        selectedAccountType = selectedAccountTypeFilter,
                        onSelectAccountType = { selectedAccountTypeFilter = it },
                        filterWithBalanceOnly = filterWithBalanceOnly,
                        onToggleBalanceFilter = { filterWithBalanceOnly = !filterWithBalanceOnly },
                        selectedSort = selectedSortOption,
                        onSelectSort = { selectedSortOption = it }
                    )

                    if (filteredCustomers.isEmpty()) {
                        EmptyClientsView(
                            searchQuery = searchQuery,
                            onClearFilter = {
                                searchQuery = ""
                                selectedAccountTypeFilter = null
                                filterWithBalanceOnly = false
                            }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("tablet_clients_list")
                        ) {
                            items(filteredCustomers, key = { it.id }) { client ->
                                val isSelected = activeInspectedClient?.id == client.id
                                ClientListItemCard(
                                    customer = client,
                                    isSelected = isSelected,
                                    currencyFormat = currencyFormat,
                                    onClick = { inspectedClient = client },
                                    onIssueInvoice = { clientToCreateInvoice = client },
                                    onRecordPayment = { clientToSettlePayment = client },
                                    onEdit = {
                                        clientToEdit = client
                                        showAddClientDialog = true
                                    },
                                    onDelete = { clientToDelete = client }
                                )
                            }
                        }
                    }
                }

                // Right Column: Client Detail Dossier & Invoices History
                Box(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                ) {
                    if (activeInspectedClient != null) {
                        ClientDossierPanel(
                            customer = activeInspectedClient,
                            viewModel = viewModel,
                            currencyFormat = currencyFormat,
                            onIssueInvoice = { clientToCreateInvoice = activeInspectedClient },
                            onRecordPayment = { clientToSettlePayment = activeInspectedClient },
                            onEdit = {
                                clientToEdit = activeInspectedClient
                                showAddClientDialog = true
                            },
                            onClose = { inspectedClient = null }
                        )
                    } else {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Business,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Select a Client to View Dossier",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Audit credit limits, addresses, contact details, and customer order history",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Handheld / Compact Phone Layout
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ClientsHeader(
                            totalCount = customers.size,
                            onAddClient = {
                                clientToEdit = null
                                showAddClientDialog = true
                            }
                        )
                    }

                    item {
                        ClientsSummaryCard(
                            totalReceivables = totalReceivables,
                            totalCreditLimit = totalCreditLimit,
                            totalAccounts = customers.size,
                            dueAccounts = accountsWithBalanceCount,
                            currencyFormat = currencyFormat
                        )
                    }

                    item {
                        ClientsFilterSection(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            allCustomers = customers,
                            selectedAccountType = selectedAccountTypeFilter,
                            onSelectAccountType = { selectedAccountTypeFilter = it },
                            filterWithBalanceOnly = filterWithBalanceOnly,
                            onToggleBalanceFilter = { filterWithBalanceOnly = !filterWithBalanceOnly },
                            selectedSort = selectedSortOption,
                            onSelectSort = { selectedSortOption = it }
                        )
                    }

                    if (filteredCustomers.isEmpty()) {
                        item {
                            EmptyClientsView(
                                searchQuery = searchQuery,
                                onClearFilter = {
                                    searchQuery = ""
                                    selectedAccountTypeFilter = null
                                    filterWithBalanceOnly = false
                                }
                            )
                        }
                    } else {
                        items(filteredCustomers, key = { it.id }) { client ->
                            ClientListItemCard(
                                customer = client,
                                isSelected = false,
                                currencyFormat = currencyFormat,
                                onClick = { inspectedClient = client },
                                onIssueInvoice = { clientToCreateInvoice = client },
                                onRecordPayment = { clientToSettlePayment = client },
                                onEdit = {
                                    clientToEdit = client
                                    showAddClientDialog = true
                                },
                                onDelete = { clientToDelete = client }
                            )
                        }
                    }
                }

                // Phone Floating Action Button
                FloatingActionButton(
                    onClick = {
                        clientToEdit = null
                        showAddClientDialog = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .testTag("fab_add_client"),
                    containerColor = EmeraldDark,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Client")
                }
            }
        }

    // Modal Dialog: Add / Edit Client
    if (showAddClientDialog) {
        AddEditCustomerModal(
            customer = clientToEdit,
            onDismiss = { showAddClientDialog = false },
            onSave = { code, name, accountType, contact, email, phone, address, creditLimit, terms, notes ->
                viewModel.saveCustomer(
                    id = clientToEdit?.id ?: 0L,
                    code = code,
                    name = name,
                    accountType = accountType,
                    contactPerson = contact,
                    email = email,
                    phone = phone,
                    address = address,
                    creditLimit = creditLimit,
                    paymentTerms = terms,
                    notes = notes
                )
                showAddClientDialog = false
            }
        )
    }

    // Modal Dialog: Delete Confirmation
    if (clientToDelete != null) {
        val client = clientToDelete!!
        AlertDialog(
            onDismissRequest = { clientToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444)) },
            title = { Text("Delete Client Profile?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to remove ${client.name} (${client.code})?")
                    if (client.currentBalance > 0.01) {
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Notice: This client has an outstanding balance of ${currencyFormat.format(client.currentBalance)}. Deleting will remove customer ledger records.",
                                color = Color(0xFF991B1B),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(client.id)
                        if (inspectedClient?.id == client.id) inspectedClient = null
                        clientToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("btn_confirm_delete_client")
                ) {
                    Text("Delete Client")
                }
            },
            dismissButton = {
                TextButton(onClick = { clientToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Dialog: Direct AR Payment Settlement
    if (clientToSettlePayment != null) {
        ClientDirectPaymentModal(
            customer = clientToSettlePayment!!,
            currencyFormat = currencyFormat,
            onDismiss = { clientToSettlePayment = null },
            onConfirmPayment = { amount, memo ->
                viewModel.recordCustomerDirectPayment(clientToSettlePayment!!.id, amount, memo)
                clientToSettlePayment = null
            }
        )
    }

    // Modal Dialog: Create Commercial Invoice linked to Client
    if (clientToCreateInvoice != null) {
        CreateInvoiceForCustomerModal(
            customer = clientToCreateInvoice!!,
            inventoryItems = inventoryItems,
            onDismiss = { clientToCreateInvoice = null },
            onCreate = { orderNumber, partyName, notes, items ->
                viewModel.createCommercialOrder(
                    orderNumber = orderNumber,
                    type = CommercialType.COMMERCIAL_INVOICE,
                    partyName = partyName,
                    notes = notes,
                    items = items,
                    customerId = clientToCreateInvoice!!.id
                )
                clientToCreateInvoice = null
            }
        )
    }

    // Mobile Modal: Inspect Client Dossier
    if (!isWideScreen && activeInspectedClient != null) {
        AlertDialog(
            onDismissRequest = { inspectedClient = null },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(520.dp)) {
                    ClientDossierPanel(
                        customer = activeInspectedClient,
                        viewModel = viewModel,
                        currencyFormat = currencyFormat,
                        onIssueInvoice = {
                            val c = activeInspectedClient
                            inspectedClient = null
                            clientToCreateInvoice = c
                        },
                        onRecordPayment = {
                            val c = activeInspectedClient
                            inspectedClient = null
                            clientToSettlePayment = c
                        },
                        onEdit = {
                            val c = activeInspectedClient
                            inspectedClient = null
                            clientToEdit = c
                            showAddClientDialog = true
                        },
                        onClose = { inspectedClient = null }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectedClient = null }) {
                    Text("Done")
                }
            }
        )
    }
    }
}

@Composable
fun ClientsHeader(
    totalCount: Int,
    onAddClient: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Clients & Customers",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Wholesale distributors, retail boutiques, and corporate accounts ($totalCount total)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onAddClient,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("btn_add_client_top")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add Client", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ClientsSummaryCard(
    totalReceivables: Double,
    totalCreditLimit: Double,
    totalAccounts: Int,
    dueAccounts: Int,
    currencyFormat: NumberFormat
) {
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
                    text = "Total Accounts Receivable (AR)",
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
                Text(
                    text = "$dueAccounts accounts currently with outstanding balance",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                val utilizationPercent = if (totalCreditLimit > 0) {
                    ((totalReceivables / totalCreditLimit) * 100).toInt()
                } else 0
                Text(
                    text = "$utilizationPercent% credit limit utilized",
                    fontSize = 11.sp,
                    color = if (utilizationPercent > 75) AmberDark else EmeraldDark
                )
            }
        }
    }
}

@Composable
fun ClientsFilterSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    allCustomers: List<Customer>,
    selectedAccountType: CustomerAccountType?,
    onSelectAccountType: (CustomerAccountType?) -> Unit,
    filterWithBalanceOnly: Boolean,
    onToggleBalanceFilter: () -> Unit,
    selectedSort: ClientSortOption,
    onSelectSort: (ClientSortOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Search Input and Sort
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search by name, code, contact, email...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_clients_search")
            )

            // Balance Filter Toggle
            FilterChip(
                selected = filterWithBalanceOnly,
                onClick = onToggleBalanceFilter,
                label = { Text("Due Only", fontSize = 12.sp) },
                leadingIcon = if (filterWithBalanceOnly) {
                    { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFEE2E2),
                    selectedLabelColor = Color(0xFF991B1B)
                ),
                modifier = Modifier.testTag("chip_filter_due_balance")
            )
        }

        // Account Type Filters (Horizontal Scrolling Chips)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedAccountType == null,
                    onClick = { onSelectAccountType(null) },
                    label = { Text("All Types (${allCustomers.size})", fontSize = 11.sp) },
                    modifier = Modifier.testTag("chip_type_all")
                )
            }

            CustomerAccountType.values().forEach { type ->
                val count = allCustomers.count { it.accountType == type }
                val isSelected = selectedAccountType == type
                val label = when (type) {
                    CustomerAccountType.WHOLESALE -> "Wholesale"
                    CustomerAccountType.RETAIL_BOUTIQUE -> "Boutique"
                    CustomerAccountType.CORPORATE -> "Corporate"
                    CustomerAccountType.EXPORT -> "Export"
                }

                item {
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectAccountType(if (isSelected) null else type) },
                        label = { Text("$label ($count)", fontSize = 11.sp) },
                        modifier = Modifier.testTag("chip_type_${type.name.lowercase()}")
                    )
                }
            }
        }
    }
}

@Composable
fun ClientListItemCard(
    customer: Customer,
    isSelected: Boolean,
    currencyFormat: NumberFormat,
    onClick: () -> Unit,
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

    val accountTypeBadgeColor = when (customer.accountType) {
        CustomerAccountType.WHOLESALE -> Color(0xFF2563EB)
        CustomerAccountType.RETAIL_BOUTIQUE -> Color(0xFF8B5CF6)
        CustomerAccountType.CORPORATE -> Color(0xFF0D9488)
        CustomerAccountType.EXPORT -> Color(0xFFD97706)
    }

    val accountTypeLabel = when (customer.accountType) {
        CustomerAccountType.WHOLESALE -> "Wholesale"
        CustomerAccountType.RETAIL_BOUTIQUE -> "Boutique"
        CustomerAccountType.CORPORATE -> "Corporate"
        CustomerAccountType.EXPORT -> "Export"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("client_item_${customer.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Code, Name, Account Type Badge, Terms
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
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
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = accountTypeBadgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = accountTypeLabel,
                            color = accountTypeBadgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = EmeraldLight,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = customer.paymentTerms,
                            color = EmeraldDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Contact Information Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (customer.contactPerson.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = customer.contactPerson,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (customer.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = customer.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (customer.email.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = customer.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            // Financial Gauge: Due AR vs Credit Limit
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Due AR: ${currencyFormat.format(customer.currentBalance)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasBalance) Color(0xFFEF4444) else EmeraldDark
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
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("+ Invoice", fontSize = 11.sp)
                    }

                    if (hasBalance) {
                        OutlinedButton(
                            onClick = onRecordPayment,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Settle Pay", fontSize = 11.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onClick,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Dossier", fontSize = 11.sp)
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
fun ClientDossierPanel(
    customer: Customer,
    viewModel: ErpViewModel,
    currencyFormat: NumberFormat,
    onIssueInvoice: () -> Unit,
    onRecordPayment: () -> Unit,
    onEdit: () -> Unit,
    onClose: () -> Unit
) {
    val ordersFlow = remember(customer.id) { viewModel.getCustomerOrders(customer.id) }
    val orders by ordersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val accountTypeLabel = when (customer.accountType) {
        CustomerAccountType.WHOLESALE -> "Wholesale Distributor"
        CustomerAccountType.RETAIL_BOUTIQUE -> "Retail Boutique"
        CustomerAccountType.CORPORATE -> "Corporate Client"
        CustomerAccountType.EXPORT -> "Export Wholesaler"
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .testTag("client_dossier_panel"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = customer.code,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = accountTypeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close Dossier")
                    }
                }
            }

            Divider()

            // Financial Balance & Terms
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Outstanding Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = currencyFormat.format(customer.currentBalance),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (customer.currentBalance > 0) Color(0xFFEF4444) else EmeraldDark
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Payment Terms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = customer.paymentTerms,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Credit Limit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = currencyFormat.format(customer.creditLimit),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onIssueInvoice,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Create Invoice")
                }

                if (customer.currentBalance > 0.01) {
                    OutlinedButton(
                        onClick = onRecordPayment,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Record Payment")
                    }
                }
            }

            // Contact & Logistics Details Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (customer.contactPerson.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text("Contact: ${customer.contactPerson}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (customer.phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text("Phone: ${customer.phone}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (customer.email.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text("Email: ${customer.email}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (customer.address.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text("Shipping / Billing: ${customer.address}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (customer.notes.isNotBlank()) {
                        Text(
                            text = "Notes: ${customer.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Invoices and Orders History
            Text(
                text = "Invoices & Commercial Orders (${orders.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No invoices issued for this client yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(orders, key = { it.id }) { order ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = order.orderNumber,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = dateFormat.format(Date(order.timestamp)),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = currencyFormat.format(order.totalAmount),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    )
                                    val statusColor = when (order.status) {
                                        CommercialStatus.PAID -> EmeraldDark
                                        CommercialStatus.CONFIRMED -> Color(0xFF2563EB)
                                        CommercialStatus.FULFILLED -> EmeraldAccent
                                        CommercialStatus.DRAFT -> Color(0xFF6B7280)
                                        CommercialStatus.CANCELLED -> Color(0xFFEF4444)
                                    }
                                    Text(
                                        text = order.status.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
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
fun EmptyClientsView(
    searchQuery: String,
    onClearFilter: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.People,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "No clients matching \"$searchQuery\"" else "No clients found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Try adjusting your search query or account type filters",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onClearFilter) {
                Text("Reset Filters")
            }
        }
    }
}

@Composable
fun AddEditCustomerModal(
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
    var code by remember { mutableStateOf(customer?.code ?: "CUST-${(105..999).random()}") }
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var accountType by remember { mutableStateOf(customer?.accountType ?: CustomerAccountType.WHOLESALE) }
    var contactPerson by remember { mutableStateOf(customer?.contactPerson ?: "") }
    var email by remember { mutableStateOf(customer?.email ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var creditLimitText by remember { mutableStateOf(customer?.creditLimit?.toString() ?: "30000") }
    var paymentTerms by remember { mutableStateOf(customer?.paymentTerms ?: "Net 30") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }

    var isNameError by remember { mutableStateOf(false) }

    val termsOptions = listOf("Net 15", "Net 30", "Net 60", "Due on Receipt", "50% Advance", "Letter of Credit")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (customer == null) "New Client / Customer Profile" else "Edit Client Profile") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Client Code") },
                            modifier = Modifier.weight(0.4f).testTag("modal_customer_code")
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                isNameError = it.isBlank()
                            },
                            isError = isNameError,
                            label = { Text("Client / Company Name *") },
                            modifier = Modifier.weight(0.6f).testTag("modal_customer_name")
                        )
                    }
                }

                // Account Type Selector Chips
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Account Category", style = MaterialTheme.typography.labelSmall)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
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
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(0.5f)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.weight(0.5f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Shipping / Billing Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = creditLimitText,
                            onValueChange = { creditLimitText = it },
                            label = { Text("Credit Limit ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.5f)
                        )
                        OutlinedTextField(
                            value = paymentTerms,
                            onValueChange = { paymentTerms = it },
                            label = { Text("Payment Terms") },
                            modifier = Modifier.weight(0.5f)
                        )
                    }
                }

                // Preset Terms Chips
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
                        label = { Text("Notes / Specifications") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isNameError = true
                        return@Button
                    }
                    val limit = creditLimitText.toDoubleOrNull() ?: 25000.0
                    onSave(
                        code,
                        name,
                        accountType,
                        contactPerson,
                        email,
                        phone,
                        address,
                        limit,
                        paymentTerms,
                        notes
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                modifier = Modifier.testTag("btn_save_customer_modal")
            ) {
                Text(if (customer == null) "Create Client" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ClientDirectPaymentModal(
    customer: Customer,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit,
    onConfirmPayment: (amount: Double, memo: String) -> Unit
) {
    var amountText by remember { mutableStateOf(customer.currentBalance.toString()) }
    var memo by remember { mutableStateOf("Settlement of Accounts Receivable via Bank Wire") }
    var selectedAccount by remember { mutableStateOf("1020 Operating Bank Account") }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val isValid = amount > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Customer Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = EmeraldLight,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = customer.name,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
                        Text(
                            text = "Current Due Balance: ${currencyFormat.format(customer.currentBalance)}",
                            fontSize = 12.sp,
                            color = EmeraldDark
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount Received ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("input_direct_payment_amount")
                )

                // Quick Full Payment Button
                if (customer.currentBalance > 0.01) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { amountText = customer.currentBalance.toString() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Full Balance (${currencyFormat.format(customer.currentBalance)})", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { amountText = (customer.currentBalance / 2.0).toString() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("50% Deposit", fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("Payment Memo / Wire Reference") },
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Double-Entry: Debit Operating Bank Account (1020) and Credit Accounts Receivable (1100).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (isValid) onConfirmPayment(amount, memo) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                modifier = Modifier.testTag("btn_confirm_direct_payment")
            ) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateInvoiceForCustomerModal(
    customer: Customer,
    inventoryItems: List<InventoryItem>,
    onDismiss: () -> Unit,
    onCreate: (orderNumber: String, partyName: String, notes: String, items: List<CommercialOrderItem>) -> Unit
) {
    var orderNumber by remember { mutableStateOf("INV-2026-${(100..999).random()}") }
    var notes by remember { mutableStateOf("Direct Commercial Invoice for ${customer.name} (Terms: ${customer.paymentTerms})") }

    val lineItems = remember { mutableStateListOf<CommercialOrderItem>() }

    // Finished Goods available for sale
    val saleableItems = remember(inventoryItems) {
        inventoryItems.filter { it.category == com.example.data.entity.ItemCategory.FINISHED_GOOD || it.category == com.example.data.entity.ItemCategory.RAW_MATERIAL }
    }

    var selectedItemForAdd by remember { mutableStateOf<InventoryItem?>(saleableItems.firstOrNull()) }
    var quantityText by remember { mutableStateOf("50") }
    var unitPriceText by remember { mutableStateOf(selectedItemForAdd?.sellingPrice?.toString() ?: "38.0") }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val totalInvoice = lineItems.sumOf { it.lineTotal }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Issue Invoice to ${customer.name}") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = orderNumber,
                            onValueChange = { orderNumber = it },
                            label = { Text("Invoice #") },
                            modifier = Modifier.weight(0.45f)
                        )
                        OutlinedTextField(
                            value = customer.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Client") },
                            modifier = Modifier.weight(0.55f)
                        )
                    }
                }

                // Add Item Section
                item {
                    Text("Add Garments / Fabric Items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }

                if (saleableItems.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(saleableItems) { item ->
                                        FilterChip(
                                            selected = selectedItemForAdd?.id == item.id,
                                            onClick = {
                                                selectedItemForAdd = item
                                                unitPriceText = if (item.sellingPrice > 0) item.sellingPrice.toString() else (item.averageCost * 1.5).toString()
                                            },
                                            label = { Text(item.name, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = quantityText,
                                        onValueChange = { quantityText = it },
                                        label = { Text("Qty (${selectedItemForAdd?.unit?.name ?: "PCS"})") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(0.4f)
                                    )
                                    OutlinedTextField(
                                        value = unitPriceText,
                                        onValueChange = { unitPriceText = it },
                                        label = { Text("Price/Unit ($)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(0.4f)
                                    )
                                    Button(
                                        onClick = {
                                            val qty = quantityText.toDoubleOrNull() ?: 0.0
                                            val price = unitPriceText.toDoubleOrNull() ?: 0.0
                                            val item = selectedItemForAdd
                                            if (item != null && qty > 0 && price > 0) {
                                                lineItems.add(
                                                    CommercialOrderItem(
                                                        orderId = 0L,
                                                        itemId = item.id,
                                                        itemName = item.name,
                                                        unit = item.unit,
                                                        quantity = qty,
                                                        unitPrice = price,
                                                        lineTotal = qty * price
                                                    )
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                        modifier = Modifier.weight(0.2f).height(56.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add")
                                    }
                                }
                            }
                        }
                    }
                }

                // Current Line Items
                if (lineItems.isNotEmpty()) {
                    items(lineItems.size) { idx ->
                        val item = lineItems[idx]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.itemName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${item.quantity} ${item.unit.name.lowercase()} @ ${currencyFormat.format(item.unitPrice)}", fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currencyFormat.format(item.lineTotal), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                IconButton(onClick = { lineItems.removeAt(idx) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Invoice Amount:", fontWeight = FontWeight.Bold)
                            Text(currencyFormat.format(totalInvoice), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = EmeraldDark)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Order Notes & Terms") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (lineItems.isNotEmpty()) {
                        onCreate(orderNumber, customer.name, notes, lineItems)
                    }
                },
                enabled = lineItems.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                modifier = Modifier.testTag("btn_confirm_create_client_invoice")
            ) {
                Text("Issue Invoice")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
