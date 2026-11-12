package sk.spacirkovnik.model

/** Čo ukáže obrazovka na konci špacírky. [durationMinutes] je null, keď čas nedáva zmysel. */
data class FinishSummary(
    val title: String,
    /** Úvodný obrázok špacírky (prvý obrázok v hre), alebo null, ak žiadny nemá. */
    val imageUrl: String?,
    val stops: Int,
    val questions: Int,
    val durationMinutes: Int?,
)

/** Najdlhšia hra, ktorej čas ešte ukážeme; dlhší čas znamená prerušenie (napr. dohrané na druhý deň). */
const val MAX_SHOWN_DURATION_MINUTES = 5 * 60

fun finishSummary(title: String, screens: List<GameScreen>, startedAtMs: Long?, nowMs: Long): FinishSummary {
    val minutes = startedAtMs?.let { ((nowMs - it) / 60_000).toInt() }
        ?.takeIf { it in 1..MAX_SHOWN_DURATION_MINUTES }
    return FinishSummary(
        title = title,
        imageUrl = screens.firstNotNullOfOrNull { it.imageUrl?.takeIf(String::isNotBlank) },
        stops = screens.count { it.type == ScreenType.NAVIGATION },
        questions = screens.count { it.type == ScreenType.QUESTION },
        durationMinutes = minutes,
    )
}

/** Slovenský tvar podľa počtu: 1 zastávka, 2–4 zastávky, 0 a 5+ zastávok. */
fun slovakPlural(count: Int, one: String, few: String, many: String): String = when (count) {
    1 -> one
    in 2..4 -> few
    else -> many
}

/** „45 min", „1 h", „1 h 25 min". */
fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h $m min"
    }
}
