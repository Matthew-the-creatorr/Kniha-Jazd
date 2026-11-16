import Foundation

@main struct FuelRegression {
    static func check(_ condition: Bool, _ description: String) {
        if !condition {
            fputs("FAILED: \(description)\n", stderr)
            exit(1)
        }
    }
    static func close(_ a: Double, _ b: Double, _ description: String) {
        check(abs(a-b) < 0.00001, "\(description) (got \(a), expected \(b))")
    }
    static func main() {
        let all: [(liters: Double, amount: Double, fuel: String)] = [
            (16, 27.20, "Diesel B7"), (45, 76.50, "Diesel B7"), (39, 66.30, "Diesel B7")
        ]
        let summary = PrivateFuelCalculator.calculate(distanceKm:232,litersPer100Km:6.5,
            receipts:all,vehicleFuel:"Diesel B7")
        close(summary.estimatedLiters,15.08,"private estimate")
        close(summary.allocatedLiters,15.08,"allocated fuel")
        close(summary.unallocatedLiters,84.92,"remaining receipt liters")
        close(summary.estimatedCost,25.636,"weighted estimate EUR")
        close(112 * 6.5 / 100 * summary.coveredFraction,7.28,"per 112 km model trip")
        let limited=PrivateFuelCalculator.calculate(distanceKm:232,litersPer100Km:6.5,
            receipts:[(10,17,"Diesel B7")],vehicleFuel:"Diesel B7")
        close(limited.allocatedLiters,10,"limited fuel is not double counted")
        close(limited.uncoveredLiters,5.08,"uncovered estimate")
        close(2 * 7.28 * limited.coveredFraction + 0.52 * limited.coveredFraction,10,"proportional distribution")
        let incompatible=PrivateFuelCalculator.calculate(distanceKm:200,litersPer100Km:6.5,
            receipts:[(50,85,"Diesel B7"),(12,21,"Benzín E10")],vehicleFuel:"Benzín E5")
        close(incompatible.purchasedLiters,12,"no diesel mixed into petrol")
        check(incompatible.excludedReceipts == 1,"excluded incompatible receipt")
        let zero=PrivateFuelCalculator.calculate(distanceKm:0,litersPer100Km:6.5,
            receipts:[(16,28,"Diesel B7")],vehicleFuel:"Diesel B7")
        close(zero.allocatedLiters,0,"no private km means no allocated consumption")
        close(zero.unallocatedLiters,16,"purchased liters carried forward")
        print("PASS: iOS fuel accounting regression cases")
    }
}
