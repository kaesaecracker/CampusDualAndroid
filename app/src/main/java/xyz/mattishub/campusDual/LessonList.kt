package xyz.mattishub.campusDual

import org.joda.time.DateTime

class LessonList(val _list: List<Lesson>) {
    val size: Int
        get() = _list.size

    operator fun get(position: Int): Lesson = _list[position]

    /**
     * From the non passed lessons it returns the first day
     */
    fun getCurrentOrNextDay(): LessonList {
        return whereDayNotInPast().firstDay()
    }

    fun whereDayNotInPast(): LessonList {
        val result = mutableListOf<Lesson>()
        val today = DateTime(AppTimeZone)

        for (lesson in _list) {
            if (lesson.end.isBeforeNow && lesson.start.dayOfYear != today.dayOfYear)
                continue
            result.add(lesson)
        }

        return LessonList(result)
    }

    fun whereEndNotInPast(): LessonList = LessonList(_list.filter {
        return@filter it.end.isAfterNow
    })


    fun firstDay(): LessonList {
        val result = mutableListOf<Lesson>()
        if (_list.isEmpty()) return LessonList(result)

        result.add(_list.first())
        for (lesson in _list.subList(1, _list.size - 1)) {
            if (lesson.isFirstOfDay) break;
            result.add(lesson)
        }

        return LessonList(result)
    }

}