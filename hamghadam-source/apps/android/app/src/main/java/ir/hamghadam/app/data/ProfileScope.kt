package ir.hamghadam.app.data

/**
 * Data isolation between local accounts.
 *
 * Every row carries a `profileId`; a query for the active account must filter on
 * it. This helper exists so the rule lives in one place and can be unit-tested
 * without Room: the previous 0.3.0 behaviour (one implicit account) is expressed
 * as `profileId == ""`, which is exactly what migrated rows have.
 */
fun <T> List<T>.ownedBy(profileId: String, owner: (T) -> String): List<T> =
    filter { owner(it) == profileId }

/** True when the row is visible to [profileId]. */
fun <T> T.isOwnedBy(profileId: String, owner: (T) -> String): Boolean =
    owner(this) == profileId

/** The id a newly created row must be stamped with. */
fun newRowProfileId(activeProfileId: String): String =
    activeProfileId.ifBlank { DEFAULT_PROFILE_ID }
