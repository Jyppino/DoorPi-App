package com.jedeboer.doorpi

import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.view.Display
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlin.math.min


class InviteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invite)

        val registrationCode = intent.getStringExtra("code")

        try {
            val barcodeEncoder = BarcodeEncoder()
            val size = calcQrSize()
            val bitmap =
                barcodeEncoder.encodeBitmap(registrationCode, BarcodeFormat.QR_CODE, size, size)
            val imageViewQrCode: ImageView = findViewById(R.id.imageview_barcode)
            imageViewQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
        }

        val textview_code: TextView = findViewById(R.id.textview_code)
        textview_code.text = registrationCode
    }

    override fun onRestart() {
        super.onRestart()
        val intent = Intent(applicationContext, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun calcQrSize(): Int {
        val display: Display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x - 200
        val height = size.y - 200
        return min(min(width, height), 2000)
    }
}
