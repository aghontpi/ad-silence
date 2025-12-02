package bluepie.ad_silence

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogAdapter(private var logs: List<LogEntry>) : BaseAdapter() {

    override fun getCount(): Int = logs.size

    override fun getItem(position: Int): LogEntry = logs[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: LogViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
            holder = LogViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as LogViewHolder
        }

        val log = getItem(position)
        val context = parent.context

        // App Name and Timestamp
        val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
        val timeString = timeFormat.format(Date(log.timestamp))
        holder.appNameTimestamp.text = "${log.appName} $timeString"

        if (log.isAd) {
            holder.statusText.text = "Has Contained Text"
            holder.statusText.setTextColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.holo_green_light))
        } else {
            holder.statusText.text = "Clean"
            holder.statusText.setTextColor(holder.defaultStatusTextColor)
        }

        holder.logTitle.text = "Title: ${log.title}"
        holder.logText.text = "Text: ${log.text}"
        holder.logSubtext.text = "SubText: ${log.subText}"

        if (!log.matchedText.isNullOrEmpty()) {
            holder.matchedText.visibility = View.VISIBLE
            holder.matchedText.text = "Checked Text: ${log.matchedText}"
        } else {
            holder.matchedText.visibility = View.GONE
        }

        // Expansion Logic
        if (log.isExpanded) {
            holder.detailsContainer.visibility = View.VISIBLE
            holder.expandIcon.rotation = 180f
        } else {
            holder.detailsContainer.visibility = View.GONE
            holder.expandIcon.rotation = 0f
        }

        // Note for future: In ListView, the item click is usually handled by OnItemClickListener,
        // but since we have internal clickable logic (expansion), we can handle it here or let the ListView handle it.
        // However, setting OnClickListener on the root view inside getView might interfere with ListView selector.
        // A better approach for ListView expansion is to handle it in the Activity's OnItemClickListener.
        // But for simplicity and to match previous behavior, I'll leave the click logic here but be aware of focus issues.
        // Actually, for ListView, it's better to NOT set OnClickListener on the root view if we want standard list behavior.
        // But we want the whole item to be clickable to toggle expansion.
        // Let's rely on the Activity's OnItemClickListener to toggle expansion.
        
        return view
    }

    class LogViewHolder(view: View) {
        val appIcon: ImageView = view.findViewById(R.id.app_icon)
        val appNameTimestamp: TextView = view.findViewById(R.id.app_name_timestamp)

        val statusText: TextView = view.findViewById(R.id.status_text)
        val defaultStatusTextColor: Int = statusText.currentTextColor
        val expandIcon: ImageView = view.findViewById(R.id.expand_icon)
        val detailsContainer: LinearLayout = view.findViewById(R.id.details_container)
        val logTitle: TextView = view.findViewById(R.id.log_title)
        val logText: TextView = view.findViewById(R.id.log_text)
        val logSubtext: TextView = view.findViewById(R.id.log_subtext)
        val matchedText: TextView = view.findViewById(R.id.matched_text)
    }

    fun updateLogs(newLogs: List<LogEntry>) {
        logs = newLogs
        notifyDataSetChanged()
    }
}
