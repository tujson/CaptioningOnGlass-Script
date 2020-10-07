package edu.gatech.cog.script

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setMargins
import androidx.recyclerview.widget.LinearLayoutManager
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private lateinit var adapter: TextAdapter
    private lateinit var script: Script
    private val displayedText = mutableListOf<String>()

    private var isScriptA = true

    private var clickTime = System.currentTimeMillis()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isSpeechRec = false
    private var displayMode = 0 // 0 = Invisible, 1 = Vuzix, 2 = Glass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        setContentView(R.layout.activity_main)

        script = loadScript(R.raw.assemblya)
        setupSpeechRecognizer()

        adapter = TextAdapter(displayedText)
        rvText.adapter = adapter
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvText.layoutManager = layoutManager

        camera.setLifecycleOwner(this)
    }

    private fun loadScript(script: Int): Script {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val jsonAdapter = moshi.adapter(Script::class.java)
        val json = resources.openRawResource(script).bufferedReader().readText()
        return jsonAdapter.fromJson(json)!!
    }

    private fun switchScript() {
        isScriptA = !isScriptA

        val toastText = if (isScriptA) {
            script = loadScript(R.raw.assemblya)
            "Assembly A"
        } else {
            script = loadScript(R.raw.assemblyb)
            "Assembly B"
        }

        Toast.makeText(applicationContext, toastText, Toast.LENGTH_SHORT).show()
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer(
            SpeechConfig.fromSubscription(
                BuildConfig.AZURE_SUBSCRIPTION_KEY,
                BuildConfig.AZURE_SERVICE_REGION
            )
        )
        speechRecognizer?.recognizing?.addEventListener { _, speechRecognitionEventArgs ->
            runOnUiThread {
                tvRecognizing.text = speechRecognitionEventArgs.result.text
            }
        }
        speechRecognizer?.recognized?.addEventListener { _, speechRecognitionEventArgs ->
            displayedText.add(speechRecognitionEventArgs.result.text)
            runOnUiThread {
                tvRecognizing.text = ""
                adapter.notifyItemInserted(displayedText.size - 1)
                rvText.scrollToPosition(displayedText.size - 1)
            }
        }
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.v("onKeyDown", "$keyCode $event")
        when (event?.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                val clickDelta = System.currentTimeMillis() - clickTime
                if (clickDelta <= 500L) {
                    switchScript()
                } else {
                    clickTime = System.currentTimeMillis()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (!isSpeechRec) {
                    when {
                        script.currentIndex >= script.script.size -> {
                            displayedText.clear()
                            adapter.notifyDataSetChanged()
                            script.currentIndex = 0
                        }
                        else -> {
                            displayedText.add(script.script[script.currentIndex])
                            adapter.notifyItemInserted(displayedText.size - 1)
                            rvText.scrollToPosition(displayedText.size - 1)
                            script.currentIndex++
                        }
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                // Switch between script and speech recognition
                isSpeechRec = !isSpeechRec
                displayedText.clear()
                adapter.notifyDataSetChanged()

                if (isSpeechRec) {
                    tvRecognizing.visibility = View.VISIBLE
                    speechRecognizer?.startContinuousRecognitionAsync()
                } else {
                    tvRecognizing.text = ""
                    tvRecognizing.visibility = View.GONE
                    script.currentIndex = 0
                    speechRecognizer?.stopContinuousRecognitionAsync()
                }
                return true
            }
            KeyEvent.KEYCODE_B -> {
                when (displayMode) {
                    0 -> {
                        // Go into Vuzix display mode
                        llBackground.visibility = View.VISIBLE
                        val layoutParams = RelativeLayout.LayoutParams(180, 200)
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
                        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
                        layoutParams.setMargins(0, 0, 100, 0)
                        llBackground.layoutParams = layoutParams

                        displayMode = 1
                    }
                    1 -> {
                        // Go into Glass display mode
                        val layoutParams = RelativeLayout.LayoutParams(222, 150)
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                        layoutParams.setMargins(12)
                        llBackground.layoutParams = layoutParams

                        displayMode = 2
                    }
                    2 -> {
                        // Go into invisible display mode
                        llBackground.visibility = View.INVISIBLE

                        displayMode = 0
                    }
                }
                return true
            }
            else -> return super.onKeyDown(keyCode, event)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isSpeechRec) {
            speechRecognizer?.startContinuousRecognitionAsync()
        }
    }

    override fun onPause() {
        super.onPause()
        speechRecognizer?.stopContinuousRecognitionAsync()
    }
}
