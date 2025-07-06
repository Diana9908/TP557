package com.example.kws

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.InputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

class AssetHttpServer(
    private val context: Context,
    port: Int = 8080
) : NanoHTTPD(port) {

    init {
        // Configurar HTTPS
        try {
            // Cargar el keystore desde res/raw en formato PKCS12
            val keyStoreStream: InputStream = context.resources.openRawResource(R.raw.mykeystore)
            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(keyStoreStream, "ajafCR7*".toCharArray()) // Contraseña del keystore

            // Inicializar KeyManagerFactory
            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keyStore, "ajafCR7*".toCharArray()) // Contraseña de la clave

            // Inicializar SSLContext
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(keyManagerFactory.keyManagers, null, null)

            // Habilitar HTTPS usando SSLServerSocketFactory
            makeSecure(sslContext.serverSocketFactory, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri.removePrefix("/")
        val assetPath = if (uri.isEmpty()) "index.html" else uri

        return try {
            val mime = getMimeType(assetPath)
            val assetManager = context.assets
            val inputStream: InputStream = assetManager.open(assetPath)
            newChunkedResponse(Response.Status.OK, mime, inputStream)
        } catch (e: Exception) {
            // Archivo no encontrado
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Arquivo não encontrado: $assetPath")
        }
    }

    private fun getMimeType(path: String): String {
        return when {
            path.endsWith(".html") -> "text/html"
            path.endsWith(".js") -> "application/javascript"
            path.endsWith(".css") -> "text/css"
            path.endsWith(".wasm") -> "application/wasm"
            path.endsWith(".json") -> "application/json"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            else -> "application/octet-stream"
        }
    }
}