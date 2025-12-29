package com.example.alpha_ai.data.models

data class History(
    var id:String?=null, val uid:String?=null, val taskType: String?=null,
    val input: String?=null, val response: String?=null){

    companion object {
        fun getHistoryList() = mutableListOf(
            History("gec", "gec","gec","My brother is more taller than I am, even though he is younger by two years.", "My brother is taller than I am, even though he is younger by two years."),
            History("gec", "gec","gec","انه يحب اكل الطعام بكثره", "إنه يحب أكل الطعام بكثرة ."),
            History("gec", "gec","gec","came to work everyday", "I came to work everyday."),
            History("gec", "gec","gec","The seats are not supposed for young people to sit down on those seats but for old people.", "The seats are not supposed for young people to sit down on those seats but for old people."),

        )

    }
}
