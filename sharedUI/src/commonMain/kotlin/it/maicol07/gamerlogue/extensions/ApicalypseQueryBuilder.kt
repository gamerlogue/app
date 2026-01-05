package it.maicol07.gamerlogue.extensions

import at.released.igdbclient.apicalypse.ApicalypseQueryBuilder
import at.released.igdbclient.apicalypse.SortOrder
import at.released.igdbclient.dsl.field.IgdbRequestField
import at.released.igdbclient.dsl.field.IgdbRequestFieldDsl
import at.released.igdbclient.dsl.field.field
import at.released.igdbclient.model.Game
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun ApicalypseQueryBuilder.sort(field: IgdbRequestField<*>, order: SortOrder) {
    this.sort(field.igdbFullName, order)
}

fun ApicalypseQueryBuilder.where(whereBuilder: ApicalypseQueryBuilderWhereBuilder.() -> Unit) {
    this.where(
        ApicalypseQueryBuilderWhereBuilder()
            .apply { whereBuilder() }
            .build()
    )
}

class ApicalypseQueryBuilderWhereBuilder() {
    private val wheres = mutableListOf<String>()

    fun build(): String = wheres.joinToString(" & ")

    fun or(other: ApicalypseQueryBuilderWhereBuilder.() -> Unit) {
        val otherWheres = ApicalypseQueryBuilderWhereBuilder()
            .apply { other() }
            .build()
        if (otherWheres.isNotEmpty()) {
            val current = this.build()
            wheres.clear()
            wheres.add("$current | ($otherWheres)")
        }
    }

    /** Exact match equal. */
    infix fun String.equals(value: String) = wheres.add("$this = $value")

    /** Exact match not equal. */
    infix fun String.notEquals(value: String) = wheres.add("$this != $value")

    /** Greater than. */
    infix fun String.greaterThan(value: Number) = wheres.add("$this > $value")

    /** Greater than or equal to. */
    infix fun String.greaterThanOrEqual(value: Number) = wheres.add("$this >= $value")

    /** Less than. */
    infix fun String.lessThan(value: Number) = wheres.add("$this < $value")

    /** Less than or equal to. */
    infix fun String.lessThanOrEqual(value: Number) = wheres.add("$this <= $value")

    /** Prefix: Exact match on the beginning of the string, can end with anything. (Case insensitive). */
    infix fun String.startsWith(value: String) = wheres.add("$this ~ $value*")

    /** Prefix: Exact match on the beginning of the string, can end with anything. (Case sensitive). */
    infix fun String.startsWithCaseSensitive(value: String) = wheres.add("$this = $value*")

    /** Postfix: Exact match on the end of the string, can start with anything. (Case insensitive). */
    infix fun String.endsWith(value: String) = wheres.add("$this ~ *$value")

    /** Postfix: Exact match on the end of the string, can start with anything. (Case sensitive). */
    infix fun String.endsWithCaseSensitive(value: String) = wheres.add("$this = *$value")

    /** Infix: Exact match anywhere in the string. (Case insensitive). */
    infix fun String.contains(value: String) = wheres.add("$this ~ *$value*")

    /** Infix: Exact match anywhere in the string. (Case sensitive). */
    infix fun String.containsCaseSensitive(value: String) = wheres.add("$this = *$value*")

    /** The value is null. */
    fun String.isNull() = wheres.add("$this = null")

    /** The value is not null. */
    fun String.isNotNull() = wheres.add("$this != null")

    /** The value exists within the list (AND between values). */
    infix fun String.`in`(values: Collection<String>) = wheres.add("$this = [${values.joinToString(",")}]")

    /** The value does not exist within the list (AND between values). */
    infix fun String.notIn(values: Collection<String>) = wheres.add("$this = ![${values.joinToString(",")}]")

    /** The value exists within the list (OR between values). */
    infix fun String.inAny(values: Collection<String>) = wheres.add("$this = (${values.joinToString(",")})")

    /** The value does not exist within the list (OR between values). */
    infix fun String.notInAny(values: Collection<String>) = wheres.add("$this = !(${values.joinToString(",")})")

    /** Exact match on arrays. (Does not work on ids, strings, etc). */
    infix fun String.matchesAll(values: Collection<String>) = wheres.add("$this = {${values.joinToString(",")}}")

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.equals */
    infix fun IgdbRequestField<*>.equals(value: String) = this.igdbFullName equals value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.notEquals */
    infix fun IgdbRequestField<*>.notEquals(value: String) = this.igdbFullName notEquals value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.greaterThan */
    infix fun IgdbRequestField<*>.greaterThan(value: Number) = this.igdbFullName greaterThan value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.greaterThanOrEqual */
    infix fun IgdbRequestField<*>.greaterThanOrEqual(value: Number) = this.igdbFullName greaterThanOrEqual value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.lessThan */
    infix fun IgdbRequestField<*>.lessThan(value: Number) = this.igdbFullName lessThan value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.lessThanOrEqual */
    infix fun IgdbRequestField<*>.lessThanOrEqual(value: Number) = this.igdbFullName lessThanOrEqual value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.startsWith */
    infix fun IgdbRequestField<*>.startsWith(value: String) = this.igdbFullName startsWith value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.startsWithCaseSensitive */
    infix fun IgdbRequestField<*>.startsWithCaseSensitive(
        value: String
    ) = this.igdbFullName startsWithCaseSensitive value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.endsWith */
    infix fun IgdbRequestField<*>.endsWith(value: String) = this.igdbFullName endsWith value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.endsWithCaseSensitive */
    infix fun IgdbRequestField<*>.endsWithCaseSensitive(value: String) = this.igdbFullName endsWithCaseSensitive value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.contains */
    infix fun IgdbRequestField<*>.contains(value: String) = this.igdbFullName contains value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.containsCaseSensitive */
    infix fun IgdbRequestField<*>.containsCaseSensitive(value: String) = this.igdbFullName containsCaseSensitive value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.isNull */
    fun IgdbRequestField<*>.isNull() = this.igdbFullName.isNull()

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.isNotNull */
    fun IgdbRequestField<*>.isNotNull() = this.igdbFullName.isNotNull()

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.in */
    infix fun IgdbRequestField<*>.`in`(values: Collection<String>) = this.igdbFullName `in` values

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.notIn */
    infix fun IgdbRequestField<*>.notIn(values: Collection<String>) = this.igdbFullName notIn values

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.inAny */
    infix fun IgdbRequestField<*>.inAny(values: Collection<String>) = this.igdbFullName inAny values

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.notInAny */
    infix fun IgdbRequestField<*>.notInAny(values: Collection<String>) = this.igdbFullName notInAny values

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.matchesAll */
    infix fun IgdbRequestField<*>.matchesAll(values: Collection<String>) = this.igdbFullName matchesAll values

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.equals */
    infix fun IgdbRequestFieldDsl<*, *>.equals(value: String) = (this.all.parent ?: this.all) equals value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.notEquals */
    infix fun IgdbRequestFieldDsl<*, *>.notEquals(value: String) = (this.all.parent ?: this.all) notEquals value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.greaterThan */
    infix fun IgdbRequestFieldDsl<*, *>.greaterThan(value: Number) = (this.all.parent ?: this.all) greaterThan value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.greaterThanOrEqual */
    infix fun IgdbRequestFieldDsl<*, *>.greaterThanOrEqual(
        value: Number
    ) = (this.all.parent ?: this.all) greaterThanOrEqual value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.lessThan */
    infix fun IgdbRequestFieldDsl<*, *>.lessThan(value: Number) = (this.all.parent ?: this.all) lessThan value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.lessThanOrEqual */
    infix fun IgdbRequestFieldDsl<*, *>.lessThanOrEqual(
        value: Number
    ) = (this.all.parent ?: this.all) lessThanOrEqual value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.startsWith */
    infix fun IgdbRequestFieldDsl<*, *>.startsWith(value: String) = (this.all.parent ?: this.all) startsWith value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.startsWithCaseSensitive */
    infix fun IgdbRequestFieldDsl<*, *>.startsWithCaseSensitive(
        value: String
    ) = (this.all.parent ?: this.all) startsWithCaseSensitive value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.endsWith */
    infix fun IgdbRequestFieldDsl<*, *>.endsWith(value: String) = (this.all.parent ?: this.all) endsWith value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.endsWithCaseSensitive */
    infix fun IgdbRequestFieldDsl<*, *>.endsWithCaseSensitive(
        value: String
    ) = (this.all.parent ?: this.all) endsWithCaseSensitive value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.contains */
    infix fun IgdbRequestFieldDsl<*, *>.contains(value: String) = (this.all.parent ?: this.all) contains value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.containsCaseSensitive */
    infix fun IgdbRequestFieldDsl<*, *>.containsCaseSensitive(
        value: String
    ) = (this.all.parent ?: this.all) containsCaseSensitive value

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.isNull */
    fun IgdbRequestFieldDsl<*, *>.isNull() = (this.all.parent ?: this.all).isNull()

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.isNotNull */
    fun IgdbRequestFieldDsl<*, *>.isNotNull() = (this.all.parent ?: this.all).isNotNull()

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.in */
    infix fun IgdbRequestFieldDsl<*, *>.`in`(values: Collection<String>) = (this.all.parent ?: this.all) `in` values

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.notIn */
    infix fun IgdbRequestFieldDsl<*, *>.notIn(values: Collection<String>) = (this.all.parent ?: this.all) notIn values

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.inAny */
    infix fun IgdbRequestFieldDsl<*, *>.inAny(values: Collection<String>) = (this.all.parent ?: this.all) inAny values

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.notInAny */
    infix fun IgdbRequestFieldDsl<*, *>.notInAny(
        values: Collection<String>
    ) = (this.all.parent ?: this.all) notInAny values

    /** @see it.maicol07.gamerlogue.extensions.ApicalypseQueryBuilderWhereBuilder.matchesAll */
    infix fun IgdbRequestFieldDsl<*, *>.matchesAll(
        values: Collection<String>
    ) = (this.all.parent ?: this.all) matchesAll values
}

// Utility filters
@OptIn(ExperimentalTime::class)
fun ApicalypseQueryBuilderWhereBuilder.alreadyReleased() {
    Game.field.first_release_date lessThan (Clock.System.now().toEpochMilliseconds() / 1000)
}

@OptIn(ExperimentalTime::class)
fun ApicalypseQueryBuilderWhereBuilder.notYetReleased() {
    Game.field.first_release_date greaterThan (Clock.System.now().toEpochMilliseconds() / 1000)
}
