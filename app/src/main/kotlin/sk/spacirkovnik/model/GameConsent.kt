package sk.spacirkovnik.model

/**
 * Optional per-game consent shown before a game starts (e.g. sharing the player's e-mail
 * with an event organizer). Authored in the catalog (games-info.json) so the text can be
 * written/changed in Firebase without releasing a new app version.
 *
 * Bump [version] whenever the wording changes — players who accepted an older version are
 * then asked to consent again. The full terms live at [url] (on spacirkovnik.sk); [summary]
 * is the short, in-dialog explanation required for informed consent.
 */
data class GameConsent(
    val version: Int = 1,
    val title: String? = null,
    val summary: String? = null,
    val organizer: String? = null,
    val url: String? = null,
    val required: Boolean = true,
)
