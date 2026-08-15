package it.maicol07.gamerlogue.ui.views.game

import at.released.igdbclient.apicalypse.ApicalypseQueryBuilder
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * The detail query was hand-written as field strings before it moved to the generated DSL; this
 * pins the field list it produces to what the screen used to ask IGDB for, so a wrong DSL path
 * (or a dropped field) fails here instead of silently rendering an empty section.
 */
class GameDetailFieldsTest : StringSpec({
    "the detail query asks for exactly the fields the screen renders" {
        val expected = setOf(
            "age_ratings.category",
            "age_ratings.rating",
            "age_ratings.rating_cover_url",
            "alternative_names.comment",
            "alternative_names.name",
            "bundles.cover.image_id",
            "bundles.first_release_date",
            "bundles.id",
            "bundles.name",
            "bundles.rating",
            "category",
            "collections.games.cover.image_id",
            "collections.games.first_release_date",
            "collections.games.id",
            "collections.games.name",
            "collections.games.rating",
            "collections.name",
            "cover.image_id",
            "dlcs.cover.image_id",
            "dlcs.first_release_date",
            "dlcs.id",
            "dlcs.name",
            "dlcs.rating",
            "expanded_games.cover.image_id",
            "expanded_games.first_release_date",
            "expanded_games.id",
            "expanded_games.name",
            "expanded_games.rating",
            "expansions.cover.image_id",
            "expansions.first_release_date",
            "expansions.id",
            "expansions.name",
            "expansions.rating",
            "first_release_date",
            "franchise.name",
            "franchises.name",
            "game_engines.name",
            "game_modes.name",
            "genres.name",
            "involved_companies.company.name",
            "involved_companies.developer",
            "involved_companies.publisher",
            "keywords.name",
            "language_supports.language.name",
            "language_supports.language_support_type.name",
            "multiplayer_modes.campaigncoop",
            "multiplayer_modes.dropin",
            "multiplayer_modes.lancoop",
            "multiplayer_modes.offlinecoop",
            "multiplayer_modes.offlinecoopmax",
            "multiplayer_modes.offlinemax",
            "multiplayer_modes.onlinecoop",
            "multiplayer_modes.onlinecoopmax",
            "multiplayer_modes.onlinemax",
            "multiplayer_modes.splitscreen",
            "name",
            "parent_game.cover.image_id",
            "parent_game.first_release_date",
            "parent_game.id",
            "parent_game.name",
            "platforms.id",
            "platforms.name",
            "platforms.platform_logo.image_id",
            "player_perspectives.name",
            "ports.cover.image_id",
            "ports.first_release_date",
            "ports.id",
            "ports.name",
            "ports.rating",
            "rating",
            "rating_count",
            "release_dates.date",
            "release_dates.platform",
            "release_dates.release_region",
            "release_dates.status.name",
            "remakes.cover.image_id",
            "remakes.id",
            "remakes.name",
            "remasters.id",
            "remasters.name",
            "screenshots.image_id",
            "similar_games.cover.image_id",
            "similar_games.first_release_date",
            "similar_games.id",
            "similar_games.name",
            "similar_games.rating",
            "standalone_expansions.cover.image_id",
            "standalone_expansions.first_release_date",
            "standalone_expansions.id",
            "standalone_expansions.name",
            "standalone_expansions.rating",
            "status",
            "storyline",
            "summary",
            "themes.name",
            "version_parent.cover.image_id",
            "version_parent.first_release_date",
            "version_parent.id",
            "version_parent.name",
            "videos.name",
            "videos.video_id",
            "websites.category",
            "websites.trusted",
            "websites.url",
        )

        // The builder emits Apicalypse's short form, `f a,b,c;`.
        val query = ApicalypseQueryBuilder().fields(*DetailFields).build().toString()
        val actual = query.removePrefix("f ").removeSuffix(";").split(",").map { it.trim() }.toSet()

        // Split assertions: the two diffs read better than one 100-element mismatch.
        (actual - expected) shouldBe emptySet()
        (expected - actual) shouldBe emptySet()
    }
})
