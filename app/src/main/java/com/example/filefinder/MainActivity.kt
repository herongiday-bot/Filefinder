package com.example.filefinder

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.graphics.Typeface
import android.widget.*
import androidx.documentfile.provider.DocumentFile
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : Activity() {

    private val COLOR_ORANGE = 0xFFFF8A00.toInt()
    private val COLOR_BG = 0xFF101010.toInt()
    private val COLOR_PANEL = 0xFF1C1C1C.toInt()
    private val COLOR_MUTED = 0xFFAAAAAA.toInt()
    private val COLOR_WHITE = 0xFFF5F5F5.toInt()

    private val prefs by lazy { getSharedPreferences("finder", MODE_PRIVATE) }
    private val executor = Executors.newSingleThreadExecutor()
    private val roots = mutableListOf<DocumentFile>()
    private val searchVersion = AtomicInteger(0)

    private lateinit var query: EditText
    private lateinit var results: LinearLayout
    private lateinit var status: TextView
    private lateinit var count: TextView

    companion object {
        private const val PICK_FOLDER = 41
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        restoreFolders()
        showWelcome()
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(12))
            setBackgroundColor(COLOR_BG)
        }

        val brand = TextView(this).apply {
            text = "FILE FINDER"
            textSize = 13f
            letterSpacing = 0.18f
            setTextColor(COLOR_ORANGE)
            typeface = Typeface.DEFAULT_BOLD
        }
        page.addView(brand)

        val title = TextView(this).apply {
            text = "Find anything."
            textSize = 30f
            setTextColor(COLOR_WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(4))
        }
        page.addView(title)

        val subtitle = TextView(this).apply {
            text = "Search files across folders you choose."
            textSize = 14f
            setTextColor(COLOR_MUTED)
        }
        page.addView(subtitle)

        query = EditText(this).apply {
            hint = "Search by file name or extension…"
            setHintTextColor(0xFF777777.toInt())
            setTextColor(COLOR_WHITE)
            textSize = 16f
            setSingleLine(true)
            setPadding(dp(14), 0, dp(14), 0)
            background = rounded(COLOR_PANEL, dp(12))
        }

        val searchBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, dp(22), 0, dp(12))
        }
        searchBox.addView(query, LinearLayout.LayoutParams(0, dp(52), 1f))

        val go = Button(this).apply {
            text = "SEARCH"
            setTextColor(0xFF171717.toInt())
            setBackgroundColor(COLOR_ORANGE)
            isAllCaps = false
            setOnClickListener { searchFiles(query.text.toString()) }
        }
        val goParams = LinearLayout.LayoutParams(dp(94), dp(52)).apply {
            leftMargin = dp(8)
        }
        searchBox.addView(go, goParams)
        page.addView(searchBox)

        val choose = Button(this).apply {
            text = "＋  Choose folders to search"
            isAllCaps = false
            setTextColor(COLOR_ORANGE)
            setBackgroundColor(COLOR_PANEL)
            setOnClickListener { chooseFolder() }
        }
        page.addView(choose, LinearLayout.LayoutParams(-1, dp(48)))

        val infoRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, dp(18), 0, dp(8))
        }

        status = TextView(this).apply {
            text = "Ready"
            textSize = 13f
            setTextColor(COLOR_MUTED)
        }
        count = TextView(this).apply {
            text = ""
            textSize = 13f
            setTextColor(COLOR_ORANGE)
            gravity = android.view.Gravity.RIGHT
        }
        infoRow.addView(status, LinearLayout.LayoutParams(0, -2, 1f))
        infoRow.addView(count, LinearLayout.LayoutParams(0, -2, 1f))
        page.addView(infoRow)

        val scroll = ScrollView(this)
        results = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(results)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(page)

        query.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                val term = s?.toString().orEmpty()
                if (term.length >= 2) {
                    searchFiles(term)
                } else if (term.isEmpty()) {
                    showWelcome()
                }
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun chooseFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, PICK_FOLDER)
    }

    @Deprecated("Legacy result callback is used for broad Android compatibility.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FOLDER && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val flags = data.flags and
                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            try {
                contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {
            }

            if (roots.none { it.uri == uri }) {
                DocumentFile.fromTreeUri(this, uri)?.let { roots.add(it) }
            }
            saveFolders()
            status.text = "${roots.size} folder location(s) selected"

            if (query.text.length >= 2) {
                searchFiles(query.text.toString())
            } else {
                showWelcome()
            }
        }
    }

    private fun restoreFolders() {
        prefs.getStringSet("uris", emptySet())?.forEach { raw ->
            try {
                val uri = Uri.parse(raw)
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                DocumentFile.fromTreeUri(this, uri)?.let { roots.add(it) }
            } catch (_: Exception) {
            }
        }
    }

    private fun saveFolders() {
        prefs.edit()
            .putStringSet("uris", roots.map { it.uri.toString() }.toSet())
            .apply()
    }

    private fun searchFiles(raw: String) {
        val term = raw.trim().lowercase()
        if (term.length < 2) return

        val version = searchVersion.incrementAndGet()
        status.text = "Searching…"
        count.text = ""
        results.removeAllViews()

        executor.execute {
            val found = mutableListOf<DocumentFile>()
            var visited = 0

            fun walk(folder: DocumentFile, depth: Int) {
                if (depth > 30 || found.size >= 500) return

                val children = try {
                    folder.listFiles()
                } catch (_: Exception) {
                    emptyArray()
                }

                for (file in children) {
                    visited++
                    if (file.name.orEmpty().lowercase().contains(term)) {
                        found.add(file)
                    }
                    if (file.isDirectory) {
                        walk(file, depth + 1)
                    }
                    if (found.size >= 500) break
                }
            }

            roots.forEach { walk(it, 0) }

            runOnUiThread {
                if (version != searchVersion.get()) return@runOnUiThread

                status.text = if (roots.isEmpty()) {
                    "Choose at least one folder to begin."
                } else {
                    "Searched $visited files and folders"
                }
                count.text = "${found.size} result(s)"

                when {
                    roots.isEmpty() -> addMessage(
                        "No folders selected",
                        "Tap “Choose folders to search” and select a folder such as Internal storage or Downloads."
                    )
                    found.isEmpty() -> addMessage(
                        "No matches found",
                        "Try a shorter name or search for an extension such as .pdf or .xlsx."
                    )
                    else -> found.sortedBy { it.name?.lowercase() }.forEach {
                        addFileRow(it)
                    }
                }
            }
        }
    }

    private fun addFileRow(file: DocumentFile) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(COLOR_PANEL, dp(10))
        }

        val name = TextView(this).apply {
            text = file.name ?: "Unnamed file"
            textSize = 15f
            setTextColor(COLOR_WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        val details = TextView(this).apply {
            val size = if (file.isFile) formatSize(file.length()) else "Folder"
            val date = if (file.lastModified() > 0) {
                DateFormat.getDateInstance().format(Date(file.lastModified()))
            } else {
                "Date unavailable"
            }
            text = "$size  •  $date"
            textSize = 12f
            setTextColor(COLOR_MUTED)
            setPadding(0, dp(5), 0, 0)
        }

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.RIGHT
        }
        actions.addView(smallButton("OPEN") { openFile(file) })
        actions.addView(smallButton("SHARE") { shareFile(file) })

        if (file.canWrite()) {
            actions.addView(smallButton("DELETE") {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Delete file?")
                    .setMessage("Delete ${file.name}? This cannot be undone.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        if (file.delete()) {
                            searchFiles(query.text.toString())
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Could not delete this item.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .show()
            })
        }

        card.addView(name)
        card.addView(details)
        card.addView(actions)

        val params = LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(9)
        }
        results.addView(card, params)
    }

    private fun smallButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 10f
            isAllCaps = true
            setTextColor(COLOR_ORANGE)
            setBackgroundColor(COLOR_PANEL)
            setOnClickListener { action() }
        }
    }

    private fun openFile(file: DocumentFile) {
        try {
            val mime = contentResolver.getType(file.uri) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(file.uri, mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "No app found to open this file.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(file: DocumentFile) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = contentResolver.getType(file.uri) ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, file.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share file"))
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to share this file.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showWelcome() {
        results.removeAllViews()

        if (roots.isEmpty()) {
            addMessage(
                "Welcome to File Finder",
                "Choose the folders you want to search. You can select more than one folder, then search by filename or extension."
            )
            status.text = "No folders selected"
        } else {
            addMessage(
                "Ready to search",
                "${roots.size} folder location(s) selected. Type at least two characters to see live results, or press SEARCH."
            )
            status.text = "Ready"
        }
        count.text = ""
    }

    private fun addMessage(titleText: String, bodyText: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(20))
            background = rounded(COLOR_PANEL, dp(12))
        }

        val title = TextView(this).apply {
            text = titleText
            textSize = 17f
            setTextColor(COLOR_WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        val body = TextView(this).apply {
            text = bodyText
            textSize = 13f
            setTextColor(COLOR_MUTED)
            setPadding(0, dp(8), 0, 0)
        }

        box.addView(title)
        box.addView(body)
        results.addView(box)
    }

    private fun rounded(color: Int, radius: Int): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        return String.format("%.2f GB", mb / 1024.0)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
