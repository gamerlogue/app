package it.maicol07.gamerlogue.extensions

import io.github.kdroidfilter.platformtools.Platform

/**
 * Android 12
 */
private const val AndroidMonetApiLevel = 31
/**
 * Android 13
 */
private const val AndroidPerAppLanguageApiLevel = 33

/**
 * The version of the platform (e.g., Android API level)
 */
expect val Platform.version: Int

/**
 * Returns true if the platform supports device colors (dynamic colors provided by Monet theming system (A12+)).
 */
fun Platform.supportsDeviceColors() = this == Platform.ANDROID && version >= AndroidMonetApiLevel

fun Platform.supportsSystemAppLanguage() = this == Platform.IOS_NATIVE || (this == Platform.ANDROID && version >= AndroidPerAppLanguageApiLevel)
