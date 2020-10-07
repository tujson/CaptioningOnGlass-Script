package edu.gatech.cog.script

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.android.synthetic.main.activity_main.*

private val TAG = MainActivity::class.java.simpleName

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: TextAdapter
    private lateinit var script: Script
    private val displayedText = mutableListOf<String>()

    private var isScriptA = true

    private var clickTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        setContentView(R.layout.activity_main)

        script = loadScript(R.raw.assemblya)

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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.v("onKeyDown", "$keyCode $event")
        if (event?.keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            val clickDelta = System.currentTimeMillis() - clickTime
            if (clickDelta <= 500L) {
                switchScript()
            } else {
                clickTime = System.currentTimeMillis()
            }
            return true
        } else if (event?.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
            || event?.keyCode == KeyEvent.KEYCODE_B
        ) {
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
            return true
        } else if (event?.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            rvText.visibility =
                if (rvText.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
            llBackground.visibility =
                if (llBackground.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
        }

        return super.onKeyDown(keyCode, event)
    }
}
