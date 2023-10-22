package app.suprsend.user.preference

data class Section(
    val name: String,
    val description: String,
    val subCategories: List<SubCategory>
)