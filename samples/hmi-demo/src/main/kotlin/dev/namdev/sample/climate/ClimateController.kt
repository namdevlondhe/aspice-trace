package dev.namdev.sample.climate

data class Climate(
    val driverTemp: Float = 22f,
    val passengerTemp: Float = 22f,
    val fan: Int = 2,
    val ac: Boolean = false,
    val recirc: Boolean = false,
    val defrost: Boolean = false,
)

class ClimateController(
    private val minTemp: Float = 16f,
    private val maxTemp: Float = 28f,
    private val step: Float = 0.5f,
) {
    var state = Climate()
        private set

    @Requirement("SW-HMI-002")
    fun changeDriverTemp(steps: Int) {
        val target = (state.driverTemp + steps * step).coerceIn(minTemp, maxTemp)
        state = state.copy(driverTemp = target)
    }

    @Requirement("SW-HMI-003")
    fun changeFan(delta: Int) {
        state = state.copy(fan = (state.fan + delta).coerceIn(1, MAX_FAN))
    }

    @Requirement("SW-HMI-004")
    fun enterDefrost() {
        state = state.copy(defrost = true, ac = true, recirc = false, fan = MAX_FAN)
    }

    // Leaving defrost should restore the previous fan and A/C (SW-HMI-005): not implemented yet.
    fun exitDefrost() {
        state = state.copy(defrost = false)
    }

    @Requirement("SW-HMI-099") // Typo for an ID that no spec defines: reported as an unknown reference.
    fun toggleRecirc() {
        state = state.copy(recirc = !state.recirc)
    }

    private companion object {
        const val MAX_FAN = 6
    }
}
