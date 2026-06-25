package it.maicol07.gamerlogue.extensions

import io.github.kdroidfilter.platformtools.Platform

actual val Platform.version: Int
    get() = android.os.Build.VERSION.SDK_INT
