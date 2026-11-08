package com.example.uade.rememberapp.platform.contacts

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

/**
 * Datos extraídos de un contacto seleccionado mediante el selector del sistema.
 */
data class ContactInfo(
    val displayName: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
)

/**
 * Utilidad para resolver nombre, número de teléfono y correo de un contacto seleccionado.
 */
object ContactPickerHelper {

    fun resolveContact(context: Context, contactUri: Uri): ContactInfo {
        var contactId: String? = null
        var displayName: String? = null

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
        )

        try {
            context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (idIndex >= 0) contactId = cursor.getString(idIndex)
                    if (nameIndex >= 0) displayName = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {
        }

        if (contactId == null) {
            return ContactInfo(displayName = displayName)
        }

        var phone: String? = null
        try {
            val phoneCursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null,
            )
            phoneCursor?.use { cursor ->
                var fallbackPhone: String? = null
                while (cursor.moveToNext()) {
                    val numIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val typeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                    val number = if (numIndex >= 0) cursor.getString(numIndex) else null
                    val type = if (typeIndex >= 0) cursor.getInt(typeIndex) else -1
                    if (number != null) {
                        if (type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
                            phone = number
                            break
                        } else if (fallbackPhone == null) {
                            fallbackPhone = number
                        }
                    }
                }
                if (phone == null) phone = fallbackPhone
            }
        } catch (_: Exception) {
        }

        var email: String? = null
        try {
            val emailCursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                arrayOf(contactId),
                null,
            )
            emailCursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val emailIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                    if (emailIndex >= 0) email = cursor.getString(emailIndex)
                }
            }
        } catch (_: Exception) {
        }

        val cleanedPhone = phone?.replace(Regex("[\\s\\-\\(\\)]"), "")

        return ContactInfo(
            displayName = displayName,
            phoneNumber = cleanedPhone,
            email = email?.trim(),
        )
    }
}
