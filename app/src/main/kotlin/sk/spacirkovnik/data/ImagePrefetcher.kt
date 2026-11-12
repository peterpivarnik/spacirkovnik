package sk.spacirkovnik.data

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import sk.spacirkovnik.model.GameDefinition

/**
 * Stiahne obrázky špacírky vopred do diskovej cache obrázkov, aby sa na trase načítali hneď aj pri
 * slabom signáli. Beží na pozadí a nič neblokuje; čo sa nestiahne, appka načíta ako doteraz až pri
 * zobrazení. Obrázky, ktoré už v cache sú, sa znova nesťahujú.
 */
object ImagePrefetcher {

    fun prefetch(context: Context, game: GameDefinition) {
        val appContext = context.applicationContext
        val loader = SingletonImageLoader.get(appContext)
        imageUrls(game).forEach { url ->
            loader.enqueue(
                ImageRequest.Builder(appContext)
                    .data(url)
                    // Ide len o uloženie na disk — do pamäte ich nenačítavame a dekódujeme
                    // v malom rozmere, aby to telefón zbytočne nezaťažilo.
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .size(64, 64)
                    .build()
            )
        }
    }

    fun imageUrls(game: GameDefinition): List<String> =
        game.screens.mapNotNull { it.imageUrl?.takeIf(String::isNotBlank) }.distinct()
}
