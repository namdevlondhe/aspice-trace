package dev.namdev.sample.climate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClimateControllerTest {

    private val controller = ClimateController()

    @Test
    @Requirement("SW-HMI-001", "SW-HMI-002")
    fun `temperature moves by half a degree and stays in range`() {
        controller.changeDriverTemp(+1)
        assertEquals(22.5f, controller.state.driverTemp)
        controller.changeDriverTemp(+100)
        assertEquals(28f, controller.state.driverTemp)
    }

    @Test
    @Requirement("SW-HMI-003")
    fun `fan stays between 1 and 6`() {
        controller.changeFan(+10)
        assertEquals(6, controller.state.fan)
    }

    @Test
    @Requirement("SW-HMI-004")
    fun `defrost forces A_C on, fresh air and max fan`() {
        controller.enterDefrost()
        assertTrue(controller.state.ac)
        assertEquals(6, controller.state.fan)
    }

    @Test
    @Requirement("SW-HMI-005")
    fun `leaving defrost restores the previous fan speed`() {
        controller.enterDefrost()
        controller.exitDefrost()
        assertEquals(2, controller.state.fan) // Fails until SW-HMI-005 is implemented.
    }
}
