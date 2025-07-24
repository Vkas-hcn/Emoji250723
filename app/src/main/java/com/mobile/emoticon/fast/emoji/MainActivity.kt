package com.mobile.emoticon.fast.emoji

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.mobile.emoticon.fast.emoji.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.random.Random
import androidx.core.graphics.createBitmap

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var emojiAdapter: EmojiAdapter
    private lateinit var rabbitAdapter: SimpleEmojiAdapter
    private lateinit var catAdapter: SimpleEmojiAdapter
    private lateinit var bearAdapter: SimpleEmojiAdapter

    private val fixedCompositeEmojis: List<EmojiItem.CompositeEmoji> by lazy {
        generateFixedCompositeEmojis()
    }

    private var currentSelectedType = EmojiType.EMOJI

    private var selectedEmojiCount = 0
    private var firstSelectedEmoji: EmojiItem? = null
    private var secondSelectedEmoji: EmojiItem? = null
    private var resultBitmap: Bitmap? = null

    private var currentSelectedImage: Int? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            currentSelectedImage?.let { resourceId ->
                downloadSingleImage(resourceId)
            } ?: downloadImage()
        } else {
            Toast.makeText(this, "Need to store permissions to download pictures", Toast.LENGTH_SHORT).show()
        }
    }

    private val batchDownloadPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pendingBatchDownloadType?.let { type ->
                executeBatchDownload(type)
            }
        } else {
            Toast.makeText(this, "Need to store permissions to download pictures", Toast.LENGTH_SHORT).show()
        }
        pendingBatchDownloadType = null
    }

    private var pendingBatchDownloadType: EmojiType? = null

    enum class EmojiType {
        EMOJI, FLOWER, FOOT, FRUITS, HEART,Cat, Bear, Rabbit
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupListRecyclerViews()
        clickFun()
        initEmojiType()
    }

    private fun setupRecyclerView() {
        emojiAdapter = EmojiAdapter { emojiItem ->
            onEmojiItemClick(emojiItem)
        }

        with(binding.inHome.rvEmoji) {
            layoutManager = GridLayoutManager(this@MainActivity, 4) // 一行4个
            adapter = emojiAdapter
        }
    }

    private fun setupListRecyclerViews() {
        rabbitAdapter = SimpleEmojiAdapter { resourceId ->
            showImageDialog(resourceId)
        }
        with(binding.inList.rvDown1) {
            layoutManager = GridLayoutManager(this@MainActivity, 5)
            adapter = rabbitAdapter
        }
        rabbitAdapter.updateData(EmojiDataUtils.iconRabbit)


        catAdapter = SimpleEmojiAdapter { resourceId ->
            showImageDialog(resourceId)
        }
        with(binding.inList.rvDown2) {
            layoutManager = GridLayoutManager(this@MainActivity, 5)
            adapter = catAdapter
        }
        catAdapter.updateData(EmojiDataUtils.iconCat)


        bearAdapter = SimpleEmojiAdapter { resourceId ->
            showImageDialog(resourceId)
        }
        with(binding.inList.rvDown3) {
            layoutManager = GridLayoutManager(this@MainActivity, 5)
            adapter = bearAdapter
        }
        bearAdapter.updateData(EmojiDataUtils.iconBear)


    }

    private fun initEmojiType() {
        switchToEmojiType(EmojiType.EMOJI)
    }

    private fun clickFun() {
        with(binding) {
            imgHome.setOnClickListener {
                inHome.home.isVisible = true
                inList.list.isVisible = false
                inSetting.setting.isVisible = false
                imgHome.setImageResource(R.drawable.ic_home_1)
                imgList.setImageResource(R.drawable.ic_list_2)
                imgSetting.setImageResource(R.drawable.ic_setting_2)
            }
            imgList.setOnClickListener {
                inHome.home.isVisible = false
                inList.list.isVisible = true
                inSetting.setting.isVisible = false
                imgHome.setImageResource(R.drawable.ic_home_2)
                imgList.setImageResource(R.drawable.ic_list_1)
                imgSetting.setImageResource(R.drawable.ic_setting_2)
            }
            imgSetting.setOnClickListener {
                inHome.home.isVisible = false
                inList.list.isVisible = false
                inSetting.setting.isVisible = true
                imgHome.setImageResource(R.drawable.ic_home_2)
                imgList.setImageResource(R.drawable.ic_list_2)
                imgSetting.setImageResource(R.drawable.ic_setting_1)
            }

            inHome.llEmoji.setOnClickListener {
                switchToEmojiType(EmojiType.EMOJI)
            }

            inHome.llFlower.setOnClickListener {
                switchToEmojiType(EmojiType.FLOWER)
            }

            inHome.llFoot.setOnClickListener {
                switchToEmojiType(EmojiType.FOOT)
            }

            inHome.llEmojiFruits.setOnClickListener {
                switchToEmojiType(EmojiType.FRUITS)
            }

            inHome.llEmojiHeart.setOnClickListener {
                switchToEmojiType(EmojiType.HEART)
            }

            inHome.imgBgRef.setOnClickListener {
                resetSelection()
            }

            inHome.tvShare.setOnClickListener {
                shareImage()
            }

            inHome.tvDown.setOnClickListener {
                checkPermissionAndDownload()
            }

            inHome.conDialog.setOnClickListener {
                inHome.conDialog.isVisible = false
            }

            inList.tvShare.setOnClickListener {
                shareCurrentImage()
            }

            inList.tvDown.setOnClickListener {
                checkPermissionAndDownloadSingle()
            }

            inList.conDialog.setOnClickListener {
                inList.conDialog.isVisible = false
            }

            inList.imgDownload1All.setOnClickListener {
                showBatchDownloadConfirmDialog(EmojiType.Rabbit)
            }

            inList.imgDownload2All.setOnClickListener {
                showBatchDownloadConfirmDialog(EmojiType.Cat)
            }
            inList.imgDownload3All.setOnClickListener {
                showBatchDownloadConfirmDialog(EmojiType.Bear)
            }
            inSetting.atvPp.setOnClickListener {
                // 跳转浏览器
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://baidu.com"))
                startActivity(intent)
            }
            inSetting.atvShare.setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(
                    Intent.EXTRA_TEXT,
                    "https://play.google.com/store/apps/details?id=${this@MainActivity.packageName}"
                )
                startActivity(Intent.createChooser(intent, "share"))
            }
        }
    }

    private fun switchToEmojiType(type: EmojiType) {
        currentSelectedType = type
        updateSelectedState(type)

        val emojiList = when (type) {
            EmojiType.EMOJI -> {
                fixedCompositeEmojis
            }
            EmojiType.FLOWER -> {
                EmojiDataUtils.iconFlower.map { EmojiItem.SingleEmoji(it) }
            }
            EmojiType.FOOT -> {
                EmojiDataUtils.iconFoot.map { EmojiItem.SingleEmoji(it) }
            }
            EmojiType.FRUITS -> {
                EmojiDataUtils.iconFruits.map { EmojiItem.SingleEmoji(it) }
            }
            EmojiType.HEART -> {
                EmojiDataUtils.iconHeart.map { EmojiItem.SingleEmoji(it) }
            }
            else -> fixedCompositeEmojis
        }

        emojiAdapter.updateData(emojiList)
    }

    private fun updateSelectedState(selectedType: EmojiType) {
        with(binding.inHome) {
            imgCheckEmoji.visibility = View.GONE
            imgCheckFlower.visibility = View.GONE
            imgCheckFoot.visibility = View.GONE
            imgCheckFruits.visibility = View.GONE
            imgCheckHeart.visibility = View.GONE

            when (selectedType) {
                EmojiType.EMOJI -> imgCheckEmoji.visibility = View.VISIBLE
                EmojiType.FLOWER -> imgCheckFlower.visibility = View.VISIBLE
                EmojiType.FOOT -> imgCheckFoot.visibility = View.VISIBLE
                EmojiType.FRUITS -> imgCheckFruits.visibility = View.VISIBLE
                EmojiType.HEART -> imgCheckHeart.visibility = View.VISIBLE
                else -> {}
            }
        }
    }

    private fun generateFixedCompositeEmojis(): List<EmojiItem.CompositeEmoji> {
        val random = Random(12345)
        val compositeEmojis = mutableListOf<EmojiItem.CompositeEmoji>()

        repeat(40) {
            val faceId = EmojiDataUtils.iconFace.random(random)
            val eyeId = EmojiDataUtils.iconEye.random(random)
            val mouthId = EmojiDataUtils.iconMouth.random(random)
            val handId = EmojiDataUtils.iconHand.random(random)

            compositeEmojis.add(
                EmojiItem.CompositeEmoji(
                    faceId = faceId,
                    eyeId = eyeId,
                    mouthId = mouthId,
                    handId = handId
                )
            )
        }

        return compositeEmojis
    }

    private fun onEmojiItemClick(emojiItem: EmojiItem) {
        when (selectedEmojiCount) {
            0 -> {
                firstSelectedEmoji = emojiItem
                selectedEmojiCount = 1
                setEmojiToPosition(emojiItem, 1)
            }
            1 -> {
                secondSelectedEmoji = emojiItem
                selectedEmojiCount = 2
                setEmojiToPosition(emojiItem, 2)

                val compositeResult = createCompositeEmoji(firstSelectedEmoji!!, secondSelectedEmoji!!)
                showResultDialog(compositeResult)
            }
            else -> {
                resetSelection()
                firstSelectedEmoji = emojiItem
                selectedEmojiCount = 1
                setEmojiToPosition(emojiItem, 1)
            }
        }
    }

    private fun setEmojiToPosition(emojiItem: EmojiItem, position: Int) {
        when (position) {
            1 -> {
                with(binding.inHome) {
                    tv1.visibility = View.GONE
                    img1.visibility = View.VISIBLE
                    when (emojiItem) {
                        is EmojiItem.SingleEmoji -> {
                            img1.setImageResource(emojiItem.resourceId)
                        }
                        is EmojiItem.CompositeEmoji -> {
                            val bitmap = EmojiUtils.createEmojiBitmap(this@MainActivity, emojiItem, 112)
                            img1.setImageBitmap(bitmap)
                        }
                    }
                }
            }
            2 -> {
                with(binding.inHome) {
                    tv2.visibility = View.GONE
                    img2.visibility = View.VISIBLE
                    when (emojiItem) {
                        is EmojiItem.SingleEmoji -> {
                            img2.setImageResource(emojiItem.resourceId)
                        }
                        is EmojiItem.CompositeEmoji -> {
                            val bitmap = EmojiUtils.createEmojiBitmap(this@MainActivity, emojiItem, 112)
                            img2.setImageBitmap(bitmap)
                        }
                    }
                }
            }
        }
    }

    private fun createCompositeEmoji(first: EmojiItem, second: EmojiItem): Bitmap {
        return when {
            first is EmojiItem.CompositeEmoji && second is EmojiItem.CompositeEmoji -> {
                val newComposite = EmojiItem.CompositeEmoji(
                    faceId = if (Random.nextBoolean()) first.faceId else second.faceId,
                    eyeId = if (Random.nextBoolean()) first.eyeId else second.eyeId,
                    mouthId = if (Random.nextBoolean()) first.mouthId else second.mouthId,
                    handId = if (Random.nextBoolean()) first.handId else second.handId
                )
                EmojiUtils.createEmojiBitmap(this, newComposite, 200)
            }
            else -> {
                createBlendedBitmap(first, second)
            }
        }
    }

    private fun createBlendedBitmap(first: EmojiItem, second: EmojiItem): Bitmap {
        val size = 200

        val firstBitmap = when (first) {
            is EmojiItem.SingleEmoji -> {
                EmojiUtils.createSingleEmojiBitmap(this, first, size)
            }
            is EmojiItem.CompositeEmoji -> {
                EmojiUtils.createEmojiBitmap(this, first, size)
            }
        }

        val secondBitmap = when (second) {
            is EmojiItem.SingleEmoji -> {
                EmojiUtils.createSingleEmojiBitmap(this, second, size)
            }
            is EmojiItem.CompositeEmoji -> {
                EmojiUtils.createEmojiBitmap(this, second, size)
            }
        }

        return if (Random.nextBoolean()) {
            EmojiUtils.advancedBlendBitmaps(firstBitmap, secondBitmap)
        } else {
            EmojiUtils.creativeBlendBitmaps(firstBitmap, secondBitmap)
        }
    }

    private fun showResultDialog(resultBitmap: Bitmap) {
        this.resultBitmap = resultBitmap
        with(binding.inHome) {
            imgResult.setImageBitmap(resultBitmap)
            conDialog.isVisible = true
        }
    }

    private fun showImageDialog(resourceId: Int) {
        currentSelectedImage = resourceId
        with(binding.inList) {
            imgResult.setImageResource(resourceId)
            conDialog.isVisible = true
        }
    }

    private fun showBatchDownloadConfirmDialog(type: EmojiType) {


        val imageCount = when (type) {
            EmojiType.Bear -> EmojiDataUtils.iconBear.size
            EmojiType.Rabbit -> EmojiDataUtils.iconRabbit.size
            EmojiType.Cat -> EmojiDataUtils.iconCat.size
            else -> 0
        }

        AlertDialog.Builder(this)
            .setTitle("Batch download")
            .setMessage("Are you sure you want to download all the pictures? A total of ${imageCount} pictures")
            .setPositiveButton("Sure") { _, _ ->
                checkPermissionAndBatchDownload(type)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetSelection() {
        selectedEmojiCount = 0
        firstSelectedEmoji = null
        secondSelectedEmoji = null
        resultBitmap = null

        with(binding.inHome) {
            tv1.visibility = View.VISIBLE
            img1.visibility = View.GONE
            tv2.visibility = View.VISIBLE
            img2.visibility = View.GONE

            conDialog.isVisible = false
        }
    }

    private fun shareImage() {
        resultBitmap?.let { bitmap ->
            try {
                val cachePath = File(externalCacheDir, "images")
                cachePath.mkdirs()
                val file = File(cachePath, "emoji_composite_${System.currentTimeMillis()}.png")

                val fileOutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.close()

                val imageUri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Share expressions"))
                binding.inHome.conDialog.isVisible = false
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Sharing failed：${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No pictures to share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareCurrentImage() {
        currentSelectedImage?.let { resourceId ->
            try {
                val bitmap = ContextCompat.getDrawable(this, resourceId)?.let { drawable ->
                    val bitmap = Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap
                }

                bitmap?.let { bmp ->
                    val cachePath = File(externalCacheDir, "images")
                    cachePath.mkdirs()
                    val file = File(cachePath, "emoji_single_${System.currentTimeMillis()}.png")

                    val fileOutputStream = FileOutputStream(file)
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                    fileOutputStream.close()

                    val imageUri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        file
                    )

                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, imageUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    binding.inList.conDialog.isVisible = false
                    startActivity(Intent.createChooser(shareIntent, "Share expressions"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Sharing failed：${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No pictures to share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadImage()
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                downloadImage()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun checkPermissionAndDownloadSingle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            currentSelectedImage?.let { resourceId ->
                downloadSingleImage(resourceId)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                currentSelectedImage?.let { resourceId ->
                    downloadSingleImage(resourceId)
                }
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun checkPermissionAndBatchDownload(type: EmojiType) {
        pendingBatchDownloadType = type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            executeBatchDownload(type)
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                executeBatchDownload(type)
            } else {
                batchDownloadPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun downloadImage() {
        resultBitmap?.let { bitmap ->
            try {
                val filename = "emoji_composite_${System.currentTimeMillis()}.png"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }

                    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        contentResolver.openOutputStream(it)?.use { outputStream ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        }
                        Toast.makeText(this, "Images saved to album", Toast.LENGTH_SHORT).show()
                    }
                    binding.inHome.conDialog.isVisible = false
                } else {
                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val file = File(picturesDir, filename)

                    val fileOutputStream = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                    fileOutputStream.close()

                    sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                    binding.inHome.conDialog.isVisible = false
                    Toast.makeText(this, "Images saved to album", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Saving failed：${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No picture to save", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadSingleImage(resourceId: Int) {
        try {
            // 将资源转换为Bitmap
            val bitmap = ContextCompat.getDrawable(this, resourceId)?.let { drawable ->
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }

            bitmap?.let { bmp ->
                val filename = "emoji_single_${System.currentTimeMillis()}.png"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }

                    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        contentResolver.openOutputStream(it)?.use { outputStream ->
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        }
                        binding.inList.conDialog.isVisible = false
                        Toast.makeText(this, "Images saved to album", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val file = File(picturesDir, filename)

                    val fileOutputStream = FileOutputStream(file)
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                    fileOutputStream.close()

                    sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                    binding.inList.conDialog.isVisible = false

                    Toast.makeText(this, "Images saved to album", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.inList.conDialog.isVisible = false
            Toast.makeText(this, "Saving failed：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun executeBatchDownload(type: EmojiType) {
        val resourceList = when (type) {
            EmojiType.Rabbit -> EmojiDataUtils.iconRabbit
            EmojiType.Bear -> EmojiDataUtils.iconBear
            EmojiType.Cat -> EmojiDataUtils.iconCat

            else -> return
        }

        val typeName = when (type) {
            EmojiType.Rabbit -> "Rabbit"
            EmojiType.Bear -> "Bear"
            EmojiType.Cat -> "Cat"
            else -> "emoji"
        }

        CoroutineScope(Dispatchers.IO).launch {
            var successCount = 0
            var failCount = 0

            for ((index, resourceId) in resourceList.withIndex()) {
                try {
                    val bitmap = withContext(Dispatchers.Main) {
                        ContextCompat.getDrawable(this@MainActivity, resourceId)?.let { drawable ->
                            val bitmap =
                                createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
                            val canvas = Canvas(bitmap)
                            drawable.setBounds(0, 0, canvas.width, canvas.height)
                            drawable.draw(canvas)
                            bitmap
                        }
                    }

                    bitmap?.let { bmp ->
                        val filename = "${typeName}_${index + 1}_${System.currentTimeMillis()}.png"

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val contentValues = ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                            }

                            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                            uri?.let {
                                contentResolver.openOutputStream(it)?.use { outputStream ->
                                    bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                                }
                                successCount++
                            } ?: run { failCount++ }
                        } else {
                            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                            val file = File(picturesDir, filename)

                            val fileOutputStream = FileOutputStream(file)
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                            fileOutputStream.close()

                            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                            successCount++
                        }
                    } ?: run { failCount++ }

                    delay(100)

                } catch (e: Exception) {
                    e.printStackTrace()
                    failCount++
                }
            }

            withContext(Dispatchers.Main) {
                val message = if (failCount == 0) {
                    "Successfully downloaded ${successCount} pictures to the album"
                } else {
                    "Download completed: Success ${successCount} Zhang, failed ${failCount} Zhang"
                }
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
            }
        }

        Toast.makeText(this, "Start batch download, please wait...", Toast.LENGTH_SHORT).show()
    }
}