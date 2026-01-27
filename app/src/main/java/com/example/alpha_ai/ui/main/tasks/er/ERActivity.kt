package com.example.alpha_ai.ui.main.tasks.er

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.alpha_ai.R
import com.example.alpha_ai.core.base.BaseActivity
import com.example.alpha_ai.core.common.NavigationDestination
import com.example.alpha_ai.databinding.ActivityErBinding
import com.example.alpha_ai.ui.auth.register.RegisterViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.getValue

class ERActivity : BaseActivity<ActivityErBinding, ERViewModel>() {
    override val viewModel: ERViewModel by viewModels()
    override fun inflateBinding(): ActivityErBinding {
        return DataBindingUtil.setContentView(this,R.layout.activity_er)
    }

    override fun handleNavigation(destination: NavigationDestination) {

    }
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording: Boolean = false
    private var audioFilePath: String? = null

    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val requestCode = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.vm = viewModel

        binding.content.btnSubmit.setOnClickListener {
            if(binding.content.btnSubmit.text.equals("Record")){
                if (checkPermissions()) {
                    startRecording()
                } else {
                    requestPermissions()
                }
            }
            else {
                stopRecording()
                viewModel.sendAudioToSST(audioFilePath!!)
            }
        }

        viewModel.output2.observe(this) { audioBytes ->
            if (audioBytes != null) {
                saveAndPlayAudio(audioBytes)
            }
        }
    }

    private fun checkPermissions(): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
            return true
        }
        return true
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == this.requestCode) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }
            if (allGranted) {
                startRecording()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun startRecording() {
        audioFilePath = "${externalCacheDir?.absolutePath}/recording.wav"
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(audioFilePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("STTActivity", "prepare() failed")
            }
            start()
            isRecording = true
            binding.content.btnSubmit.text = "Stop"
            binding.content.btnSubmit.icon = getDrawable(R.drawable.video)
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        isRecording = false
        mediaRecorder = null
        binding.content.btnSubmit.text = "Record"
        binding.content.btnSubmit.icon = getDrawable(R.drawable.mic)
    }

    private fun saveAndPlayAudio(audioBytes: ByteArray) {
        try {
            audioFilePath = "${externalCacheDir?.absolutePath}/output.wav"
            val file = File(audioFilePath!!)
            val fos = FileOutputStream(file)
            fos.write(audioBytes)
            fos.close()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFilePath)
                prepare()
                start()
            }
        } catch (e: IOException) {
            Log.e("TTSActivity", "saveAndPlayAudio() failed")
        }
    }
}