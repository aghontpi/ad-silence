package bluepie.ad_silence

internal fun matchesEmptyKeyword(keyword: String, title: String, text: String, subText: String): Boolean =
    keyword.equals("allow-keyword-empty", ignoreCase = true) &&
        title.isEmpty() && text.isEmpty() && subText.isEmpty()
