package it.maicol07.gamerlogue.extensions

// Source - https://stackoverflow.com/a/78801411
// Posted by Saher Al-Sous, modified by community. See post 'Timeline' for change history
// Retrieved 2025-12-25, License - CC BY-SA 4.0

import kotlin.math.pow
import kotlin.math.round

fun Double.roundTo(dec: Int): Double {
    return round(this * 10.0.pow(dec)) / 10.0.pow(dec)
}
