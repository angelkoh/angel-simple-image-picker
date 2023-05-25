package angel.androidapps.imagepicker

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RvAdapter(private val onSelected: (Uri) -> Unit) : RecyclerView.Adapter<RvAdapter.VH>() {

    private var list: List<Uri> = emptyList()

    fun set(list: List<Uri>) {
        if (list.size <= 1) {
            //remove if only 1 image
            this.list = emptyList()
        } else {
            this.list = list
        }
        notifyDataSetChanged()
    }


    class VH(itemView: View, private val onSelected: (Uri) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.iv_image)

        fun bind(uri: Uri) {
            ivImage.setOnClickListener { onSelected.invoke(uri) }
            Glide.with(ivImage)
                .load(uri)
                .into(ivImage)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.rv_item_image, parent, false),
        onSelected
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size
}