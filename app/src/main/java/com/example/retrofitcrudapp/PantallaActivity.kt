package com.example.retrofitcrudapp

import android.view.View
import android.os.Build
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

abstract class PantallaActivity : AppCompatActivity() {
    protected fun mostrar(root: View) {
        enableEdgeToEdge()
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            // Android 7 ya reduce la ventana con adjustResize; no descontar el teclado dos veces.
            val types = if (Build.VERSION.SDK_INT >= 30) {
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            } else WindowInsetsCompat.Type.systemBars()
            val bars = insets.getInsets(types)
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }
}
