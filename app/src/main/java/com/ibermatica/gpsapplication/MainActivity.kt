package com.ibermatica.gpsapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLEncoder


class MainActivity : AppCompatActivity() {

    var gpx: String? = ""
    var url = "goo.gl/maps/HN4i77mkQiWKsU8v9" //link del google maps sin el https://

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val extra = intent.getStringExtra(Intent.EXTRA_TEXT)
        if(extra?.isNotEmpty() == true){
            url = extra.substringAfter("https://")
            defaultOperation()
        }
    }

    private fun defaultOperation(){
        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient().newBuilder()
                .build()

            url = URLEncoder.encode(url, "utf-8")

            findViewById<TextView>(R.id.tvRuta).text = "Convirtiendo enlace gmaps a GPX"
            Log.i("defaultOperation","Convirtiendo enlace gmaps a GPX")

            val request: Request = Request.Builder()
                .url("https://mapstogpx.com/load.php?d=default&lang=en&elev=off&tmode=off&pttype=fixed&o=gpx&cmt=off&desc=off&descasname=off&w=on&dtstr=20220509_103135&gdata=$url")
                .method("GET", null)
                .addHeader(
                    "Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
                )
                .addHeader(
                    "Accept-Language",
                    "es-ES,es;q=0.9,en-US;q=0.8,en;q=0.7,zh-CN;q=0.6,zh;q=0.5,de;q=0.4"
                )
                .addHeader("Cache-Control", "no-cache")
                .addHeader("Connection", "keep-alive")
                .addHeader("DNT", "1")
                .addHeader("Pragma", "no-cache")
                .addHeader("Referer", "https://mapstogpx.com/")
                .addHeader("Sec-Fetch-Dest", "document")
                .addHeader("Sec-Fetch-Mode", "navigate")
                .addHeader("Sec-Fetch-Site", "same-origin")
                .addHeader("Sec-Fetch-User", "?1")
                .addHeader("Upgrade-Insecure-Requests", "1")
                .addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.54 Safari/537.36"
                )
                .addHeader(
                    "sec-ch-ua",
                    "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"101\", \"Google Chrome\";v=\"101\""
                )
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("sec-ch-ua-platform", "\"Windows\"")
                .build()

            val response: Response = client.newCall(request).execute()
            Log.i("defaultOperation","Respuesta obtenida del servidor mapstogpx")
            gpx = response.body()?.string()
            Log.i("defaultOperation","Trasformacion finalizada.Escribiendo fichero GPX en local")

            runOnUiThread {
                findViewById<TextView>(R.id.tvRuta).text = gpx
                if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    writeGPXToFile()
                    findViewById<TextView>(R.id.tvRuta).text = "El fichero GPX esta en $(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath)"
                    Log.i("defaultOperation","El fichero GPX esta en $(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath)")
                }else {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(grantResults.first() == PackageManager.PERMISSION_GRANTED){
            writeGPXToFile()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun writeGPXToFile(){

        val temp = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + "/" + "coords.gpx")
        val stream = FileOutputStream(file)
        try {

            if(!file.exists()){
                file.createNewFile()
            }

            stream.write(gpx?.toByteArray())
            findViewById<TextView>(R.id.tvRuta).text = "Escritura fichero GPX completada"
            Log.i("writeGPXToFile","Escritura fichero GPX completada")

        } catch (e: IOException) {
            Log.e("writeGPXToFile","ERROR")
            e.printStackTrace()
        } finally {
            stream.close()
        }
    }

    private fun uploadFileToGdrive (file : File){


    }


}