package me.kaesaecracker.campusDual

import org.joda.time.DateTime

fun DateTime.getUnixTimestamp() = this.millis / 1000