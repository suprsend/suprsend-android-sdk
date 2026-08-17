package app.suprsend.android

data class ProductVo(
    val id: String,
    val title: String,
    val amount: Double,
    val url: String
) : BaseItem() {
    fun getPrice(): String {
        return "$amount/- RS"
    }

    override fun getItemId(): String {
        return id
    }
}
