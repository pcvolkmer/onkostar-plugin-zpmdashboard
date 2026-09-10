package dev.dnpm.zpmdashboard

import java.util.regex.Matcher
import java.util.regex.Pattern

class Einsendenummer(private val value: String?) {

    fun matches(other: Einsendenummer): Boolean {
        if (null == this.normalized() || null == other.normalized()) {
            return false
        }

        return this.normalized() == other.normalized()
    }

    fun normalized(): String? {
        fun keyFromMatcher(matcher: Matcher): String {
            val prefix = matcher.group("prefix")
            val year = matcher.group("year")
            val number = matcher.group("number")

            return String.format("%s/%s/%s", prefix, year, number)
        }

        if (null == value) {
            return null
        }

        val pattern1 = Pattern.compile("(?<prefix>[A-Z])/(\\d{2})?(?<year>\\d{2})/0*(?<number>\\d+)")
        val matcher1 = pattern1.matcher(value)

        val pattern2 = Pattern.compile("(?<prefix>[A-Z])\\s*0*(?<number>\\d+)[\\-/](?<year>\\d{2})")
        val matcher2 = pattern2.matcher(value)

        if (matcher1.find()) {
            return keyFromMatcher(matcher1)
        } else if (matcher2.find()) {
            return keyFromMatcher(matcher2)
        } else {
            return null
        }
    }


}