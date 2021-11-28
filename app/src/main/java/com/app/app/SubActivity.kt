package com.app.app

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*

class SubActivity : AppCompatActivity() {

    //알림
    private val channelID = "NC106"
    private var notificationManager: NotificationManager? = null
    //알림

    //블루투스
    private var bluetoothAdapter: BluetoothAdapter? = null // 블루투스 어댑터
    private var devices: Set<BluetoothDevice>? = null // 블루투스 디바이스 데이터 셋
    private var bluetoothDevice: BluetoothDevice? = null // 블루투스 디바이
    private var bluetoothSocket: BluetoothSocket? = null // 블루투스 소켓
    private var outputStream: OutputStream? = null // 블루투스에 데이터를 출력하기 위한 출력 스트림
    private var inputStream: InputStream? = null // 블루투스에 데이터를 입력하기 위한 입력 스트림
    private var workerThread: Thread? = null // 문자열 수신에 사용되는 쓰레드
    private lateinit var readBuffer: ByteArray // 수신 된 문자열을 저장하기 위한 버퍼
    private var readBufferPosition= 0 // 버퍼 내 문자 저장 위치
    private var pairedDeviceCount = 0
    //블루투스

    @SuppressLint("UseSwitchCompatOrMaterialCode", "UnspecifiedImmutableFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this,"이 기기는 블루투스를 지원하지 않습니다.", Toast.LENGTH_SHORT).show()
        }
        else {
            if (bluetoothAdapter!!.isEnabled) {
                selectBluetoothDevice()
            } else {
                startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT)
            }
        }

        val sw2: Switch = findViewById(R.id.ser)
        val sw: Switch = findViewById(R.id.ala)

        sw.setOnCheckedChangeListener {_, _ ->
            if (sw.isChecked) {
                Toast.makeText(this,"알림을 켰습니다.", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this,"알림을 껐습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        sw2.setOnCheckedChangeListener {_, _ ->
            if (sw2.isChecked) {
                Toast.makeText(this,"서비스를 켰습니다.", Toast.LENGTH_SHORT).show()
                serviceStart()
            }
            else {
                Toast.makeText(this,"서비스를 껐습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(baseContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(channelID,"TrafficLight","Notification about TrafficLight")

        val notification1 = NotificationCompat.Builder(this@SubActivity, channelID)
            .setContentTitle("신호등 안내")
            .setContentText("적색신호입니다. 보행을 멈춰주세요.")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.light_large))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        fun displayNotification1() {
            if (sw.isChecked) {
                notificationManager?.notify(45, notification1)
            }
        }

        val notification2 = NotificationCompat.Builder(this@SubActivity, channelID)
            .setContentTitle("신호등 안내")
            .setContentText("녹색신호입니다. 횡단보도를 건너가세요.")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.light_large))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        fun displayNotification2() {
            if (sw.isChecked) {
                notificationManager?.notify(45, notification2)
            }
        }

        val textViewReceive: TextView = findViewById(R.id.receive)
        textViewReceive.addTextChangedListener {
            when(textViewReceive.text.toString()){
                "1" -> {displayNotification1()}
                "0" -> {displayNotification2()}
                else -> {}
            }
        }
    }

    private fun createNotificationChannel(id : String, name:String, channelDescription:String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = channelDescription
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ENABLE_BT -> if (requestCode == RESULT_OK) {
                selectBluetoothDevice()
            }
        }
    }

    private fun selectBluetoothDevice() {
        devices = bluetoothAdapter!!.bondedDevices
        pairedDeviceCount = (devices as MutableSet<BluetoothDevice>?)!!.size
        if (pairedDeviceCount == 0) {
            Toast.makeText(this,"페어링된 장치가 없습니다.", Toast.LENGTH_SHORT).show()
        }
        else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("페어링 된 블루투스 디바이스 목록")
            val list: MutableList<String> = ArrayList()
            for (bluetoothDevice in (devices as MutableSet<BluetoothDevice>?)!!) {
                list.add(bluetoothDevice.name)
            }
            list.add("취소")


            val charSequences = list.toTypedArray<CharSequence>()
            list.toTypedArray<CharSequence>()
            builder.setItems(charSequences) { _, which ->
                connectDevice(charSequences[which].toString())
            }
            builder.setCancelable(false)
            builder.create().show()
        }
    }

    private fun connectDevice(deviceName: String) {
        for (tempDevice in devices!!) {
            if (deviceName == tempDevice.name) {
                bluetoothDevice = tempDevice
                break
            }
        }
        try {
            bluetoothSocket = bluetoothDevice?.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"))
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            inputStream = bluetoothSocket?.inputStream
            if(deviceName == "취소"){
                Toast.makeText(this, "연결을 취소했습니다", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "$deviceName 와 연결되었습니다", Toast.LENGTH_SHORT).show()
            }
            receiveData()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode", "UnspecifiedImmutableFlag")
    private fun receiveData() {
        val handler = Handler()
        val textViewReceive: TextView = findViewById(R.id.receive)
        readBufferPosition = 0
        readBuffer = ByteArray(1024)
        workerThread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val byteAvailable = inputStream!!.available()
                    if (byteAvailable > 0) {
                        val bytes = ByteArray(byteAvailable)
                        inputStream!!.read(bytes)
                        for (i in 0 until byteAvailable) {
                            val tempByte = bytes[i]
                            if (tempByte == '\n'.code.toByte()) {

                                val encodedBytes = ByteArray(readBufferPosition)
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.size)
                                val charset: Charset = Charsets.US_ASCII
                                val text = String(encodedBytes, charset)
                                readBufferPosition = 0
                                handler.post {
                                    textViewReceive.text = text.trimIndent()
                                }
                            }
                            else {
                                readBuffer[readBufferPosition++] = tempByte
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        workerThread!!.start()
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 10
    }

    private fun serviceStart() {
        val intent = Intent(this,Foreground::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
    private fun serviceStop() {
        val intent = Intent(this,Foreground::class.java)
        stopService(intent)
    }
}