package sk.knihajazd.prototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateFuelAllocatorTest {
    @Test fun scannedFuelIsAllocatedToPrivateKmWithRemainder() {
        val s=PrivateFuelAllocator.calculate(
            232.0,6.5,
            listOf(
                FuelTicketData(16.0,27.20,"Diesel B7"),
                FuelTicketData(45.0,76.50,"Diesel B7"),
                FuelTicketData(39.0,66.30,"Diesel B7")
            ),
            "Diesel B7"
        )
        assertEquals(15.08,s.estimatedConsumptionLiters,0.00001)
        assertEquals(100.0,s.purchasedLiters,0.00001)
        assertEquals(15.08,s.allocatedLiters,0.00001)
        assertEquals(84.92,s.unallocatedPurchasedLiters,0.00001)
        assertEquals(0.0,s.consumptionNotCoveredLiters,0.00001)
        assertEquals(7.28,112.0*6.5/100.0*s.coveredFraction,0.00001)
        assertEquals(25.636,s.estimatedAllocatedCost,0.00001)
    }

    @Test fun limitedFuelIsSharedProportionallyNotDuplicated() {
        val s=PrivateFuelAllocator.calculate(
            232.0,6.5,listOf(FuelTicketData(10.0,17.0,"Diesel B7")),"Diesel B7"
        )
        val allocationPer112Km=112.0*6.5/100.0*s.coveredFraction
        val allocationForUnconverted8Km=8.0*6.5/100.0*s.coveredFraction
        assertEquals(10.0,s.allocatedLiters,0.00001)
        assertEquals(5.08,s.consumptionNotCoveredLiters,0.00001)
        assertEquals(10.0,allocationPer112Km*2+allocationForUnconverted8Km,0.00001)
    }

    @Test fun dieselIsNeverAllocatedToPetrolTrips() {
        val s=PrivateFuelAllocator.calculate(
            200.0,6.5,
            listOf(FuelTicketData(50.0,85.0,"Diesel B7"),FuelTicketData(12.0,21.0,"Benzín E10")),
            "Benzín E5"
        )
        assertEquals(12.0,s.purchasedLiters,0.00001)
        assertEquals(12.0,s.allocatedLiters,0.00001)
        assertEquals(1,s.excludedReceipts)
    }

    @Test fun noPrivateKmKeepsFuelUnallocated() {
        val s=PrivateFuelAllocator.calculate(0.0,6.5,listOf(FuelTicketData(16.0,28.0,"Diesel B7")),"Diesel B7")
        assertEquals(0.0,s.allocatedLiters,0.00001)
        assertEquals(16.0,s.unallocatedPurchasedLiters,0.00001)
        assertTrue(s.coveredFraction==0.0)
    }
}
