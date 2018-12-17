package xyz.mattishub.campusDual

import org.joda.time.DateTime
import org.joda.time.DateTimeZone

data class JsonLesson(val title: String = "",
                      val start: String = "",
                      val end: String = "",
                      val allDay: Boolean = false,
                      val description: String = "",
                      val color: String = "",
                      val editable: Boolean = false,
                      val room: String = "",
                      val sroom: String = "",
                      val instructor: String = "",
                      val sinstructor: String = "",
                      val remarks: String = "")

data class Lesson(var title: String = "",
                  val startEpoch: Long,
                  val endEpoch: Long,
                  val room: String,
                  val instructor: String) {
    val start: DateTime
        get() = DateTime(startEpoch * 1000, DateTimeZone.forOffsetHours(+1))
    val end: DateTime
        get() = DateTime(endEpoch * 1000, DateTimeZone.forOffsetHours(+1))

    var isFirstOfDay = false
}

