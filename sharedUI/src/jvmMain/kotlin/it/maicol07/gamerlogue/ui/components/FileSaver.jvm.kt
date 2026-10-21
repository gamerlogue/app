package it.maicol07.gamerlogue.ui.components

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write

/** The desktop "save as" dialog. */
actual suspend fun saveFile(bytes: ByteArray, fileName: String): Boolean {
    val file = FileKit.openFileSaver(
        suggestedName = fileName.substringBeforeLast('.'),
        defaultExtension = fileName.substringAfterLast('.', missingDelimiterValue = "")
    ) ?: return false
    file.write(bytes)
    return true
}
