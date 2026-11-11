package sk.spacirkovnik.model

/**
 * Čo môže hráč so špacírkou spraviť — jediné miesto, kde sa rozhoduje o odomknutí.
 * Karta v zozname podľa toho len vyberie, čo ukáže (ikonu, tlačidlo, text).
 */
enum class GameAccess {
    /** Hrá sa: hra je voľná, zadarmo po prihlásení (a hráč je prihlásený), kúpená alebo testovacia. */
    UNLOCKED,

    /** Zadarmo po prihlásení, hráč ešte nie je prihlásený. */
    FREE_AFTER_SIGN_IN,

    /** Platená hra a hráč nie je prihlásený — bez prihlásenia nevieme, či ju už nemá kúpenú. */
    SIGN_IN_TO_BUY,

    /** Platená hra, hráč je prihlásený a ešte ju nemá. */
    BUY,

    /** Pripravujeme. */
    COMING_SOON,

    /** Iný zamknutý stav (neznámy stav z katalógu). */
    LOCKED;

    val isUnlocked: Boolean get() = this == UNLOCKED

    /** Hráč ju môže získať hneď — kúpou alebo prihlásením. */
    val isOffer: Boolean get() = this == FREE_AFTER_SIGN_IN || this == SIGN_IN_TO_BUY || this == BUY

    val needsSignIn: Boolean get() = this == FREE_AFTER_SIGN_IN || this == SIGN_IN_TO_BUY
}

fun resolveGameAccess(
    status: GameStatus?,
    isSignedIn: Boolean,
    isActivated: Boolean,
    isTestGame: Boolean,
): GameAccess = when {
    status == GameStatus.ACTIVE || isActivated || isTestGame -> GameAccess.UNLOCKED
    status == GameStatus.FREE_WITH_LOGIN ->
        if (isSignedIn) GameAccess.UNLOCKED else GameAccess.FREE_AFTER_SIGN_IN
    status == GameStatus.PURCHASABLE ->
        if (isSignedIn) GameAccess.BUY else GameAccess.SIGN_IN_TO_BUY
    status == GameStatus.COMING_SOON -> GameAccess.COMING_SOON
    else -> GameAccess.LOCKED
}
