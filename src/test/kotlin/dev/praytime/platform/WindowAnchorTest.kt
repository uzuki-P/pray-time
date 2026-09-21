package dev.praytime.platform

import java.awt.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals

class WindowAnchorTest {

    private val bounds = Rectangle(0, 0, 1920, 1080)

    @Test
    fun topLeftPutsWindowAtScreenOriginPlusMargin() {
        assertEquals(
            java.awt.Point(12, 12),
            anchorPosition(WindowAnchor.TOP_LEFT, bounds, width = 296, height = 148, margin = 12),
        )
    }

    @Test
    fun topRightPutsWindowAtTopRightCorner() {
        assertEquals(
            java.awt.Point(1612, 12),
            anchorPosition(WindowAnchor.TOP_RIGHT, bounds, width = 296, height = 148, margin = 12),
        )
    }

    @Test
    fun middleRightCentersWindowVertically() {
        assertEquals(
            java.awt.Point(1612, 466),
            anchorPosition(WindowAnchor.MIDDLE_RIGHT, bounds, width = 296, height = 148, margin = 12),
        )
    }

    @Test
    fun bottomRightPutsWindowAboveBottomEdge() {
        assertEquals(
            java.awt.Point(1612, 920),
            anchorPosition(WindowAnchor.BOTTOM_RIGHT, bounds, width = 296, height = 148, margin = 12),
        )
    }

    @Test
    fun positionsAreRelativeToMonitorBounds() {
        val second = Rectangle(1920, 0, 1920, 1080)
        assertEquals(
            java.awt.Point(3532, 12),
            anchorPosition(WindowAnchor.TOP_RIGHT, second, width = 296, height = 148, margin = 12),
        )
        assertEquals(
            java.awt.Point(1932, 920),
            anchorPosition(WindowAnchor.BOTTOM_LEFT, second, width = 296, height = 148, margin = 12),
        )
    }

    @Test
    fun allAnchorsKeepWindowInsideBounds() {
        WindowAnchor.entries.forEach { anchor ->
            val position = anchorPosition(anchor, bounds, width = 296, height = 148, margin = 12)
            check(position.x >= bounds.x)
            check(position.y >= bounds.y)
            check(position.x + 296 <= bounds.maxX.toInt())
            check(position.y + 148 <= bounds.maxY.toInt())
        }
    }
}
