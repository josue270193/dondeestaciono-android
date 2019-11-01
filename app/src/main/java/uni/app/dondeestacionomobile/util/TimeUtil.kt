package uni.app.dondeestacionomobile.util

import org.threeten.bp.OffsetDateTime
import java.util.concurrent.TimeUnit

class TimeUtil {
    companion object {
        fun calculateTimeLapse(started: OffsetDateTime): String {
            val timeDifference = ""
            val now = OffsetDateTime.now()

            val duration = (now.toEpochSecond() - started.toEpochSecond()) * 1000

            val diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(duration)
            val diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration)
            val diffInHours = TimeUnit.MILLISECONDS.toHours(duration)
            val diffInDays = TimeUnit.MILLISECONDS.toDays(duration)

            if (diffInDays > 365) {
                return String.format("hace %d año", diffInDays / 365)
            } else if (diffInDays > 1) {
                return String.format("hace %d dias", diffInDays)
            } else if (diffInHours > 1) {
                return String.format("hace %d horas", diffInHours)
            } else if (diffInMinutes > 1) {
                return String.format("hace %d dias", diffInMinutes)
            } else if (diffInSeconds > 1) {
                return String.format("hace %d segundos", diffInSeconds)
            }
            return timeDifference
        }
    }
}