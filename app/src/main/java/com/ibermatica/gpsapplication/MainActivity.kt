package com.ibermatica.gpsapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.github.rybalkinsd.kohttp.dsl.httpPost
import io.github.rybalkinsd.kohttp.ext.url
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
    val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + "/rutas"
    val file = File(path + "/" + "coords.gpx")

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

            findViewById<TextView>(R.id.tvRuta).text = "2 Convirtiendo enlace gmaps a GPX"
            Log.i("defaultOperation","2 Convirtiendo enlace gmaps a GPX")

            val request: Request = Request.Builder()
                .url("https://mapstogpx.com/load.php?d=default&lang=en&elev=off&tmode=off&pttype=fixed&o=gpx&cmt=off&desc=off&descasname=off&w=off&rn=RutaImportadaDeMaps&dtstr=20220523_170630&gdata=$url")
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
                    findViewById<TextView>(R.id.tvRuta).text = "El fichero GPX esta en $path"
                    Log.i("defaultOperation","El fichero GPX esta en $path")
                }else {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
                }

                //Subimos a drive
                findViewById<TextView>(R.id.tvRuta).text = "Subiendo a gdrive"
                uploadFileToGdrive()

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

    private fun writeGPXToFile() {

        Log.i("writeGPXToFile","El fichero GPX estara en $path")
        val stream = FileOutputStream(file)
        try {

            if(!file.exists()){
                file.delete()
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

    private fun uploadFileToGdrive (){

        Log.i("uploadFileToGdrive","Subiendo a gdrive")

        val policy = ThreadPolicy.Builder()
            .permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val token = "4/0AX4XfWiJSfgusjelZ85XQbR7xHSJXBKeBSGQiPCV6_ybdgVseXR27iA3TylPauBnBtNXVQ"
//        val file = ""

        val response = httpPost {
            url("https://www.googleapis.com/upload/drive/v3/files?uploadType=media")
            header {
                "Authorization" to "Bearer $token"
            }

            body {
                file(file)
            }
        }

        Log.i("uploadFileToGdrive","response = [${response.toString()}]")

        Log.i("uploadFileToGdrive","Fichero subido a gdrive")
    }







}