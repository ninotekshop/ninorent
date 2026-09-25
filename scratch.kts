fun parseDateRangeFallback(range: String): Triple<String, String, String> {
    val regex = Regex("""^(.*?)\s*-\s*(.*?)\s*\((.*?)\)$""")
    val match = regex.find(range)
    if (match != null) {
        return Triple(match.groupValues[1].trim(), match.groupValues[2].trim(), match.groupValues[3].trim())
    }
    return Triple(range, "", "")
}

println(parseDateRangeFallback("19:00 20/09/2026 - 19:00 21/09/2026 (1.5 ngày)"))
println(parseDateRangeFallback("10/09/2026 - 12/09/2026 (3 ngày)"))