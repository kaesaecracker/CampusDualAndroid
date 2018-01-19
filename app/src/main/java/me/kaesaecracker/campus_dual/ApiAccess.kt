package me.kaesaecracker.campus_dual

import com.beust.klaxon.Klaxon
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.result.Result

val apiBaseUrl = "http://li1810-192.members.linode.com/cd_api/"

var data: Array<SchoolDay>? = null;
fun refreshData(userId: String, password: String) {
    // TODO response header handling
    Fuel.Companion.post(apiBaseUrl+"", listOf(
            Pair("userId", userId),
            Pair("password", password)
    )).responseString { _, _, result ->
        when (result) {
            is Result.Failure -> {
                // TODO do something useful
            }

            is Result.Success -> {
                data = Klaxon().parse<Array<SchoolDay>>(result.value)
            }
        }
    }
}