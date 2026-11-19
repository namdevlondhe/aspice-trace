package dev.namdev.sample.climate

/** Illustrative UI layer: what each screen element shows and how big it is. */
@Requirement("SW-HMI-001")
class ClimateScreen(private val controller: ClimateController) {

    fun zoneLabels(): Pair<String, String> =
        controller.state.let { "%.1f °C".format(it.driverTemp) to "%.1f °C".format(it.passengerTemp) }

    @Requirement("SW-HMI-006")
    val minTouchTargetDp: Int = 76

    @Requirement("SW-HMI-007")
    fun theme(isNight: Boolean): String = if (isNight) "night" else "day"
}
