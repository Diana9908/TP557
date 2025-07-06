package com.example.kws

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var server: AssetHttpServer
    private val REQUEST_PERMISSIONS_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Asegura que se use el layout con WebView
        setContentView(R.layout.activity_main)

        // Verifica permisos de micrófono, audio settings y ubicación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            val permissionsToRequest = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_PERMISSIONS_CODE)
            }
        }

        // Inicia el servidor local de assets con HTTPS
        server = AssetHttpServer(this)
        server.start()

        // Configura la WebView
        webView = findViewById(R.id.webview)
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.domStorageEnabled = true

        // Permitir acceso a permisos como micrófono y ubicación
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }

        // Manejar certificados autofirmados
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(view: WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                handler?.proceed() // Ignorar errores SSL para certificados autofirmados (solo para desarrollo)
            }
        }

        // Carga el archivo principal desde el servidor local con HTTPS
        webView.loadUrl("https://localhost:8080/f5.html")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Todos los permisos concedidos, recargar WebView
                webView.reload()
            } else {
                // Algunos permisos denegados, podrías mostrar un mensaje al usuario
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
    }
}