package sk.spacirkovnik.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GameAccessTest {

    private fun access(
        status: GameStatus?,
        signedIn: Boolean = false,
        activated: Boolean = false,
        testGame: Boolean = false,
    ) = resolveGameAccess(status, signedIn, activated, testGame)

    @Test
    fun activeGameIsAlwaysUnlocked() {
        assertEquals(GameAccess.UNLOCKED, access(GameStatus.ACTIVE))
        assertEquals(GameAccess.UNLOCKED, access(GameStatus.ACTIVE, signedIn = true))
    }

    @Test
    fun freeWithLoginNeedsSignIn() {
        assertEquals(GameAccess.FREE_AFTER_SIGN_IN, access(GameStatus.FREE_WITH_LOGIN))
        assertEquals(GameAccess.UNLOCKED, access(GameStatus.FREE_WITH_LOGIN, signedIn = true))
    }

    @Test
    fun purchasableGame() {
        assertEquals(GameAccess.SIGN_IN_TO_BUY, access(GameStatus.PURCHASABLE))
        assertEquals(GameAccess.BUY, access(GameStatus.PURCHASABLE, signedIn = true))
        assertEquals(GameAccess.UNLOCKED, access(GameStatus.PURCHASABLE, signedIn = true, activated = true))
    }

    @Test
    fun activationOrTestGameUnlocksAnyStatus() {
        assertEquals(GameAccess.UNLOCKED, access(GameStatus.COMING_SOON, signedIn = true, activated = true))
        assertEquals(GameAccess.UNLOCKED, access(GameStatus.HIDDEN, signedIn = true, testGame = true))
    }

    @Test
    fun comingSoonAndUnknownStayLocked() {
        assertEquals(GameAccess.COMING_SOON, access(GameStatus.COMING_SOON, signedIn = true))
        assertEquals(GameAccess.LOCKED, access(GameStatus.UNKNOWN, signedIn = true))
        assertEquals(GameAccess.LOCKED, access(null))
    }

    @Test
    fun offerAndSignInFlags() {
        assertEquals(true, GameAccess.BUY.isOffer)
        assertEquals(false, GameAccess.BUY.needsSignIn)
        assertEquals(true, GameAccess.SIGN_IN_TO_BUY.needsSignIn)
        assertEquals(false, GameAccess.COMING_SOON.isOffer)
        assertEquals(false, GameAccess.LOCKED.isUnlocked)
    }
}
