package it.maicol07.gamerlogue.ui.components

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write

/**
 * The system "save as" screen: no storage permission, and the user chooses where the file goes.
 * Needs `FileKit.init(activity)`, which AppActivity calls.
 */
actual suspend fun saveFile(bytes: ByteArray, fileName: String): Boolean {
    val file = FileKit.openFileSaver(
        suggestedName = fileName.substringBeforeLast('.'),
        defaultExtension = fileName.substringAfterLast('.', missingDelimiterValue = "")
    ) ?: return false
    file.write(bytes)
    return true
}
