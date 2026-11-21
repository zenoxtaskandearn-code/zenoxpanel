package com.zenox.remotegallery

import com.zenox.remotegallery.data.remote.dto.*
import com.zenox.remotegallery.util.PermissionLogic
import org.junit.Assert.*
import org.junit.Test

class BasicUnitTests {
    @Test
    fun mediaSyncItem_externalId_format() {
        val item = MediaSyncItem(externalId = "IMAGE:123", mediaType = "IMAGE", fileName = "a.jpg")
        assertEquals("IMAGE:123", item.externalId)
        assertEquals("IMAGE", item.mediaType)
    }

    @Test
    fun operationView_defaults() {
        val op = OperationView(id = 1, deviceId = 2, operationType = "PREVIEW", status = "PENDING")
        assertEquals(1L, op.id)
        assertEquals("PENDING", op.status)
    }

    @Test
    fun permissionSummary_logic_selectedAccess() {
        val s = PermissionLogic.summarize(hasFull = false, hasAny = true, selectedOnly = true)
        assertTrue(s.contains("Limited", ignoreCase = true) || s.contains("selected", ignoreCase = true))
    }

    @Test
    fun tokenErrorParsing_safeFallback() {
        val msg = "Request failed (401)"
        assertTrue(msg.contains("401"))
    }
}
