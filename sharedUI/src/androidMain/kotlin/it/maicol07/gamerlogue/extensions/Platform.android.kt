package it.maicol07.gamerlogue.extensions

import io.github.kdroidfilter.platformtools.Platform

actual val Platform.version: Int
    @Suppress("SameReturnValue")
    get() = android.os.Build.VERSION.SDK_INT
