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
    fun addLog(entry: LogEntry) {
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
