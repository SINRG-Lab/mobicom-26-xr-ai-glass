package com.sdk.glassessdksample.ui

import GalleryActivityAdapter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sdk.glassessdksample.GalleryActivityItem
import com.sdk.glassessdksample.GalleryStore
import com.sdk.glassessdksample.R
import com.sdk.glassessdksample.databinding.ActivityGalleryBinding
import java.io.File

class GalleryActivity : AppCompatActivity () {
    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapterGallery: GalleryActivityAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recyclerGallery = findViewById<RecyclerView>(R.id.gallery_recycler)
        adapterGallery = GalleryActivityAdapter(GalleryStore.items)
        recyclerGallery.layoutManager = GridLayoutManager(this, 2)
        recyclerGallery.adapter = adapterGallery

        //val singlePath = intent.getStringExtra("downloaded_path")
        //addGalleryActivity(singlePath)
    }

    fun addGalleryActivity(imagePath: String?) {
        val newItem = GalleryActivityItem(
            title = File(imagePath).name,
            description = "",
            media = imagePath
        )
        adapterGallery.addActivity(newItem)
    }
}