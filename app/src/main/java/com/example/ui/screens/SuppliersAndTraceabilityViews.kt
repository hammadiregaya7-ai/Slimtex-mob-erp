package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ErpCurrency
import com.example.data.entity.FabricRollLot
import com.example.data.entity.InventoryItem
import com.example.data.entity.RollQualityGrade
import com.example.data.entity.Supplier
import com.example.data.entity.SupplierSpecialty
import com.example.ui.theme.AmberDark
import com.example.ui.theme.AmberLight
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import java.text.NumberFormat

@Composable
fun FabricRollTraceabilitySubView(
    rollLots: List<FabricRollLot>,
    inventoryItems: List<InventoryItem>,
    onAddRollLot: (
        rollNumber: String,
        dyeLotCode: String,
        itemId: Long,
        itemSku: String,
        itemName: String,
        supplierName: String,
        shadeBand: String,
        lengthMeters: Double,
        gsm: Int,
        widthCm: Int,
        shrinkagePercent: Double,
        defectPoints: Int,
        qualityGrade: RollQualityGrade,
        batchUsedIn: String,
        notes: String
    ) -> Unit,
    onUpdateRollInspection: (
        rollId: Long,
        defectPoints: Int,
        grade: RollQualityGrade,
        shadeBand: String,
        notes: String
    ) -> Unit,
    onDeleteRollLot: (Long) -> Unit
) {
    var selectedGradeFilter by remember { mutableStateOf<RollQualityGrade?>(null) }
    var showAddRollDialog by remember { mutableStateOf(false) }
    var rollToInspect by remember { mutableStateOf<FabricRollLot?>(null) }

    val filteredRolls = remember(rollLots, selectedGradeFilter) {
        rollLots.filter { selectedGradeFilter == null || it.qualityGrade == selectedGradeFilter }
    }

    val totalMeters = rollLots.sumOf { it.lengthMeters }
    val quarantinedCount = rollLots.count { it.qualityGrade == RollQualityGrade.QUARANTINED }

    Column(
        modifier = Modifier.fillMaxSize().testTag("roll_traceability_subview"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Summary strip & Add Roll button
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "4-Point Fabric Roll & Dye-Lot Traceability",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${rollLots.size} Rolls (${totalMeters.toInt()}m) • $quarantinedCount Quarantined • Tolerance: ≤ 28 pts/100yd²",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showAddRollDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                    modifier = Modifier.testTag("btn_add_roll_lot"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Log Roll Lot", fontSize = 12.sp)
                }
            }
        }

        // Grade Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedGradeFilter == null,
                    onClick = { selectedGradeFilter = null },
                    label = { Text("All Rolls (${rollLots.size})") }
                )
            }
            items(RollQualityGrade.values()) { grade ->
                FilterChip(
                    selected = selectedGradeFilter == grade,
                    onClick = { selectedGradeFilter = if (selectedGradeFilter == grade) null else grade },
                    label = { Text(grade.displayName) }
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredRolls, key = { it.id }) { roll ->
                val isQuarantined = roll.qualityGrade == RollQualityGrade.QUARANTINED
                val isBGrade = roll.qualityGrade == RollQualityGrade.B_GRADE
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("roll_card_${roll.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = roll.rollNumber,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = EmeraldLight.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Dye Lot: ${roll.dyeLotCode}",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = EmeraldDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Surface(
                                color = when {
                                    isQuarantined -> Color(0xFFFEE2E2)
                                    isBGrade -> AmberLight
                                    else -> EmeraldLight
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isQuarantined) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = when {
                                            isQuarantined -> Color(0xFFB91C1C)
                                            isBGrade -> AmberDark
                                            else -> EmeraldDark
                                        },
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = roll.qualityGrade.displayName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isQuarantined -> Color(0xFFB91C1C)
                                            isBGrade -> AmberDark
                                            else -> EmeraldDark
                                        }
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${roll.itemSku} • ${roll.itemName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Mill: ${roll.supplierName} • Shade Band: ${roll.shadeBand} • Batch: ${roll.batchUsedIn.ifBlank { "Unassigned" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Roll Length / Width", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "${roll.lengthMeters}m × ${roll.widthCm}cm (${roll.gsm} GSM)",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Shrinkage %", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "${roll.shrinkagePercent}%",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("4-Point Defects", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "${roll.defectPointsPer100SqYds} pts / 100yd²",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (roll.defectPointsPer100SqYds > 28) Color(0xFFDC2626) else EmeraldDark
                                )
                            }
                        }

                        if (roll.inspectorNotes.isNotBlank()) {
                            Text(
                                text = "QC Note: ${roll.inspectorNotes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { rollToInspect = roll },
                                modifier = Modifier.height(34.dp).testTag("btn_inspect_roll_${roll.id}")
                            ) {
                                Text("Log 4-Point Inspection / Grade", fontSize = 11.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { onDeleteRollLot(roll.id) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Roll", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddRollDialog) {
        val defaultItem = inventoryItems.firstOrNull()
        var rollNumber by remember { mutableStateOf("ROLL-${(System.currentTimeMillis() % 899) + 100}") }
        var dyeLotCode by remember { mutableStateOf("LOT-IND-26A") }
        var shadeBand by remember { mutableStateOf("A1 (Center Shade)") }
        var lengthMeters by remember { mutableStateOf("100.0") }
        var defectPoints by remember { mutableStateOf("8") }
        var supplierName by remember { mutableStateOf(defaultItem?.preferredSupplier ?: "Indus Spinning & Weaving Mills") }
        var notes by remember { mutableStateOf("Passed incoming 4-point shade & slub check") }

        AlertDialog(
            onDismissRequest = { showAddRollDialog = false },
            title = { Text("Register Fabric Roll & Dye Lot", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rollNumber,
                            onValueChange = { rollNumber = it },
                            label = { Text("Roll Number") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dyeLotCode,
                            onValueChange = { dyeLotCode = it },
                            label = { Text("Dye Lot Code") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = shadeBand,
                            onValueChange = { shadeBand = it },
                            label = { Text("Shade Band") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lengthMeters,
                            onValueChange = { lengthMeters = it },
                            label = { Text("Length (m)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = defectPoints,
                            onValueChange = { defectPoints = it },
                            label = { Text("Defect Pts / 100yd²") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = supplierName,
                            onValueChange = { supplierName = it },
                            label = { Text("Mill / Supplier") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("QC Inspector Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pts = defectPoints.toIntOrNull() ?: 8
                        val grade = when {
                            pts > 28 -> RollQualityGrade.QUARANTINED
                            pts > 15 -> RollQualityGrade.B_GRADE
                            else -> RollQualityGrade.A_GRADE
                        }
                        onAddRollLot(
                            rollNumber,
                            dyeLotCode,
                            defaultItem?.id ?: 1L,
                            defaultItem?.sku ?: "RM-COT-01",
                            defaultItem?.name ?: "100% Combed Cotton Poplin",
                            supplierName,
                            shadeBand,
                            lengthMeters.toDoubleOrNull() ?: 100.0,
                            180,
                            150,
                            1.5,
                            pts,
                            grade,
                            "Unassigned",
                            notes
                        )
                        showAddRollDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                ) {
                    Text("Save Roll Lot")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRollDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (rollToInspect != null) {
        val target = rollToInspect!!
        var pointsText by remember { mutableStateOf(target.defectPointsPer100SqYds.toString()) }
        var shadeText by remember { mutableStateOf(target.shadeBand) }
        var selectedGrade by remember { mutableStateOf(target.qualityGrade) }
        var notesText by remember { mutableStateOf(target.inspectorNotes) }

        AlertDialog(
            onDismissRequest = { rollToInspect = null },
            title = { Text("4-Point Defect Inspection • ${target.rollNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${target.itemName} (Dye Lot: ${target.dyeLotCode})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pointsText,
                            onValueChange = {
                                pointsText = it
                                val pts = it.toIntOrNull() ?: 0
                                selectedGrade = when {
                                    pts > 28 -> RollQualityGrade.QUARANTINED
                                    pts > 15 -> RollQualityGrade.B_GRADE
                                    else -> RollQualityGrade.A_GRADE
                                }
                            },
                            label = { Text("Defect Points / 100yd²") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = shadeText,
                            onValueChange = { shadeText = it },
                            label = { Text("Shade Band") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RollQualityGrade.values().forEach { grade ->
                            FilterChip(
                                selected = selectedGrade == grade,
                                onClick = { selectedGrade = grade },
                                label = { Text(grade.name.replace('_', ' '), fontSize = 10.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("QC Inspector Findings") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateRollInspection(
                            target.id,
                            pointsText.toIntOrNull() ?: target.defectPointsPer100SqYds,
                            selectedGrade,
                            shadeText,
                            notesText
                        )
                        rollToInspect = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                ) {
                    Text("Update Inspection")
                }
            },
            dismissButton = {
                TextButton(onClick = { rollToInspect = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SuppliersDirectorySubView(
    suppliers: List<Supplier>,
    currencyFormat: NumberFormat,
    onAddSupplier: (
        code: String,
        name: String,
        specialty: SupplierSpecialty,
        contact: String,
        country: String,
        currencyCode: String,
        leadTime: Int
    ) -> Unit,
    onPaySupplierAp: (supplierId: Long, amount: Double, memo: String) -> Unit,
    onDeleteSupplier: (Long) -> Unit
) {
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var supplierToPay by remember { mutableStateOf<Supplier?>(null) }

    val totalAp = suppliers.sumOf { it.apBalance }

    Column(
        modifier = Modifier.fillMaxSize().testTag("suppliers_directory_subview"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Supplier Mills & Dyehouse Directory",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${suppliers.size} Approved Vendors • Total Mill AP Balance: ${currencyFormat.format(totalAp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showAddSupplierDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                    modifier = Modifier.testTag("btn_add_supplier"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Mill", fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(suppliers, key = { it.id }) { sup ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("supplier_card_${sup.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = sup.code,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = EmeraldLight.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = sup.specialty.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = EmeraldDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Currency: ${sup.currencyCode} • Lead: ${sup.leadTimeDays}d",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "${sup.name} (${sup.country.ifBlank { "Global" }})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Contact: ${sup.contactPerson.ifBlank { "Procurement Desk" }} • Terms: ${sup.paymentTerms} • Rating: ${sup.qualityRating}★",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Accounts Payable (AP)", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = currencyFormat.format(sup.apBalance),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (sup.apBalance > 0) AmberDark else EmeraldDark
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (sup.apBalance > 0.01) {
                                    OutlinedButton(
                                        onClick = { supplierToPay = sup },
                                        modifier = Modifier.height(34.dp).testTag("btn_pay_supplier_${sup.id}")
                                    ) {
                                        Text("Settle AP", fontSize = 11.sp)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }
                                IconButton(
                                    onClick = { onDeleteSupplier(sup.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Supplier", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSupplierDialog) {
        var code by remember { mutableStateOf("SUP-${(System.currentTimeMillis() % 799) + 205}") }
        var name by remember { mutableStateOf("") }
        var contact by remember { mutableStateOf("") }
        var country by remember { mutableStateOf("Italy") }
        var currencyCode by remember { mutableStateOf("EUR") }
        var leadTime by remember { mutableStateOf("10") }
        var specialty by remember { mutableStateOf(SupplierSpecialty.FABRIC_WEAVING_MILL) }

        AlertDialog(
            onDismissRequest = { showAddSupplierDialog = false },
            title = { Text("Add Textile Mill / Supplier", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Supplier Code") },
                            modifier = Modifier.weight(0.4f)
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Mill / Company Name") },
                            modifier = Modifier.weight(0.6f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = country,
                            onValueChange = { country = it },
                            label = { Text("Country") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = contact,
                            onValueChange = { contact = it },
                            label = { Text("Contact Person") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currencyCode,
                            onValueChange = { currencyCode = it },
                            label = { Text("Currency (USD/EUR/JPY)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = leadTime,
                            onValueChange = { leadTime = it },
                            label = { Text("Lead Time (Days)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddSupplier(
                            code,
                            name.ifBlank { "Alpine Textile Mill" },
                            specialty,
                            contact,
                            country,
                            currencyCode.uppercase(),
                            leadTime.toIntOrNull() ?: 10
                        )
                        showAddSupplierDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                ) {
                    Text("Save Supplier")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSupplierDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (supplierToPay != null) {
        val sup = supplierToPay!!
        var amountText by remember { mutableStateOf(sup.apBalance.toString()) }
        var memoText by remember { mutableStateOf("Wire transfer settlement for ${sup.code}") }

        AlertDialog(
            onDismissRequest = { supplierToPay = null },
            title = { Text("Settle Supplier AP • ${sup.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Current AP Balance: ${currencyFormat.format(sup.apBalance)}", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Settlement Amount (USD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = memoText,
                        onValueChange = { memoText = it },
                        label = { Text("Payment Reference / Memo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            onPaySupplierAp(sup.id, amt, memoText)
                        }
                        supplierToPay = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                ) {
                    Text("Post AP Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { supplierToPay = null }) { Text("Cancel") }
            }
        )
    }
}
