package com.example.airline

import com.example.aircraft.AircraftCatalog
import com.example.data.AirlineDoc
import com.example.data.AirlineRouteDoc
import com.example.data.FleetAircraftDoc
import com.example.world.BangladeshGeoData
import kotlin.math.roundToLong

data class AirlineCycleReport(
    val grossRevenueCoins: Long,
    val fuelExpenseCoins: Long,
    val crewSalaryCoins: Long,
    val airportFeeCoins: Long,
    val maintenanceCoins: Long,
    val netProfitCoins: Long,
    val passengersCarried: Int
)

object AirlineEconomyEngine {
    fun computeDemandScore(
        originIcao: String,
        destinationIcao: String,
        ticketPriceCoins: Long,
        reputation: Long
    ): Long {
        val orig = BangladeshGeoData.findAirport(originIcao)
        val dest = BangladeshGeoData.findAirport(destinationIcao)
        val distNm = BangladeshGeoData.distanceNm(orig, dest).coerceAtLeast(45.0)
        val fairFare = (distNm * 24.0).roundToLong().coerceAtLeast(1200L)
        val priceFactor = (fairFare.toDouble() / ticketPriceCoins.coerceAtLeast(500L).toDouble()).coerceIn(0.3, 1.5)
        val repFactor = 0.65 + (reputation / 200.0)
        return (62.0 * priceFactor * repFactor).roundToLong().coerceIn(10L, 100L)
    }

    fun simulateOperatingCycle(
        airline: AirlineDoc,
        fleet: List<FleetAircraftDoc>,
        routes: List<AirlineRouteDoc>
    ): AirlineCycleReport {
        val activeRoutes = routes.filter { it.active }
        if (activeRoutes.isEmpty()) {
            val crewSalary = airline.pilotsHired * 350L + airline.cabinCrewHired * 140L
            return AirlineCycleReport(0L, 0L, crewSalary, 0L, 0L, -crewSalary, 0)
        }

        var revenue = 0L
        var fuel = 0L
        var airportFees = 0L
        var maintenance = 0L
        var totalPax = 0

        for (route in activeRoutes) {
            val assignedAircraft = fleet.firstOrNull { it.id == route.assignedFleetId } ?: fleet.firstOrNull()
            val spec = AircraftCatalog.findById(assignedAircraft?.aircraftModelId ?: "atr72")
            val loadFactor = (route.demandScore / 100.0).coerceIn(0.25, 0.96)
            val pax = (spec.passengerCapacity * loadFactor).toInt().coerceAtLeast(12)
            totalPax += pax
            revenue += pax * (route.ticketPriceCoins / 18L).coerceAtLeast(25L)

            val distNm = BangladeshGeoData.distanceNm(
                BangladeshGeoData.findAirport(route.originIcao),
                BangladeshGeoData.findAirport(route.destinationIcao)
            )
            fuel += (distNm * spec.fuelBurnKgPerSecAtMax * 9.5).roundToLong()
            airportFees += if (spec.id == "b777") 950L else 450L
            maintenance += 280L
        }

        val crewSalary = airline.pilotsHired * 350L + airline.cabinCrewHired * 140L
        val totalExpenses = fuel + crewSalary + airportFees + maintenance
        val net = revenue - totalExpenses
        return AirlineCycleReport(
            grossRevenueCoins = revenue,
            fuelExpenseCoins = fuel,
            crewSalaryCoins = crewSalary,
            airportFeeCoins = airportFees,
            maintenanceCoins = maintenance,
            netProfitCoins = net,
            passengersCarried = totalPax
        )
    }
}
