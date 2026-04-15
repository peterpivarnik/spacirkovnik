package sk.spacirkovnik.model

enum class Gender { MALE, FEMALE }

fun String.applyGender(gender: Gender): String {
    return Regex("\\{([^|{}]+)\\|([^|{}]+)\\}").replace(this) { match ->
        if (gender == Gender.MALE) match.groupValues[1] else match.groupValues[2]
    }
}
