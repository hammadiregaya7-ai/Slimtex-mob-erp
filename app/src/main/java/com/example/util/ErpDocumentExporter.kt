package com.example.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.entity.CommercialOrder
import com.example.data.entity.CommercialOrderItem
import com.example.data.entity.ErpCurrency
import com.example.data.entity.ProductionMaterialItem
import com.example.data.entity.ProductionOrder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ErpDocumentExporter {

    fun formatCommercialDocumentText(
        order: CommercialOrder,
        items: List<CommercialOrderItem>
    ): String {
        val usdFormat = NumberFormat.getCurrencyInstance(Locale.US)
        val currency = ErpCurrency.fromCode(order.currencyCode)
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(order.timestamp))
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("TEXERP INDUSTRIAL MANUFACTURING & WHOLESALE SUITE")
        sb.appendLine("${order.type.displayName.uppercase()} • ${order.orderNumber}")
        sb.appendLine("==================================================")
        sb.appendLine("Date:             $dateStr")
        sb.appendLine("Counterparty:     ${order.partyName}")
        sb.appendLine("Status:           ${order.status}")
        sb.appendLine("Currency / Rate:  ${currency.code} (${currency.symbol}) @ ${order.exchangeRateToUsd} USD")
        sb.appendLine("Incoterms:        ${order.incoterms}")
        if (order.parentDocumentRef.isNotBlank()) {
            sb.appendLine("Chained From:     ${order.parentDocumentRef}")
        }
        if (order.linkedBatchNumber.isNotBlank()) {
            sb.appendLine("Linked Work Order:${order.linkedBatchNumber}")
        }
        if (order.approvedByRole.isNotBlank()) {
            sb.appendLine("Approved By:      ${order.approvedByRole}")
        }
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("LINE ITEMS:")
        items.forEachIndexed { idx, item ->
            sb.appendLine("${idx + 1}. ${item.itemName} (${item.quantity} ${item.unit}) @ ${usdFormat.format(item.unitPrice)} = ${usdFormat.format(item.lineTotal)}")
        }
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("Total (USD Base): ${usdFormat.format(order.totalAmount)}")
        if (currency != ErpCurrency.USD) {
            sb.appendLine("Total (${currency.code}):      ${currency.symbol}${String.format(Locale.US, "%,.2f", order.foreignCurrencyTotal)}")
        }
        sb.appendLine("Paid Amount:      ${usdFormat.format(order.paidAmount)}")
        sb.appendLine("Balance Due:      ${usdFormat.format((order.totalAmount - order.paidAmount).coerceAtLeast(0.0))}")
        if (order.packingListNumber.isNotBlank() || order.totalCartons > 0) {
            sb.appendLine("--------------------------------------------------")
            sb.appendLine("EXPORT SHIPPING PACKING LIST (${order.packingListNumber.ifBlank { "PKL-${order.orderNumber}" }})")
            sb.appendLine("Total Cartons:    ${order.totalCartons}")
            sb.appendLine("Net / Gross Wt:   ${order.netWeightKg} kg / ${order.grossWeightKg} kg")
            sb.appendLine("HS Tariff Code:   ${order.hsTariffCode}")
            sb.appendLine("Port of Loading:  ${order.portOfLoading}")
            sb.appendLine("Port of Discharge:${order.portOfDischarge}")
        }
        if (order.notes.isNotBlank()) {
            sb.appendLine("Notes: ${order.notes}")
        }
        sb.appendLine("==================================================")
        return sb.toString()
    }

    fun formatProductionCostSheetText(
        order: ProductionOrder,
        materials: List<ProductionMaterialItem>
    ): String {
        val usdFormat = NumberFormat.getCurrencyInstance(Locale.US)
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("TEXERP PRODUCTION WORK ORDER & COST SHEET")
        sb.appendLine("Batch Number:     ${order.batchNumber}")
        sb.appendLine("Finished Garment: ${order.finishedItemName}")
        sb.appendLine("Target Output:    ${order.targetQuantity} units")
        sb.appendLine("Status:           ${order.status}")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("BILL OF MATERIALS (BOM):")
        materials.forEachIndexed { idx, mat ->
            sb.appendLine("${idx + 1}. ${mat.itemName}: ${mat.quantityRequired} ${mat.unit} @ ${usdFormat.format(mat.unitCost)} = ${usdFormat.format(mat.totalCost)}")
        }
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("Raw Materials Total:   ${usdFormat.format(order.rawMaterialCost)}")
        sb.appendLine("Direct Tailoring Labor:${usdFormat.format(order.directLaborCost)}")
        sb.appendLine("Factory Overhead:      ${usdFormat.format(order.overheadCost)}")
        sb.appendLine("TOTAL BATCH COST:      ${usdFormat.format(order.totalBatchCost)}")
        sb.appendLine("UNIT PRODUCTION COST:  ${usdFormat.format(order.unitProductionCost)} / unit")
        sb.appendLine("==================================================")
        return sb.toString()
    }

    fun shareDocumentText(context: Context, title: String, content: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, "Export / Share $title").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun printDocumentHtml(context: Context, jobName: String, title: String, preformattedBody: String) {
        val webView = WebView(context)
        val escapedBody = preformattedBody
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8"/>
                <style>
                    body { font-family: monospace; padding: 24px; color: #0F172A; }
                    h2 { font-family: sans-serif; color: #059669; margin-bottom: 8px; }
                    pre { background: #F8FAFC; border: 1px solid #CBD5E1; padding: 16px; border-radius: 8px; font-size: 12px; line-height: 1.5; white-space: pre-wrap; }
                </style>
            </head>
            <body>
                <h2>$title</h2>
                <pre>$escapedBody</pre>
            </body>
            </html>
        """.trimIndent()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val adapter = view.createPrintDocumentAdapter(jobName)
                printManager?.print(jobName, adapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
