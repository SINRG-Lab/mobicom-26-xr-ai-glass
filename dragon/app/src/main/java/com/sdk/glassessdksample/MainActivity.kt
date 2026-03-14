package com.sdk.glassessdksample

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
// import com.google.android.gms.auth.api.signin.GoogleSignIn
// import com.google.android.gms.auth.api.signin.GoogleSignInAccount
// import com.google.android.gms.fitness.FitnessOptions
// import com.google.android.gms.fitness.data.DataType
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.communication.LargeDataHandler
import com.oudmon.ble.base.communication.bigData.resp.GlassesDeviceNotifyListener
import com.oudmon.ble.base.communication.bigData.resp.GlassesDeviceNotifyRsp
import com.oudmon.ble.base.scan.BleScannerHelper
import com.oudmon.ble.base.scan.ScanRecord
import com.oudmon.ble.base.scan.ScanWrapperCallback
import com.oudmon.wifi.GlassesControl
import com.oudmon.wifi.bean.GlassAlbumEntity
import com.sdk.glassessdksample.databinding.ActivityMainBinding
import com.sdk.glassessdksample.ui.BluetoothUtils
import com.sdk.glassessdksample.ui.ConnectActivity
import com.sdk.glassessdksample.ui.DeviceBindActivity
import com.sdk.glassessdksample.ui.DeviceListAdapter
import com.sdk.glassessdksample.ui.DeviceManager
import com.sdk.glassessdksample.ui.GalleryActivity
import com.sdk.glassessdksample.ui.MyApplication
import com.sdk.glassessdksample.ui.SettingsActivity
import com.sdk.glassessdksample.ui.SmartWatch
import com.sdk.glassessdksample.ui.hasBluetooth
import com.sdk.glassessdksample.ui.requestAllPermission
import com.sdk.glassessdksample.ui.requestBluetoothPermission
import com.sdk.glassessdksample.ui.setOnClickListener
import com.sdk.glassessdksample.ui.startKtxActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.String

//import org.concentus.OpusDecoder

class MainActivity : AppCompatActivity() {
    private val TAG="HeyCyanSDK"
    private lateinit var binding: ActivityMainBinding

    private val requestedPermissions = buildList {
        add(Manifest.permission.INTERNET)
        add(Manifest.permission.ACCESS_WIFI_STATE)
        add(Manifest.permission.CHANGE_WIFI_STATE)
        add(Manifest.permission.ACCESS_NETWORK_STATE)
        add(Manifest.permission.CHANGE_NETWORK_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }.toTypedArray()

    private var opusToPcmIng = false

    var activityCount = -1

    lateinit var audioPath: InputStream
    lateinit var imagePath: InputStream
    lateinit var path: String
    private val requestPermissionLaunch = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { it ->
        if (it.all { it.value }) {
            GlassesControl.getInstance(MyApplication.getInstance())?.initGlasses(MyApplication.getInstance().getAlbumDirFile().absolutePath)
            GlassesControl.getInstance(MyApplication.getInstance())?.setWifiDownloadListener(object :GlassesControl.WifiFilesDownloadListener{
                override fun eisEnd(fileName: String, filePath: String) {
                    Log.i(TAG,"eisEnd fileName: $fileName filePath: $filePath")
                }

                override fun eisError(fileName: String, sourcePath: String, errorInfo: String) {
                    Log.i(TAG,"eisEnd fileName: $fileName filePath: $sourcePath errorInfo: $errorInfo")
                }

                override fun fileCount(index: Int, total: Int) {
                    Log.i(TAG,"fileCount index: $index total: $total")
                }

                override fun fileDownloadError(fileType: Int, errorType: Int) {
                    Log.i(TAG,"fileDownloadError fileType: $fileType errorType: $errorType")
                }

                override fun fileProgress(fileName: String, progress: Int) {
                    Log.i(TAG,"fileProgress fileName: $fileName progress: $progress")
                }

                override fun fileWasDownloadSuccessfully(entity: GlassAlbumEntity) {
                    Log.i(
                        TAG,
                        "fileWasDownloadSuccessfully entity: $entity, file path: ${entity.filePath}"
                    )
                    path = entity.filePath ?: return

                    val rawName = File(path).nameWithoutExtension

                    val inputFormat = SimpleDateFormat("yyyyMMddHmmssSSS", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val currentTime = timeFormat.format(Date())

                    val displayDate = try {
                        val date = inputFormat.parse(rawName)
                        outputFormat.format(date!!)
                    } catch (e: Exception) {
                        rawName
                    }

                    if (path.contains(".jpg")) {
                        try {
                            aiClient.streamChatResponse(
                                prompt = "Give a 5-10 word description of this image as if it was a caption",
                                image = File(path).readBytes(),
                                true
                            ) { fullText ->
                                Log.d(TAG, "description")
                                val newItem = GalleryActivityItem(
                                    title = "$displayDate $currentTime",
                                    description = fullText,
                                    media = path
                                )
                                GalleryStore.addItem(newItem)
                            }
                        }
                        catch (e: Exception) {
                            Log.d(TAG, "Could not import AI caption due to error: $e")
                            val newItem = GalleryActivityItem(
                                title = "$displayDate $currentTime",
                                description = "",
                                media = path
                            )
                            GalleryStore.addItem(newItem)
                        }
                    }

                    else if (path.contains("mp4")) {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(path)

                        val bitmap = retriever.getFrameAtTime(0)
                        retriever.release()

                        if (bitmap != null) {
                            val stream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                            val frameBytes = stream.toByteArray()

                            try {
                                aiClient.streamChatResponse(
                                    prompt = "Give a 5-10 word caption for the first frame of this video",
                                    image = frameBytes,
                                    caption = true
                                ) { fullText ->
                                    Log.d(TAG, "Video first frame caption: $fullText")
                                    val newItem = GalleryActivityItem(
                                        title = "$displayDate $currentTime",
                                        description = fullText,
                                        media = path
                                    )
                                    GalleryStore.addItem(newItem)
                                }
                            }
                            catch (e: Exception) {
                                Log.d(TAG, "Could not import AI caption due to error: $e")
                                val newItem = GalleryActivityItem(
                                    title = "$displayDate $currentTime",
                                    description = "",
                                    media = path
                                )
                                GalleryStore.addItem(newItem)
                            }
                        }
                    }

                    else if (path.contains("opus")) {

                    }
                }

                override fun fileDownloadComplete() {
                    Log.i(TAG,"fileDownloadComplete")

                    if (path.contains(".jpg")) {
                        // imageStreamer.start()
                    }
                    else if (path.contains(".opus")) {
                        //audioStreamer.start()
                    }

                    activityCount += 1
                    adapterRecent.updateActivity(activityCount)
                }

                override fun onGlassesControlSuccess() {
                    Log.i(TAG,"onGlassesControlSuccess")
                }

                override fun onGlassesFail(errorCode: Int) {
                    Log.i(TAG,"onGlassesFail errorCode: $errorCode")
                }

                override fun recordingToPcm(fileName: String, filePath: String, duration: Int) {
                    Log.i(TAG,"recordingToPcm fileName: $fileName filePath: $filePath duration: $duration")
                    audioPath = File(filePath).inputStream()
                    audioStreamer.start()

                    val pcmBytes = File(filePath).readBytes()
                    Log.d(TAG, "PCM raw length = ${pcmBytes.size}")
                    Log.d(TAG, "First samples: ${pcmBytes.take(20).joinToString()}")
                }

                override fun recordingToPcmError(fileName: String, errorInfo: String) {
                    Log.i(TAG,"recordingToPcmError fileName: $fileName errorInfo: $errorInfo")
                }

                override fun wifiSpeed(wifiSpeed: String) {
                    Log.i(TAG,"wifiSpeed wifiSpeed: $wifiSpeed")
                }

            })
            GlassesControl.getInstance(MyApplication.getInstance)?.importAlbum()
        } else {
            Log.i("sdk","拒绝了权限")
        }
    }

    private lateinit var glassesControl: GlassesControl
    private lateinit var ttsPlayer: TtsPlayer
    private lateinit var deepgramClient: DeepgramClient
    private lateinit var whisperWebSocket: WhisperWebSocket
    private lateinit var ttsWebSocket: TTSWebSocket
    private lateinit var aiClient: AIClient
    private lateinit var uberAPI: UberAPI
    private lateinit var micStreamer: MicStreamer
    private lateinit var audioStreamer: FileAudioStreamer
    private val deviceNotifyListener by lazy { MyDeviceNotifyListener() }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Location permission is required for location-based features", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestLocationPermissionsIfNeeded() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted && !coarseLocationGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private var isAudioActive = false
    private var isImageActive = false
    private var isVideoActive = false
    private var isThumbnailActive = false

    private val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1
    private val ACTIVITY_RECOGNITION_REQUEST_CODE = 2

    private lateinit var  adapter: DeviceListAdapter
    private var scanSize:Int=0
    val deviceList = mutableListOf<SmartWatch>()
    val bleScanCallback: BleCallback = BleCallback()
    private val runnable=MyRunnable()
    private val myHandler : Handler = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }
    protected var activity: Activity? = null

    private lateinit var adapterRecent: RecentActivityAdapter

    var deviceName = DeviceManager.getDeviceName()?.deviceName // Should change based on which device is connected to in scanning devices

    var thinkmodel = LLMManager.getModelName()?.ModelName

    private lateinit var inputRecording : InputStream

    private var i = 1

    private val latencyTimings = mutableListOf<LatencyTiming>()

    private var firstTime = true
    private var pastRun: Int = 0

    private var whisper = false

    private var imagePrompt = "Give a description of this image, providing a detailed yet concise explanation in 100 words. No need to make a tool call here, as you already have the image data. Your response will be read out loud, so give the output as if it was speech."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()

        requestPermissions()

        val prefs_device = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedDeviceName = prefs_device.getString("last_device_name", null)
        if (savedDeviceName != null) {
            DeviceManager.getDeviceName()?.deviceName = savedDeviceName
            deviceName = savedDeviceName
            Log.d("MainActivity", "Loaded saved device name: $deviceName")
        } else {
            deviceName = DeviceManager.getDeviceName()?.deviceName
            Log.d("MainActivity", "No saved device name found, using default: $deviceName")
        }

        binding.glassesName.text = deviceName

        // thinkmodel = LLMManager.getModelName()?.ModelName
        // deviceName = DeviceManager.getDeviceName()?.deviceName

        val prefs_model = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedModel = prefs_model.getString("selected_model", "gpt-4o-mini") // default fallback
        LLMManager.getModelName()?.ModelName = savedModel

        thinkmodel = LLMManager.getModelName()?.ModelName
        Log.d("MainActivity", "Loaded model: $thinkmodel")

        glassesControl = GlassesControl(MyApplication.getInstance())

        val recyclerRecent = findViewById<RecyclerView>(R.id.recent_activity_recycler)
        adapterRecent = RecentActivityAdapter(mutableListOf())
        recyclerRecent.layoutManager = LinearLayoutManager(this)
        recyclerRecent.adapter = adapterRecent

        adapter = DeviceListAdapter(this, deviceList)

        LargeDataHandler.getInstance().addOutDeviceListener(100, deviceNotifyListener)

        requestPermissionsAndStart()

        LargeDataHandler.getInstance().addBatteryCallBack("init") { _, response ->
            response?.let {
                val batteryLevel = it.battery
                val isCharging = it.isCharging
                updateBatteryStatus(batteryLevel, isCharging)
                binding.glassesName.text = deviceName
            }
        }

        ttsPlayer = TtsPlayer(scope = lifecycleScope)
        ttsPlayer.start()

        aiClient = AIClient(
            openAiKey = BuildConfig.OPENAI_API_KEY,
            accessKey = BuildConfig.AWS_ACCESS_KEY,
            secretAccessKey = BuildConfig.AWS_SECRET_KEY,
            scope = lifecycleScope,
            context = this
        )

        uberAPI = UberAPI(
            context = this,
            clientId = BuildConfig.UBER_CLIENT_ID,
            clientSecret = BuildConfig.UBER_CLIENT_SECRET,
            redirectUri = "myapp://uber-callback"
        )

        deepgramClient = DeepgramClient(
            apiKey = BuildConfig.DEEPGRAM_API_KEY,
            scope = lifecycleScope,
            listenModel = "nova-3",
            thinkModel = thinkmodel,
            speakModel = "aura-2-andromeda-en",
            context = this,
            onPartialTranscription = { text ->
            },
            onFinalTranscription = { finalText ->
            },
            onImage = { prompt ->
                // Log.d("Ossian", "Image detection")
                imagePrompt = "$prompt Provide a detailed yet concise explanation in 100 words. Note that this prompt is coming from a pair of smart glasses so if there is an image treat it like its directly in front of the user rather than just something being passed. No need to make a tool call here, as you already have the image data. Your response will be read out loud, so give the output as if it was speech."
                Log.d("Ossian", "New prompt: $imagePrompt")

                Log.d("Ossian", "Image transmission started at ${LatencyManager.now()}")
                LatencyManager.getCurrentRun()?.startInputTransmissionTime = LatencyManager.now()

                LargeDataHandler.getInstance().addOutDeviceListener(100, deviceNotifyListener)
                val thumbnailSize = 0x02
                sendControlCommand(
                    byteArrayOf(
                        0x02,
                        0x01,
                        0x06,
                        thumbnailSize.toByte(),
                        thumbnailSize.toByte(),
                        0x02
                    )
                )
            },
        )

        deepgramClient.setUberAPI(uberAPI)

        ttsWebSocket = TTSWebSocket(
            apiKey = "",
            scope = lifecycleScope,
            client = OkHttpClient()
        )
        ttsWebSocket.connect()

        micStreamer = MicStreamer(
            context = this,
            scope = lifecycleScope,
            onChunkReady = { chunk ->
                // Log.d("Glasses", "chunk: $chunk")
                // whisperWebSocket.sendAudioChunk(chunk)
            },
            onStreamingComplete = { outputFile ->
                // LatencyManager.getCurrentRun()?.finishInputTransmissionTime = LatencyManager.now()
                inputRecording = outputFile.inputStream()
                LatencyManager.getCurrentRun()?.startTime = LatencyManager.now()
                // deepgramClient.connect()
                audioStreamer.start()

                // val file = File(getExternalFilesDir(null), "latency_timings001.csv")
                // LatencyManager.exportToCsvAudio(file)
            }
        )

        audioStreamer = FileAudioStreamer(
            scope = lifecycleScope,
            inputStreamProvider = {
                // Log.d("Ossian", "recording (from glasses): ${inputRecording}")
                // Log.d("Ossian", "recording (from file): ${R.raw.recording1}")

                // resources.openRawResource(R.raw.recording2)
                inputRecording
            },
            onChunkReady = { chunk ->
                // aiClient.sendAudioChunk(chunk) (Uncomment for realtime)
                if (whisper == true) {
                    whisperWebSocket.sendAudioChunk(chunk)
                }
                else {
                    // Log.d("Ossian", "sent chunk: $chunk")
                    deepgramClient.sendAudioChunk(chunk)
                }
            },
            onStreamingComplete = {
                Log.d("Ossian", "Audio streaming complete")
                audioStreamer.stop()
                // aiClient.commitAudio()
                // isAudioActive = false
            }
        )

        /*
        imageStreamer = FileImageStreamer(
            scope = lifecycleScope,
            inputStreamProvider = {
                imagePath
            },
            onImageReady = { bytes ->
                lifecycleScope.launch {
                    /* Uncomment for labeling
                    val labels = aiClient.detectLabelsInImage(bytes)
                    Log.d("TalkActivity", "Labels: ${labels.joinToString()}")

                    val audioResponse = aiClient.synthesizeSpeech(labels)
                    audioResponse?.let { ttsPlayer.offer(it) }
                    */

                    /* Uncomment for realtime
                    val gptResponse = aiClient.sendImageForAnalysis(bytes)
                    Log.d("TalkActivity", "GPT: $gptResponse")
                    */

                    /*
                    val gptResponse = aiClient.chatWithTools("Give a description of this image, provide a detailed explanation", bytes)
                    Log.d("MainActivity", "GPT: $gptResponse")

                    val audioResponse = aiClient.synthesizeSpeech(gptResponse)
                    Log.d("MainActivity", "Audio Response: $audioResponse")
                    audioResponse?.let { ttsPlayer.offer(it) }
                     */

                    isImageActive = false
                    // imageStreamer.stop()
                    // audioStreamer.stop()
                    // whisperWebSocket.close()
                }
            }
        )
        */

        val fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ)
            .build()

        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                account,
                fitnessOptions
            )
        } else {
            initGoogleFit(account)
        }

        requestLocationPermissionsIfNeeded()

        if (!XXPermissions.isGranted(this,
                (Permission.BLUETOOTH_SCAN),
                (Permission.BLUETOOTH_CONNECT),
                (Permission.BLUETOOTH_ADVERTISE))
            ) {
                requestBluetoothPermission(this, PermissionCallback())
        }

        // Request storage if app truly needs it
        if (!XXPermissions.isGranted(this, Permission.MANAGE_EXTERNAL_STORAGE)) {
            requestAllPermission(this, object : OnPermissionCallback {
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    // Storage granted
                }
                override fun onDenied(permissions: List<String>, never: Boolean) {
                    // Handle denial
                }
            })
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "TTS_CHANNEL",
                "TTS Foreground Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    inner class PermissionCallback : OnPermissionCallback {
        override fun onGranted(permissions: MutableList<String>, all: Boolean) {
            if (!all) {

            } else{
                startKtxActivity<DeviceBindActivity>()
            }
        }

        override fun onDenied(permissions: MutableList<String>, never: Boolean) {
            super.onDenied(permissions, never)
            if(never){
                XXPermissions.startPermissionActivity(this@MainActivity, permissions);
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // LargeDataHandler.getInstance().addOutDeviceListener(100, deviceNotifyListener)

        myHandler.postDelayed(object : Runnable {
            override fun run() {
                // monitorConnection()
                LargeDataHandler.getInstance().addBatteryCallBack("init") { _, response ->
                    response?.let {
                        val batteryLevel = it.battery
                        val isCharging = it.isCharging
                        updateBatteryStatus(batteryLevel, isCharging)
                    }
                }
                LargeDataHandler.getInstance().syncBattery()
                binding.glassesName.text = deviceName
            }
        }, 10_000) // Maybe have this done based on connection/battery changes themselves

        try {
            if (!BluetoothUtils.isEnabledBluetooth(this)) {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestBluetoothPermission(this, BluetoothPermissionCallback())
                        // return
                    }
                }
                startActivityForResult(intent, 300)
            }
        } catch (e: Exception) {
        }
        if (!hasBluetooth(this)) {
            requestBluetoothPermission(this, BluetoothPermissionCallback())
        }

        requestAllPermission(this, OnPermissionCallback { permissions, all ->  })
    }

    inner class BluetoothPermissionCallback : OnPermissionCallback {
        override fun onGranted(permissions: MutableList<String>, all: Boolean) {
            if (!all) {

            }
        }

        override fun onDenied(permissions: MutableList<String>, never: Boolean) {
            super.onDenied(permissions, never)
            if (never) {
                XXPermissions.startPermissionActivity(this@MainActivity, permissions)
            }
        }

    }

    var isVideoInactive : Boolean = false
    var isAudioInactive : Boolean = false

    val mainContext: Context = this

    @SuppressLint("MissingPermission")
    private fun initView() {
        setOnClickListener(
            binding.glassesCard,
            binding.btnCamera,
            binding.btnVideo,
            binding.btnVoice,
            binding.btnThumbnail,
            binding.btnDownload,
            binding.btnGallery,
            binding.btnSettings
        ) {
            when (this) {
                /*
                binding.btnScan -> {
                    // smartGlassesIntegration.scanForGlasses()
                    ?"
                    startKtxActivity<DeviceBindActivity>()
                }

                binding.btnConnect -> {
                    smartGlassesIntegration.connectToGlasses(DeviceManager.getInstance().deviceAddress)
                }

                binding.btnDisconnect -> {
                    smartGlassesIntegration.disconnectFromGlasses()
                }
                */
                binding.glassesCard -> {
                    val intent = Intent(this@MainActivity, ConnectActivity::class.java)
                    startActivity(intent)
                }

                binding.btnCamera -> {
                    endAllProcesses()
                    if (!isImageActive) {
                        isImageActive = true
                        sendControlCommand(byteArrayOf(0x02, 0x01, 0x01))
                        addRecentActivity()
                    }
                }

                binding.btnVideo -> {
                    if (!isVideoActive) {
                        endAllProcesses()
                        isVideoActive = true
                        isVideoInactive = false
                        sendControlCommand(byteArrayOf(0x02, 0x01, 0x02))
                    } else {
                        isVideoActive = false
                        isVideoInactive = true
                        addRecentActivity()
                        sendControlCommand(byteArrayOf(0x02, 0x01, 0x03))
                    }
                }

                binding.btnVoice ->  {
                    /*
                    lifecycleScope.launch {
                        for (i in 1..1) {
                            LargeDataHandler.getInstance().addOutDeviceListener(100, deviceNotifyListener)
                            Log.d("Ossian", "Latency timing past run, start time: ${LatencyManager.getCurrentRun()?.startTime}, finish time: ${LatencyManager.getCurrentRun()?.finishTime}")

                            Log.d("Ossian", "Test $i started at ${LatencyManager.now()}")
                            val run = LatencyManager.newRun()
                            run.startTime = LatencyManager.now()

                            //Log.d("Ossian", "Network transmission started at ${LatencyManager.now()}")
                            //LatencyManager.getCurrentRun()?.startUplinkTime = LatencyManager.now()

                            //whisperWebSocket.connect()

                            if (i == 1) {
                                deepgramClient.connect()
                                while (!deepgramClient.isConnected()) { delay(10) }
                            }
                            //Log.d("Ossian", "Network transmission ended at ${LatencyManager.now()}")
                            //LatencyManager.getCurrentRun()?.finishUplinkTime = LatencyManager.now()
                            // Would be right when mic streamer stops
                            //Log.d("Ossian", "Input transmission started at ${LatencyManager.now()}")
                            //LatencyManager.getCurrentRun()?.startInputTransmissionTime = LatencyManager.now()
                            audioStreamer.start()

                            val file = File(getExternalFilesDir(null), "latency_timings001.csv")
                            LatencyManager.exportToCsvAI(file)

                            /*
                            val file1 = File(getExternalFilesDir(null), "bitrates.csv")
                            BitrateManager.exportToCsv(file1)
                            */

                            if (i == 1) {
                                delay(60000)
                            }
                            else {
                                delay(60000)
                            }
                        }
                    }
                    */

                    lifecycleScope.launch {
                        if (!isAudioActive) {
                            Log.d("Ossian", "Latency timing past run, start time: ${LatencyManager.getCurrentRun()?.startTime}, finish time: ${LatencyManager.getCurrentRun()?.finishTime}")

                            Log.d("Ossian", "Test $i started at ${LatencyManager.now()}")
                            val run = LatencyManager.newRun()

                            val prefs_model = getSharedPreferences("app_prefs", MODE_PRIVATE)
                            val savedModel = prefs_model.getString("selected_model", "gpt-4o-mini") // default fallback
                            Log.d("Ossian", "past llm model: $thinkmodel, current llm model: $savedModel")
                            if (thinkmodel != savedModel) {
                                deepgramClient = DeepgramClient(
                                    apiKey = "64aca23e42527ea7ca7153e158195db12259ea7f",
                                    scope = lifecycleScope,
                                    listenModel = "nova-3",
                                    thinkModel = savedModel,
                                    speakModel = "aura-2-andromeda-en",
                                    context = mainContext,
                                    onPartialTranscription = { text ->
                                    },
                                    onFinalTranscription = { finalText ->
                                    },
                                    onImage = { prompt ->
                                        // Log.d("Ossian", "Image detection")
                                        imagePrompt = "$prompt, providing a detailed explanation. Note that this prompt is coming from a pair of smart glasses so if there is an image treat it like its directly in front of the user rather than just something being passed. No need to make a tool call here, as you already have the image data. Your response will be read out loud, so give the output as if it was speech."
                                        Log.d("Ossian", "New prompt: $imagePrompt")

                                        Log.d("Ossian", "Image transmission started at ${LatencyManager.now()}")
                                        LatencyManager.getCurrentRun()?.startInputTransmissionTime = LatencyManager.now()

                                        LargeDataHandler.getInstance().addOutDeviceListener(100, deviceNotifyListener)
                                        val thumbnailSize = 0x02
                                        sendControlCommand(
                                            byteArrayOf(
                                                0x02,
                                                0x01,
                                                0x06,
                                                thumbnailSize.toByte(),
                                                thumbnailSize.toByte(),
                                                0x02
                                            )
                                        )
                                    },
                                )
                            }

                            deepgramClient.connect()
                            while (!deepgramClient.isConnected()) { delay(10) }

                            endAllProcesses()

                            isAudioActive = true
                            isAudioInactive = false

                            // audioStreamer.start()
                            // sendControlCommand(byteArrayOf(0x02, 0x01, 0x08))
                            micStreamer.start()

                            val file = File(getExternalFilesDir(null), "latency_timings001.csv")
                            LatencyManager.exportToCsv(file)
                        }
                        else {
                            isAudioActive = false
                            isAudioInactive = true

                            addRecentActivity()
                            // audioStreamer.stop()
                            // sendControlCommand(byteArrayOf(0x02, 0x01, 0x0c))
                            micStreamer.stop()
                        }
                    }
                }

                binding.btnThumbnail -> {
                    endAllProcesses()
                    LargeDataHandler.getInstance().addOutDeviceListener(100, deviceNotifyListener)
                    if (!isThumbnailActive) {
                        isThumbnailActive = true
                        val thumbnailSize = 0x02
                        sendControlCommand(
                            byteArrayOf(
                                0x02,
                                0x01,
                                0x06,
                                thumbnailSize.toByte(),
                                thumbnailSize.toByte(),
                                0x02
                            )
                        )
                        addRecentActivity()
                    }

                    /*
                    LargeDataHandler.getInstance().addOutDeviceListener(100, deviceNotifyListener)
                    lifecycleScope.launch {
                        Log.d("Ossian", "Device name: ${DeviceManager.getDeviceName()?.deviceName}")
                        endAllProcesses()
                        isThumbnailActive = true
                        for (i in 1..100) {
                            Log.d("Ossian", "Latency timing past run, start time: ${LatencyManager.getCurrentRun()?.startTime}, finish time: ${LatencyManager.getCurrentRun()?.finishTime}")

                            Log.d("Ossian", "Test $i started at ${LatencyManager.now()}")
                            val run = LatencyManager.newRun()
                            run.startTime = LatencyManager.now()

                            Log.d("Ossian", "Image transmission started at ${LatencyManager.now()}")
                            LatencyManager.getCurrentRun()?.startInputTransmissionTime = LatencyManager.now()

                            Log.d("Ossian", "Image transmission started at ${LatencyManager.getCurrentRun()?.startInputTransmissionTime}")

                            startTime = System.currentTimeMillis()
                            Log.d("BitrateTest", "Start time: $startTime")
                            val thumbnailSize = 0x02
                            sendControlCommand(
                                byteArrayOf(
                                    0x02,
                                    0x01,
                                    0x06,
                                    thumbnailSize.toByte(),
                                    thumbnailSize.toByte(),
                                    0x02
                                )
                            )

                            val file = File(getExternalFilesDir(null), "latency_timings.csv")
                            LatencyManager.exportToCsvImage(file)

                            val file1 = File(getExternalFilesDir(null), "bitrates.csv")
                            BitrateManager.exportToCsv(file1)

                            delay(60000)
                        }
                    }
                    */
                }

                binding.btnDownload -> {
                    endAllProcesses()
                    requestPermissionLaunch.launch(requestedPermissions)
                }

                binding.btnGallery -> {
                    val intent = Intent(this@MainActivity, GalleryActivity::class.java)
                    startActivity(intent)
                }

                binding.btnSettings -> {
                    val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    fun sendControlCommand(bytes: ByteArray) {
        LargeDataHandler.getInstance().glassesControl(bytes) { _, resp ->
            // Log.d("Glasses", "Command sent, status=${resp.errorCode}")
            if (resp.errorCode == 1) {
                if (!isAudioInactive) {
                    // addRecentActivity()
                    // Log.d("Glasses", "isVideoActive = $isVideoActive and isVideoInactive = $isVideoActive")
                }
            }
        }
    }

    private var thumbnailBuffer: ByteArrayOutputStream? = null

    var totalBytes = 0L

    var startTime = System.currentTimeMillis()

    inner class MyDeviceNotifyListener : GlassesDeviceNotifyListener() {

        @SuppressLint("MissingPermission")
        @RequiresApi(Build.VERSION_CODES.O)
        override fun parseData(cmdType: Int, response: GlassesDeviceNotifyRsp) {
            // Log.d("Glasses", "Payload length=${response.loadData.size}, bytes=${response.loadData.joinToString { "%02X".format(it) }}, where ${response.loadData[6].toInt()}")
            lifecycleScope.launch {
                when (response.loadData[6].toInt()) {
                    0x02 -> {
                        // Log.d("Glasses", "Picture event received")
                        LargeDataHandler.getInstance().getPictureThumbnails { cmdType, success, data ->
                            lifecycleScope.launch {
                                if (data != null) {
                                    if (thumbnailBuffer == null) {
                                        // Log.d("Glasses", "Image transmission begin")
                                        thumbnailBuffer = ByteArrayOutputStream()
                                    }
                                    thumbnailBuffer!!.write(data) // accumulate every chunk

                                    Log.d("BitrateTest", "Chunk bytes length = ${data.size}")
                                    Log.d(
                                        "BitrateTest",
                                        "First 10 bytes: ${
                                            data.take(10).joinToString { "%02X".format(it) }
                                        }"
                                    )

                                    // val run = BitrateManager.newRun()

                                    val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                                    Log.d("BitrateTest", "Seconds from start: $elapsedSec s")

                                    if (elapsedSec > 0) {
                                        val bitrate = (data.size * 8) / elapsedSec // bits per second
                                        Log.d("BitrateTest", "Current bitrate: $bitrate bps")

                                        // run.inputRate = bitrate.toString()
                                        // Log.d("BitrateTest", "run number: $run")
                                    }

                                    if (success) { // only now we know all chunks have been received
                                        Log.d("Ossian", "Image transmission ended at ${LatencyManager.now()}")
                                        LatencyManager.getCurrentRun()?.finishInputTransmissionTime = LatencyManager.now()

                                        val fullData = thumbnailBuffer!!.toByteArray()
                                        thumbnailBuffer = null // reset for next image

                                        aiClient.streamChatResponse(imagePrompt, fullData, false)
                                        { fullText ->
                                            LargeDataHandler.getInstance().addOutDeviceListener(100, deviceNotifyListener)
                                            addRecentActivity()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    0x01 -> {
                        Log.d("Glasses", "response.loadData[6].toInt() = 0x01")
                        Log.d("Glasses", "response.loadData[7].toInt() = ${response.loadData[7].toInt()}")
                        if (response.loadData[7].toInt() == 0) {
                            Log.d("Glasses", "Recording stopped")
                            // requestPermissionLaunch.launch(requestedPermissions)
                            isAudioActive = false
                            isAudioInactive = true
                            // audioStreamer.stop()
                            // sendControlCommand(byteArrayOf(0x02, 0x01, 0x0c))
                            micStreamer.stop()
                        } else {
                            lifecycleScope.launch {
                                Log.d("Glasses", "Recording started")
                                Log.d("Ossian", "Test $i started at ${LatencyManager.now()}")

                                deepgramClient.connect()
                                while (!deepgramClient.isConnected()) {
                                    delay(10)
                                }

                                endAllProcesses()
                                isAudioActive = true
                                isAudioInactive = false
                                // audioStreamer.start()
                                // sendControlCommand(byteArrayOf(0x02, 0x01, 0x08))
                                micStreamer.start()

                                val file = File(getExternalFilesDir(null), "latency_timings001.csv")
                                LatencyManager.exportToCsv(file)
                                /*
                            if (isAudioActive) {
                                val audioChunk = response.loadData
                                whisperWebSocket.sendAudioChunk(audioChunk)
                            }
                            */
                            }
                        }
                    }

                    //ota 升级
                    0x04 -> {
                        try {
                            val download = response.loadData[7].toInt()
                            val soc = response.loadData[8].toInt()
                            val nor = response.loadData[9].toInt()
                            //download 固件下载进度 soc 下载进度 nor 升级进度
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    0x0c -> {
                        //眼镜触发暂停事件，语音播报
                        if (response.loadData[7].toInt() == 1) {
                            Log.d("Glasses", "0x0c")
                            //to do
                        }
                    }

                    0x0d -> {
                        //解除APP绑定事件
                        if (response.loadData[7].toInt() == 1) {
                            Log.d("Glasses", "0x0d")
                            //to do
                        }
                    }

                    //眼镜内存不足事件
                    0x0e -> {
                        Log.d("Glasses", "0x0e")
                    }
                    //翻译暂停事件
                    0x10 -> {
                        Log.d("Glasses", "0x10")
                    }
                    //眼镜音量变化事件
                    0x12 -> {
                        Log.d("Glasses", "0x12")
                        //音乐音量
                        //最小音量
                        response.loadData[8].toInt()
                        //最大音量
                        response.loadData[9].toInt()
                        //当前音量
                        response.loadData[10].toInt()

                        //来电音量
                        //最小音量
                        response.loadData[12].toInt()
                        //最大音量
                        response.loadData[13].toInt()
                        //当前音量
                        response.loadData[14].toInt()

                        //眼镜系统音量
                        //最小音量
                        response.loadData[16].toInt()
                        //最大音量
                        response.loadData[17].toInt()
                        //当前音量
                        response.loadData[18].toInt()

                        //当前的音量模式
                        response.loadData[19].toInt()

                    }
                }
            }
        }
    }

    /*
    fun playRawOpusFile(opusFilePath: String, sampleRate: Int = 16000, channels: Int = 1, onPcmData: (ByteArray) -> Unit) {
        val decoder = OpusDecoder(sampleRate, channels)
        val inputFile = File(opusFilePath)
        val inputStream = FileInputStream(inputFile)

        val buffer = ByteArray(4096)
        val frameSize = 960
        val pcmShorts = ShortArray(frameSize * channels)

        while (true) {
            val read = inputStream.read(buffer)
            if (read <= 0) break

            try {
                val decodedSamples = decoder.decode(buffer, 0, read, pcmShorts, 0, frameSize, false)
                val pcmBytes = ShortArrayToByteArray(pcmShorts, decodedSamples)
                onPcmData(pcmBytes)
            } catch (e: Exception) {
                Log.e("OpusDecode", "Error decoding: ${e.message}")
            }
        }

        inputStream.close()
        decoder.resetState()
    }

    private fun ShortArrayToByteArray(shorts: ShortArray, length: Int): ByteArray {
        val byteBuffer = ByteBuffer.allocate(length * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until length) {
            byteBuffer.putShort(shorts[i])
        }
        return byteBuffer.array()
    }
    */

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val uri = intent?.data
        if (uri != null && uri.toString().startsWith("myapp://uber-callback")) {
            val code = uri.getQueryParameter("code")
            if (code != null) {
                lifecycleScope.launch {
                    val success = uberAPI.handleRedirectAndStoreToken(code)
                    Toast.makeText(this@MainActivity, if (success) "Uber Authorized" else "Uber Auth Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val fitnessOptions = FitnessOptions.builder()
                    .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                    .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                    .addDataType(DataType.TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ)
                    .build()

                val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
                initGoogleFit(account)
            } else {
                Log.e("GoogleFit", "Permissions denied")
            }
        }
    }

    private fun initGoogleFit(account: GoogleSignInAccount) {
        // deepgramClient.context = this
        deepgramClient.googleSignInAccount = account
        deepgramClient.initHealthAPI()
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    ACTIVITY_RECOGNITION_REQUEST_CODE
                )
                return
            }
        }
    }

    inner class MyRunnable:Runnable{
        override fun run() {
            BleScannerHelper.getInstance().stopScan(this@MainActivity)
        }

    }

    inner class BleCallback : ScanWrapperCallback {
        override fun onStart() {
        }

        override fun onStop() {

        }

        override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
            @Suppress("MissingPermission")
            if (device != null && (!device.name.isNullOrEmpty())) {
//                if (device.name.startsWith("O_")||device.name.startsWith("Q_")) {
//
//                }

                val smartWatch = SmartWatch(device.name, device.address, rssi)
                Log.i("1111", device.name + "---" + device.address)

                if (!deviceList.contains(smartWatch)) {
                    scanSize++
                    deviceList.add(0, smartWatch)
                    deviceList.sortByDescending { it -> it.rssi }
                    adapter.notifyDataSetChanged()
                    if (scanSize > 30) {
                        BleScannerHelper.getInstance().stopScan(this@MainActivity)
                    }
                }

                if (device.name == deviceName) {
                    myHandler.removeCallbacks(runnable) // Stop scan timeout
                    BleScannerHelper.getInstance()
                        .stopScan(this@MainActivity) // Stop scanning
                    connectToDevice(smartWatch)
                    binding.glassesName.text = deviceName
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {

        }

        override fun onParsedData(device: BluetoothDevice?, scanRecord: ScanRecord?) {

        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {

        }
    }

    private fun requestPermissionsAndStart() {
        val perms = listOf(
            com.hjq.permissions.Permission.BLUETOOTH_SCAN,
            com.hjq.permissions.Permission.BLUETOOTH_CONNECT,
            com.hjq.permissions.Permission.ACCESS_FINE_LOCATION
        )
        XXPermissions.with(this)
            .permission(perms)
            .request { permissions, all ->
                if (all && BluetoothUtils.isEnabledBluetooth(this)) {
                    startScan()
                } else {
                    Log.w("BLE", "Missing permissions or Bluetooth disabled")
                }
            }
    }

    private fun startScan() {
        deviceList.clear()
        adapter.notifyDataSetChanged()
        BleScannerHelper.getInstance().reSetCallback()
        if(!BluetoothUtils.isEnabledBluetooth(this@MainActivity)){
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            @Suppress("MissingPermission")
            activity!!.startActivityForResult(intent, 300)
        } else {
            scanSize = 0
            BleScannerHelper.getInstance()
                .scanDevice(this@MainActivity, null, bleScanCallback)
            myHandler.removeCallbacks(runnable)
            myHandler.postDelayed(runnable, 15 * 1000)
        }
    }

    private fun connectToDevice(smartWatch: SmartWatch) {
        BleOperateManager.getInstance().connectDirectly(smartWatch.deviceAddress)
        updateConnectionStatus(true)
    }

    private fun monitorConnection() {
        if (BleOperateManager.getInstance().isConnected() == true) {
            updateConnectionStatus(true)
        }
        else {
            updateConnectionStatus(false)
        }
    }

    private fun updateConnectionStatus(isConnected: Boolean) {
        if (isConnected) {
            binding.connectionStatus.text = "Smart Glasses Connected"
            binding.connectionDot.setBackgroundResource(R.drawable.circle_connected)
        } else {
            binding.connectionStatus.text = "Smart Glasses Disconnected"
            binding.connectionDot.setBackgroundResource(R.drawable.circle_disconnected)
        }
    }

    private fun updateBatteryStatus(batteryLevel: Int, isCharging: Boolean) {
        binding.batteryStatus.text = "$batteryLevel%"
        if (isCharging) {
            if (batteryLevel < 100) {
                binding.batteryIcon.setBackgroundResource(R.drawable.outline_battery_android_bolt_24)
            }
            else {
                binding.batteryIcon.setBackgroundResource(R.drawable.outline_battery_android_frame_bolt_24)
            }
        }
        else {
            when (batteryLevel) {
                in 0 ..0-> binding.batteryIcon.setBackgroundResource(R.drawable.outline_battery_android_0_24)
                in 1..16 -> binding.batteryIcon.setBackgroundResource(R.drawable.outline_battery_android_frame_1_24)
                in 17..33 -> binding.batteryIcon.setBackgroundResource(R.drawable.outline_battery_android_frame_2_24)
                in 34..50 -> binding.batteryIcon.setBackgroundResource(R.drawable.outline_battery_android_frame_3_24)
                in 51..66 -> binding.batteryIcon.setBackgroundResource(R.drawable.outline_battery_android_frame_4_24)
                in 66..82 -> binding.batteryIcon.setBackgroundResource(R.drawable.outline_battery_android_frame_5_24)
                in 83..99 -> binding.batteryIcon.setBackgroundResource(R.drawable.outline_battery_android_frame_6_24)
                in 100..100 -> binding.batteryIcon.setBackgroundResource(R.drawable.outline_battery_android_frame_full_24)
            }
        }
    }

    fun addRecentActivity() {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        var newItem = RecentActivityItem(
            title = "Took a Photo",
            subtitle = currentTime,
            iconRes = android.R.drawable.ic_menu_camera,
            badgeText = "Unsynced"
        )

        if (isVideoInactive) {
            newItem = RecentActivityItem(
                title = "Took a Video",
                subtitle = currentTime,
                iconRes = android.R.drawable.ic_media_play,
                badgeText = "Unsynced"
            )
        }
        else if (isAudioInactive) {
            newItem = RecentActivityItem(
                title = "Transmitted Audio",
                subtitle = currentTime,
                iconRes = android.R.drawable.ic_btn_speak_now,
                badgeText = "AI"
            )
        }
        else if (isThumbnailActive) {
            newItem = RecentActivityItem(
                title = "Image Recognition",
                subtitle = currentTime,
                iconRes = android.R.drawable.ic_menu_view,
                badgeText = "AI"
            )
        }
        adapterRecent.addActivity(newItem)
    }

    // Wrap all of the audio/image/video processes together down the line so they can all be start/stopped together
    private fun endAllProcesses() {
        if (isAudioActive) {
            //smartGlassesIntegration.stopAudio()
            //audioStreamer.stop()
            //whisperWebSocket.close()
            isAudioActive = false
        }
        if (isAudioInactive) {
            //smartGlassesIntegration.stopAudio()
            //audioStreamer.stop()
            //whisperWebSocket.close()
            isAudioInactive = false
        }
        if (isImageActive) {
            //imageStreamer.stop()
            isImageActive = false
        }
        if (isVideoActive) {
            //smartGlassesIntegration.stopVideo()
            //videoStreamer.stop()
            isVideoActive = false
        }
        if (isVideoInactive) {
            //smartGlassesIntegration.stopVideo()
            //videoStreamer.stop()
            isVideoInactive = false
        }
        if (isThumbnailActive) {
            //thumbnailBuffer = null
            isThumbnailActive = false
        }
    }

    fun exportTimingsToCsv(file: File, timings: List<LatencyTiming>) {
        file.printWriter().use { out ->
            out.println("Start Time,Finish Time")
            timings.forEach { t ->
                out.println("${t.startTime ?: ""},${t.finishTime ?: ""}")
            }
        }
    }

}

object GalleryStore {
    val items = mutableListOf<GalleryActivityItem>()

    fun addItem(item: GalleryActivityItem) {
        if (!items.any { it.media == item.media }) {
            items.add(0, item) // newest at front
        }
    }
}