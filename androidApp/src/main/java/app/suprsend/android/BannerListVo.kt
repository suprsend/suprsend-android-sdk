package app.suprsend.android

data class BannerListVo(
    val list: List<BannerVo>
) : BaseItem() {
    override fun getItemId(): String {
        return "1"
    }
}
