import Foundation

/// Fuel purchased at a pump is not the same as fuel consumed during a trip.
/// This calculator estimates private consumption from aggregate private kilometres
/// and a user-supplied l/100 km rate, without storing private locations or timestamps.
struct FuelAllocation: Equatable {
    let privateKm: Double
    let estimatedLiters: Double
    let purchasedLiters: Double
    let allocatedLiters: Double
    let unallocatedLiters: Double
    let uncoveredLiters: Double
    let averagePrice: Double
    let estimatedCost: Double
    let excludedReceipts: Int

    var coveredFraction: Double {
        estimatedLiters > 0 ? allocatedLiters / estimatedLiters : 0
    }
}

enum PrivateFuelCalculator {
    static func fuelClass(_ name: String) -> String {
        let name = name.lowercased()
        if name.contains("diesel") || name.contains("nafta") || name.contains("b7") { return "diesel" }
        if name.contains("benz") || name.contains("e10") || name.contains("e5") { return "petrol" }
        return "unknown"
    }

    static func calculate(
        distanceKm: Double, litersPer100Km: Double,
        receipts: [(liters: Double, amount: Double, fuel: String)],
        vehicleFuel: String
    ) -> FuelAllocation {
        let km = distanceKm.isFinite && distanceKm > 0 ? distanceKm : 0
        let rate = litersPer100Km.isFinite && litersPer100Km > 0 && litersPer100Km <= 50
            ? litersPer100Km : 6.5
        let estimate = km * rate / 100
        let category = fuelClass(vehicleFuel)
        let matching = receipts.filter {
            $0.liters.isFinite && $0.liters > 0 &&
                category != "unknown" && fuelClass($0.fuel) == category
        }
        let purchased = matching.reduce(0.0) { $0 + $1.liters }
        let money = matching.reduce(0.0) { result, receipt in
            result + (receipt.amount.isFinite && receipt.amount > 0 ? receipt.amount : 0)
        }
        let unitPrice = purchased > 0 ? money / purchased : 0
        let allocated = min(purchased, estimate)
        return FuelAllocation(
            privateKm: km, estimatedLiters: estimate, purchasedLiters: purchased,
            allocatedLiters: allocated, unallocatedLiters: max(0, purchased - allocated),
            uncoveredLiters: max(0, estimate - allocated), averagePrice: unitPrice,
            estimatedCost: allocated * unitPrice,
            excludedReceipts: receipts.count - matching.count
        )
    }
}
