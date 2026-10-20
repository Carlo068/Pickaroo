package com.example.pikaroo.ui.orders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.pikaroo.ui.orders.model.MapPoint
import com.example.pikaroo.ui.orders.view.OSM_MAP_TAG
import com.example.pikaroo.ui.orders.view.OsmMapView
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OsmMapViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val shop = MapPoint(19.4326, -99.1332)
    private val destination = MapPoint(19.4180, -99.1450)

    private fun description(): String? =
        composeRule.onNodeWithTag(OSM_MAP_TAG).fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)

    private fun show(driver: () -> MapPoint?) {
        composeRule.setContent {
            OsmMapView(
                shop = shop,
                destination = destination,
                driver = driver(),
                modifier = Modifier.fillMaxWidth().height(240.dp)
            )
        }
    }

    @Test
    fun theMapShowsTheShopTheDestinationAndNoDriverYet() {
        show { null }

        assertEquals("shop=19.4326,-99.1332;destination=19.4180,-99.1450;driver=none", description())
    }

    @Test
    fun theDriverPinAppearsMovesAndDisappears() {
        var driver by mutableStateOf<MapPoint?>(null)
        show { driver }
        assertEquals("shop=19.4326,-99.1332;destination=19.4180,-99.1450;driver=none", description())

        driver = MapPoint(19.4300, -99.1300)
        composeRule.waitForIdle()
        assertEquals("shop=19.4326,-99.1332;destination=19.4180,-99.1450;driver=19.4300,-99.1300", description())

        driver = MapPoint(19.4250, -99.1400)
        composeRule.waitForIdle()
        assertEquals("shop=19.4326,-99.1332;destination=19.4180,-99.1450;driver=19.4250,-99.1400", description())

        driver = null
        composeRule.waitForIdle()
        assertEquals("shop=19.4326,-99.1332;destination=19.4180,-99.1450;driver=none", description())
    }

    @Test
    fun theMapSurvivesBeingShownAndRemoved() {
        var visible by mutableStateOf(true)
        composeRule.setContent {
            if (visible) OsmMapView(shop, destination, null, Modifier.fillMaxWidth().height(240.dp))
        }
        composeRule.onNodeWithTag(OSM_MAP_TAG).assertExists()

        visible = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(OSM_MAP_TAG).assertDoesNotExist()

        visible = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(OSM_MAP_TAG).assertExists()
    }
}
