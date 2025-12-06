package bluepie.ad_silence

import java.util.LinkedList
import java.util.concurrent.CopyOnWriteArrayList

data class LogEntry(
    val appName: String,
    val timestamp: Long,
    val isAd: Boolean,
    val title: String,
    val text: String,
    val subText: String,
    val matchedText: String? = null,
    var isExpanded: Boolean = false
)

object LogManager {
    private val logs = LinkedList<LogEntry>()
    private val lifecycleLogs = LinkedList<LogEntry>()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    @Volatile var isLoggingEnabled: Boolean = false

    fun addLog(entry: LogEntry) {
        if (!isLoggingEnabled) return
        synchronized(logs) {
            logs.addFirst(entry)
            // Keep only last 100 logs to avoid memory issues
            if (logs.size > 100) {
                logs.removeLast()
            }
        }
        notifyListeners()
    }

    fun addLifecycleLog(entry: LogEntry) {
        if (!isLoggingEnabled) return
        synchronized(lifecycleLogs) {
            lifecycleLogs.addFirst(entry)
            // Keep only last 100 lifecycle logs
            if (lifecycleLogs.size > 100) {
                lifecycleLogs.removeLast()
            }
        }
        notifyListeners()
    }

    fun getLogs(): List<LogEntry> {
        val allLogs = ArrayList<LogEntry>()
        synchronized(logs) {
            allLogs.addAll(logs)
        }
        synchronized(lifecycleLogs) {
            allLogs.addAll(lifecycleLogs)
        }
        // show newest first
        allLogs.sortByDescending { it.timestamp }
        return allLogs
    }

    fun clearLogs() {
        synchronized(logs) {
            logs.clear()
        }
        synchronized(lifecycleLogs) {
           lifecycleLogs.clear()
        }
        notifyListeners()
    }

    fun addMockData() {
        val mockLogs = listOf(
            LogEntry("Spotify", System.currentTimeMillis(), true, "Song Title", "Artist Name", "Album Info", "Artist Name"),
            LogEntry("YouTube", System.currentTimeMillis() - 60000, false, "Video Title", "Channel Name", "Description", null),
            LogEntry("Pandora", System.currentTimeMillis() - 120000, true, "Ad Title", "Ad Text", "Sponsored", "Ad Text"),
            LogEntry("System", System.currentTimeMillis() - 180000, false, "Service Started", "AdSilence", "Background Service", null)
        )
        synchronized(logs) {
            logs.addAll(0, mockLogs)
        }
        notifyListeners()
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it.invoke() }
    }
}
