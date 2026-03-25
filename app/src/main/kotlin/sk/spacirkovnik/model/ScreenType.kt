package sk.spacirkovnik.model

import com.google.gson.annotations.SerializedName

enum class ScreenType {
    @SerializedName("CONTINUE") CONTINUE,
    @SerializedName("BROWSE") BROWSE,
    @SerializedName("QUESTION") QUESTION,
    @SerializedName("NAVIGATION") NAVIGATION
}
