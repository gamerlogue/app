package it.maicol07.gamerlogue.data

/** Application error codes the profile endpoints answer with, beyond the generic ones. */
object ApiErrorCodes {
    const val PROFILE_PRIVATE = "profile_private"
    const val USER_BLOCKED = "user_blocked"
    const val REVIEW_REQUIRED = "review_required"

    /** Generic errors carry their HTTP status as the code; someone else's private list is a 404. */
    const val NOT_FOUND = "404"

    /** Another user's content that is hidden from the current one, which the screens show as such. */
    val HIDDEN_CONTENT = setOf(PROFILE_PRIVATE, USER_BLOCKED, NOT_FOUND)

    /** An attribute validation error, whose code is `<id>/data.attributes.<field>`. */
    fun isValidation(code: String): Boolean = "/data.attributes." in code
}
