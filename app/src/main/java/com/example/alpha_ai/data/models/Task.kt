package com.example.alpha_ai.data.models

import com.example.alpha_ai.R

data class Task(val id:String?=null, val taskName: Int?=null, val taskImageId: Int?=null){

    companion object{
        fun getTasksList() = listOf(
            Task("gec", R.string.gec_title, R.drawable.gec),
            Task("ocr", R.string.ocr_title, R.drawable.ocr),
//            Task("doc", R.string.doc_title, R.drawable.doc),
            Task("stt", R.string.stt_title, R.drawable.sst),
            Task("er", R.string.er_title, R.drawable.er),
            Task("wr", R.string.wr_title, R.drawable.wr),
        )

    }
}
