import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sdk.glassessdksample.GalleryActivityItem
import com.sdk.glassessdksample.R
import java.io.File

class GalleryActivityAdapter(
    private val activities: MutableList<GalleryActivityItem>
) : RecyclerView.Adapter<GalleryActivityAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.gallery_title)
        val description: TextView = view.findViewById(R.id.gallery_description)
        val media: ImageView = view.findViewById(R.id.gallery_media)
        val playIcon: ImageView = view.findViewById(R.id.gallery_play_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_activity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = activities[position]
        holder.title.text = activity.title
        holder.description.text = activity.description

        val mediaFile = activity.media
        val context = holder.media.context

        if (mediaFile?.contains("mp4") == true) {
            // Load video thumbnail
            Glide.with(context)
                .asBitmap()
                .load(mediaFile)
                .frame(1000000)
                .centerCrop()
                .into(holder.media)

            holder.playIcon.visibility = View.VISIBLE
        } else {
            // Load image normally
            Glide.with(context)
                .load(mediaFile)
                .centerCrop()
                .into(holder.media)

            holder.playIcon.visibility = View.GONE
        }

        // Handle click to open
        holder.media.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            val file = File(mediaFile)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val type = if (file.extension == "mp4") "video/mp4" else "image/*"
            intent.setDataAndType(uri, type)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = activities.size

    fun addActivity(item: GalleryActivityItem) {
        if (activities.size >= 20) activities.removeAt(activities.size - 1)
        activities.add(0, item)
        notifyItemInserted(0)
    }
}