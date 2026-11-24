package it.maicol07.gamerlogue.services

import at.released.igdbclient.model.Game
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import java.time.Instant

private fun day(iso: String) = Instant.parse("${iso}T00:00:00Z")

class ReleaseNewsTest : StringSpec({
    val from = LocalDate.parse("2026-11-23")
    val to = LocalDate.parse("2026-11-24")

    "out in the checked days, out a week later, and moved far enough to a day to come" {
        val backlog = listOf(
            Game(id = 1L, first_release_date = day("2026-11-23")),
            Game(id = 2L, first_release_date = day("2026-12-01")),
            Game(id = 3L, first_release_date = day("2027-03-12")),
            Game(id = 4L, first_release_date = day("2027-03-14")),
            Game(id = 5L, first_release_date = day("2026-11-10")),
            Game(id = 6L),
        )
        val known = mapOf(
            // Delayed by months: news.
            3 to day("2026-12-20").epochSecond,
            // Shifted by two days: IGDB settling the day, not news.
            4 to day("2027-03-12").epochSecond,
            // Moved, but into the past: not news.
            5 to day("2026-12-20").epochSecond,
            // Moved to a week from now: told as soon, not as moved.
            2 to day("2027-02-01").epochSecond,
        )

        val news = releaseNews(backlog, known, from, to)

        news.released.map { it.id } shouldBe listOf(1L)
        news.soon.map { it.id } shouldBe listOf(2L)
        news.moved.map { it.first.id to it.second } shouldBe listOf(3L to day("2026-12-20").epochSecond)
        news.dates.keys shouldBe setOf(1, 2, 3, 4, 5)
    }
})
