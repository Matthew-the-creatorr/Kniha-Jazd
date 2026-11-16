package sk.knihajazd.prototype

import kotlin.math.max
import kotlin.math.min

/**
 * Účtovný odhad: zakúpené palivo nie je to isté ako reálne spotrebované palivo.
 * Neuchováva súkromné lokality, časy ani jednotlivé GPS body.
 */
data class FuelTicketData(val liters: Double, val amount: Double, val fuel: String)

data class PrivateFuelSummary(
    val privateKm: Double,
    val estimatedConsumptionLiters: Double,
    val purchasedLiters: Double,
    val allocatedLiters: Double,
    val unallocatedPurchasedLiters: Double,
    val consumptionNotCoveredLiters: Double,
    val averagePricePerLiter: Double,
    val estimatedAllocatedCost: Double,
    val excludedReceipts: Int
) {
    val coveredFraction: Double
        get() = if (estimatedConsumptionLiters > 0) allocatedLiters / estimatedConsumptionLiters else 0.0
}

object PrivateFuelAllocator {
    fun fuelClass(fuel: String): String = when {
        fuel.contains("Diesel", true) || fuel.contains("Nafta", true) || fuel.contains("B7", true) -> "diesel"
        fuel.contains("Benz", true) || fuel.contains("E10", true) || fuel.contains("E5", true) -> "petrol"
        else -> "unknown"
    }

    fun calculate(
        totalPrivateKm: Double,
        consumptionPer100Km: Double,
        receipts: List<FuelTicketData>,
        vehicleFuel: String
    ): PrivateFuelSummary {
        val distance = totalPrivateKm.takeIf { it.isFinite() && it > 0 } ?: 0.0
        val rate = consumptionPer100Km.takeIf { it.isFinite() && it > 0 && it <= 50 } ?: 6.5
        val estimated = distance * rate / 100.0
        val desiredClass = fuelClass(vehicleFuel)
        val matching = receipts.filter {
            it.liters.isFinite() && it.liters > 0 &&
                desiredClass != "unknown" && fuelClass(it.fuel) == desiredClass
        }
        val purchased = matching.sumOf { it.liters }
        val purchasedCost = matching.sumOf { it.amount.takeIf { a -> a.isFinite() && a > 0 } ?: 0.0 }
        val price = if (purchased > 0) purchasedCost / purchased else 0.0
        val allocated = min(purchased, estimated)
        return PrivateFuelSummary(
            privateKm = distance,
            estimatedConsumptionLiters = estimated,
            purchasedLiters = purchased,
            allocatedLiters = allocated,
            unallocatedPurchasedLiters = max(0.0, purchased - allocated),
            consumptionNotCoveredLiters = max(0.0, estimated - allocated),
            averagePricePerLiter = price,
            estimatedAllocatedCost = allocated * price,
            excludedReceipts = receipts.size - matching.size
        )
    }
}
