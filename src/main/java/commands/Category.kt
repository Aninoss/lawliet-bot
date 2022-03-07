package commands

enum class Category(val id: String, val emoji: String, val isIndependent: Boolean) {

    GIMMICKS("gimmicks", "🪀", true),
    AI_TOYS("aitoys", "🤖", true),
    CONFIGURATION("configuration", "⚙️", true),
    UTILITY("utility", "🔨", true),
    MODERATION("moderation", "👮", true),
    INFORMATION("information", "ℹ️", true),
    FISHERY_SETTINGS("fishery_settings_category", "⚙️", true),
    FISHERY("fishery_category", "🎣", true),
    CASINO("casino", "🎰", true),
    INTERACTIONS("interactions", "🫂", true),
    EXTERNAL("external_services", "📤", true),
    NSFW("nsfw", "🔞", true),
    SPLATOON_2("splatoon_2", "🦑", true),
    PATREON_ONLY("patreon_only", "⭐", false);

    companion object {

        @JvmStatic
        fun independentValues(): Array<Category> {
            return values()
                .filter { obj: Category -> obj.isIndependent }
                .toTypedArray()
        }

        @JvmStatic
        fun findCategoryByCommand(c: Class<out Command>): Category? {
            val categoryName = c.getPackage().name.split(".")[2]
            return when (categoryName) {
                "gimmickscategory" -> GIMMICKS
                "nsfwcategory" -> NSFW
                "configurationcategory" -> CONFIGURATION
                "utilitycategory" -> UTILITY
                "informationcategory" -> INFORMATION
                "splatoon2category" -> SPLATOON_2
                "interactionscategory" -> INTERACTIONS
                "externalcategory" -> EXTERNAL
                "fisherysettingscategory" -> FISHERY_SETTINGS
                "fisherycategory" -> FISHERY
                "casinocategory" -> CASINO
                "moderationcategory" -> MODERATION
                "aitoyscategory" -> AI_TOYS
                else -> null
            }
        }

        @JvmStatic
        fun fromId(id: String): Category? {
            return values()
                .filter { it.id == id }
                .getOrNull(0)
        }

    }

}