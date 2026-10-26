package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SupplierSpecialty(val displayName: String) {
    YARN_SPINNER("Yarn Spinner"),
    FABRIC_WEAVING_MILL("Fabric Weaving Mill"),
    DYEHOUSE_CHEMICALS("Dyehouse & Chemicals"),
    TRIMS_NOTIONS("Trims & Hardware")
}

@Entity(
    tableName = "suppliers",
    indices = [Index(value = ["code"], unique = true)]
)
data class Supplier(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val code: String,
    val name: String,
    val specialty: SupplierSpecialty = SupplierSpecialty.FABRIC_WEAVING_MILL,
    val contactPerson: String = "",
    val email: String = "",
    val phone: String = "",
    val country: String = "",
    val currencyCode: String = "USD",
    val leadTimeDays: Int = 10,
    val qualityRating: Double = 4.8,
    val paymentTerms: String = "Net 30",
    val apBalance: Double = 0.0,
    val notes: String = ""
)
