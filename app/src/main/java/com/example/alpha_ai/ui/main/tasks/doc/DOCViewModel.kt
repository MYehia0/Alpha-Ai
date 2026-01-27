package com.example.alpha_ai.ui.main.tasks.doc

import androidx.databinding.ObservableField
import com.example.alpha_ai.core.base.BaseViewModel
//import com.example.alpha_ai.database.apis.OCRModelApi
import java.io.File
import java.io.FileInputStream
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
//import org.apache.pdfbox.pdmodel.PDDocument
//import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.xwpf.usermodel.XWPFDocument

class DOCViewModel : BaseViewModel(){
    var output = ObservableField<String>()
    var loadingVisibility = ObservableField<Boolean>()

    var file: File? = null

    fun submit(){
        if(file == null){
            return
        }
        // Do something with the extracted text
        loadingVisibility.set(true)
//        navigator?.showLoading("Loading...")
        val text = when (file?.extension) {
            "doc" -> {
                file?.absolutePath?.let { readDocFile(it) }
            }
            "docx" -> {
                file?.absolutePath?.let { readDocxFile(it) }
            }
//            "pdf" -> {
//                file?.absolutePath?.let { readPdfFile(it) }
//            }
            else -> {
                null
            }
        }
        output.set(text?:"Error: Response is null")
//        navigator?.hideLoading()
        loadingVisibility.set(false)
    }

    fun readDocFile(filePath: String): String {
        val fileInputStream = FileInputStream(filePath)
        val document = HWPFDocument(fileInputStream)
        val wordExtractor = WordExtractor(document)
        val text = wordExtractor.text
        wordExtractor.close()
        fileInputStream.close()
        return text
    }

    fun readDocxFile(filePath: String): String {
        val fileInputStream = FileInputStream(filePath)
        val document = XWPFDocument(fileInputStream)
        val paragraphs = document.paragraphs
        val stringBuilder = StringBuilder()
        for (paragraph in paragraphs) {
            stringBuilder.append(paragraph.text).append("\n")
        }
        fileInputStream.close()
        return stringBuilder.toString()
    }

//    private fun readPdfFile(filePath: String): String {
//        val file = File(filePath)
//        val document = PDDocument.load(file)
//        val pdfStripper = PDFTextStripper()
//        val text = pdfStripper.getText(document)
//        document.close()
//        return text
//    }

}