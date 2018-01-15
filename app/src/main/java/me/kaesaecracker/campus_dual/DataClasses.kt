package me.kaesaecracker.campus_dual


data class Lesson(
        val title: String,
        val start: Long,
        val end: Long,
        val allDay: Boolean,
        val description: String,
        val color: String,
        val editable: Boolean,
        val room: String,
        val sroom: String,
        val instructor: String,
        val sinstructor: String,
        val remarks: String)