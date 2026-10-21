package it.maicol07.gamerlogue.ui.components

/**
 * Saves [bytes] as [fileName], whose extension sets the file type: where the user picks (Android,
 * desktop) or as a browser download (web). Answers false when the user cancelled.
 */
expect suspend fun saveFile(bytes: ByteArray, fileName: String): Boolean
