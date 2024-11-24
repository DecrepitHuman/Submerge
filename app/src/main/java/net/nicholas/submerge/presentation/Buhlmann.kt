package net.nicholas.submerge.presentation

import android.util.Log
import androidx.compose.runtime.MutableState
import kotlin.math.pow

class Buhlmann(
    // Variables
    val depth_m: MutableState<Double>, // Depth in meters
    val time: MutableState<Int>, // Time in minutes
    val fO2: MutableState<Double>, // Fraction of oxygen TODO: Implement gas switching - shouldn't be too hard?
    val pFactor: Int, // Safety factor 0 - 2 TODO: Need to properly implement this

    // Panel information
    val n2: MutableState<Double>,
    val noFly: MutableState<Int>,
    val noDeco: MutableState<Int>,
    val deco: MutableState<Int>,
) {
    private val depth = (depth_m.value - 1) / 10.0
    private val fN2 = 1.0 - fO2.value

    // Safety variables
    private val safetyStopDuration = 3 // Safety stop duration in minutes at 3 meters

    // Ascent / Descent rates
    private val ascentRate = 10 // Ascent rate in meters per minute
    private val ascentStepTime = 0.1 // Refresh tissues every 6 seconds
    private val descentRate = 10 // Descent rate in meters per minute
    private val descentStepTime = 0.1 // Refresh tissues every 6 seconds

    // Initiate tissue loading for atmospheric pressure
    // TODO: Jakob, does this actually need to happen?
    private var tissues = (0..15).map { compLoading(fN2, fN2, 100.0, hN2[it]) }

    private fun calculateCeilings(): List<Pair<Int, Double>> {
        return tissues.mapIndexed { i, it ->
            Pair(i, compCeiling(it, aN2[i], bN2[i]))
        }
    }

    // Finds the nearest set deco stop to the tissue ceiling depth in meters
    private fun nearestDecoStop(depth: Double): Int {
        return listOf(3, 6, 9, 12).filter { it >= depth }.minOrNull() ?: 12
    }

    // TODO: Something isn't right here
    // 1 minute stop at 3m for a 53 meter dive for 10 minutes can't be right
    private fun calculateDecompression(): List<Int> {
        val stops = mutableListOf(safetyStopDuration, 0, 0, 0) // 3m, 6m, 9m, 12m
        var lastStop = depth_m.value // Start ascent simulation from bottom depth

        var ceilings = calculateCeilings()
        var decoms = ceilings.filter { it.second > 1.0 }

        while (decoms.isNotEmpty()) {
            // Figure out which tissue needs to decompress at what depth
            val ceiling = decoms.maxOf { it.second }
            val nearestDeco = nearestDecoStop((ceiling - 1) * 10)

            // Calculate ascent time and loading
            val initialDepth = lastStop
            val ascent = lastStop - nearestDeco
            val stepDistance = ascentRate * ascentStepTime

            var currentDepth = initialDepth
            val totalSteps = (ascent / stepDistance).toInt()

            for (i in 1 .. totalSteps) {
                currentDepth -= stepDistance // Decrease depth by step distance

                // Refresh tissues while ascending
                tissues = tissues.mapIndexed { j, it ->
                    compLoading(it * fN2, ((currentDepth * 10) - 1) * fN2, ascentStepTime, hN2[j])
                }
            }


            // Re-calculate tissue loading after one minute of decompressing at nearest decompression depth
            tissues = tissues.mapIndexed { i, it ->
                compLoading(it * fN2, (1 + nearestDeco / 10) * fN2, 1.0, hN2[i])
            }

            // Keep track of the minutes decompressed
            stops[nearestDeco / 3 - 1] += 1
            lastStop = nearestDeco.toDouble()

            // Recheck ceilings for further decompression
            ceilings = calculateCeilings()
            decoms = ceilings.filter { it.second > 1.0 }
        }

        return stops
    }

    // Refreshes the data on the UI
    private fun refresh(
        decoT: Int,
        noDecoT: Int,
        noFlyT: Int
    ) {
        deco.value = decoT
        noDeco.value = noDecoT
        noFly.value = noFlyT
    }

    fun simulateDescent() {
        val stepDistance = descentRate * descentStepTime // Distance traveled per step
        var currentDepth = 0.0 // Starting depth
        val totalDescent = depth_m.value // Depth to descend to
        val totalSteps = (totalDescent / stepDistance).toInt()

        for (i in 1 .. totalSteps) {
            currentDepth += stepDistance

            // Refresh tissues while descending
            tissues = tissues.mapIndexed { j, it ->
                compLoading(it * fN2, ((currentDepth * 10) - 1) * fN2, descentStepTime, hN2[j])
            }
        }

        tissues = tissues.mapIndexed { i, it ->
            compLoading(it * fN2, depth * fN2, time.value.toDouble(), hN2[i])
        }
    }

    fun loadToBottom() {
        simulateDescent()
        val decoTime = calculateDecompression()
        val decoT = decoTime.sum()

        Log.d("Submerge", "$decoTime: $decoT")

        // Run calculations for no deco limits
        val noDecoT = if (decoT == safetyStopDuration) {
            val surfaceTissues = (0..15).map { compLoading(fN2, fN2, 100.0, hN2[it]) }
            tissues = surfaceTissues // Reset tissues for new simulations
            var noDecoT = 0

            val timeCopy = time.value // Store user defined time so we can adjust it for no deco simulations
            time.value += 1

            while (noDecoT < 99 && calculateDecompression().sum() == safetyStopDuration) {
                time.value += 1
                noDecoT += 1
                tissues = surfaceTissues
            }

            time.value = timeCopy
            noDecoT -= time.value // Only show no deco after bottom time

            noDecoT
        } else { 0 }

        refresh(decoT - safetyStopDuration, noDecoT, 0) // TODO: Implement no-fly logic
    }

    companion object {
        // https://en.wikipedia.org/wiki/B%C3%BChlmann_decompression_algorithm#Versions
        // Tissue half-times
        val hN2 = listOf(
            5.0, 8.0, 12.5, 18.5,
            27.0, 38.3, 54.3, 77.0,
            109.0, 146.0, 187.0, 239.0,
            305.0, 390.0, 498.0, 635.0
        )

        // Pre-calculated to save resources
        // a = 2 * (tht ** -1/3)
        val aN2 = listOf(
            1.1696, 1.0, 0.8618, 0.7562,
            0.62, 0.5043, 0.441, 0.4,
            0.375, 0.35, 0.3295, 0.3065,
            0.2835, 0.261, 0.248, 0.2327
        )

        // Pre-calculated to save resources
        // b = 1.005 - (tht ** -1/2)
        val bN2 = listOf(
            0.5578, 0.6514, 0.7222, 0.7825,
            0.8126, 0.8434, 0.8693, 0.8910,
            0.9092, 0.9222, 0.9319, 0.9403,
            0.9477, 0.9544, 0.9602, 0.9653
        )

        // This tells us the loading (bar) of a tissue compartment at a depth (bar) for a given time (minutes)
        fun compLoading(Pbegin: Double, Pgas: Double, te: Double, tht: Double): Double {
            /**
             *  @param Pbegin: Inert gas pressure in compartment before exposure
             *  @param Pgas: Inert gas pressure in the compartment after the exposure time (ATM * 0.79 N2 or other mixture)
             *  @param te: Exposure time in minutes
             *  @param tht: Tissue compartment half time in minutes
             *  @return The gas loading of a tissue compartment (ATA)
             */
            return Pbegin + (Pgas - Pbegin) * (1 - 2.0.pow(-te / tht))
        }

        // This tells us a compartments maximum depth it can ascend to without bubble formation
        fun compCeiling(Pcomp: Double, a: Double, b: Double): Double {
            /**
             * @param Pcomp: Inert gas pressure in compartment
             * @return The ceiling pressure of a tissue compartment before bubble formation occurs
             *           if below 1 bar, then we can ascend to surface without bubbles
             */
            return (Pcomp - a) * b
        }

        // This tells us what tissue compartment is the most loaded, and to what depth (bar) it can ascend to without bubble formation
        fun ceiling(Pbegin: Double, depth: Int, te: Double): Double {
            /**
             *  @param Pbegin: Inert gas pressure in compartment before exposure
             *  @param depth: Depth in bar
             *  @param te: Exposure time in minutes
             *  @return The shallowest depth possible to ascend to without bubble formation
             */
            return (0..15).maxOf { compCeiling(compLoading(Pbegin, depth * Pbegin, te, hN2[it]), aN2[it], bN2[it]) }
        }

    }
}

//fun main() {
//      // Dive parameters
//    val fN2 = 0.79
//    val depth = 4 // bar
//    val time = 10.0 // minutes
//    val compartment = 5
//
//    val loading = compLoading(fN2, depth * fN2, time, hN2[compartment - 1])
//    println(loading)
//    println(compCeiling(loading, aN2[compartment - 1], bN2[compartment - 1]))
//
//    println(ceiling(0.79, 4, 10.0))
//}