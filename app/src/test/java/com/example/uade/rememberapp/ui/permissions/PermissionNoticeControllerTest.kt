package com.example.uade.rememberapp.ui.permissions

import com.example.uade.rememberapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PermissionNoticeControllerTest {

    @Test
    fun `reportLimitations with empty set does not emit dialog or snackbar`() {
        val controller = PermissionNoticeController()

        controller.reportLimitations(emptySet(), showDialog = true)

        assertNull(controller.dialogNotice)
        assertNull(controller.snackbarNotice)
    }

    @Test
    fun `reportLimitations with showDialog true emits dialog notice and dismiss clears it`() {
        val controller = PermissionNoticeController()

        controller.reportLimitations(
            missing = setOf(LimitedCapability.Notifications, LimitedCapability.ExactAlarms),
            isPlaceOnly = false,
            showDialog = true,
        )

        val dialog = controller.dialogNotice
        assertNotNull(dialog)
        assertEquals(
            setOf(LimitedCapability.Notifications, LimitedCapability.ExactAlarms),
            dialog?.capabilities,
        )
        assertNull(controller.snackbarNotice)

        controller.dismissDialog()
        assertNull(controller.dialogNotice)
    }

    @Test
    fun `reportLimitations with showDialog false emits snackbar notice and can promote to dialog`() {
        val controller = PermissionNoticeController()

        controller.reportLimitations(
            missing = setOf(LimitedCapability.BackgroundLocation),
            isPlaceOnly = true,
            showDialog = false,
        )

        val snackbar = controller.snackbarNotice
        assertNotNull(snackbar)
        assertEquals(true, snackbar?.isPlaceOnly)
        assertNull(controller.dialogNotice)

        controller.showDialogFromSnackbar(snackbar!!)
        assertNull(controller.snackbarNotice)
        assertEquals(snackbar, controller.dialogNotice)
    }

    @Test
    fun `snackbarMessageResFor selects specific or multiple limitation string`() {
        assertEquals(
            R.string.permission_limitation_snackbar_notifications,
            snackbarMessageResFor(setOf(LimitedCapability.Notifications)),
        )
        assertEquals(
            R.string.permission_limitation_snackbar_exact_alarms,
            snackbarMessageResFor(setOf(LimitedCapability.ExactAlarms)),
        )
        assertEquals(
            R.string.permission_limitation_snackbar_location,
            snackbarMessageResFor(setOf(LimitedCapability.BackgroundLocation)),
        )
        assertEquals(
            R.string.permission_limitation_snackbar_multiple,
            snackbarMessageResFor(setOf(LimitedCapability.Notifications, LimitedCapability.ExactAlarms)),
        )
    }
}
