package bluepie.ad_silence

data class CustomApp(
    val name: String,
    val packageName: String,
    val keywords: List<String>,
    var isEnabled: Boolean = true
)
