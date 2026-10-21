package it.maicol07.gamerlogue.ui.components

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download

/** A browser download, which goes where the browser keeps downloads. */
actual suspend fun saveFile(bytes: ByteArray, fileName: String): Boolean {
    FileKit.download(bytes, fileName)
    return true
}
