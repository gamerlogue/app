package it.maicol07.gamerlogue.extensions

import io.github.kdroidfilter.platformtools.Platform

actual val Platform.version: Int
    get() = System.getProperty("os.version")?.toIntOrNull() ?: 0
