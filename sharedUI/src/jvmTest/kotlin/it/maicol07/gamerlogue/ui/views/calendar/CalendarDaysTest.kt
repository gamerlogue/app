package it.maicol07.gamerlogue.ui.views.calendar

import at.released.igdbclient.model.Game
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import java.time.Instant

class CalendarDaysTest : StringSpec({
    "games are bucketed by UTC release day in input order, undated ones dropped" {
        val games = listOf(
            Game(id = 1L, first_release_date = Instant.parse("2026-10-11T00:00:00Z")),
            Game(id = 2L),
            Game(id = 3L, first_release_date = Instant.parse("2026-10-11T23:59:59Z")),
            Game(id = 4L, first_release_date = Instant.parse("2026-10-12T00:00:00Z")),
        )

        games.groupByReleaseDay().mapValues { (_, day) -> day.map(Game::id) } shouldBe mapOf(
            LocalDate(2026, 10, 11) to listOf(1L, 3L),
            LocalDate(2026, 10, 12) to listOf(4L),
        )
    }
})
