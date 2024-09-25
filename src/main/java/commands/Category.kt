package commands

enum class Category(val id: String, val emoji: String, val isIndependent: Boolean, val isNSFW: Boolean) {

    GIMMICKS("gimmicks", "🪀", true, false),
    AI_TOYS("aitoys", "🤖", true, false),
    CONFIGURATION("configuration", "⚙️", true, false),
    UTILITY("utility", "🔨", true, false),
    MODERATION("moderation", "👮", true, false),
    INFORMATION("information", "ℹ️", true, false),
    FISHERY_SETTINGS("fishery_settings_category", "⚙️", true, false),
    FISHERY("fishery_category", "🎣", true, false),
    CASINO("casino", "🎰", true, false),
    INVITE_TRACKING("invite_tracking_category", "✉️", true, false),
    BIRTHDAYS("birthdays", "🎂", true, false),
    INTERACTIONS("interactions", "🫂", true, false),
    NSFW_INTERACTIONS("nsfw_interactions", "❤️", true, true),
    EXTERNAL("external_services", "📤", true, false),
    NSFW("nsfw", "🔞", true, true),
    SPLATOON_2("splatoon_2", "🦑", true, false),
    PATREON_ONLY("patreon_only", "⭐", false, false);

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
                "nsfwinteractionscategory" -> NSFW_INTERACTIONS
                "externalcategory" -> EXTERNAL
                "fisherysettingscategory" -> FISHERY_SETTINGS
                "fisherycategory" -> FISHERY
                "casinocategory" -> CASINO
                "invitetrackingcategory" -> INVITE_TRACKING
                "birthdaycategory" -> BIRTHDAYS
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