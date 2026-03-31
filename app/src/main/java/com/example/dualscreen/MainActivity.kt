package com.example.dualscreen

import android.Manifest
import android.app.Activity
import android.app.Presentation
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private lateinit var displayManager: DisplayManager
    private var primaryWebView: WebView? = null
    private var secondaryPresentation: SecondaryDisplayPresentation? = null
    private lateinit var prefs: SharedPreferences

    companion object {
        private const val PREFS_NAME = "dual_screen_prefs"
        private const val KEY_POS_URL = "pos_url"
        private const val KEY_CUSTOMER_URL = "customer_url"
        private const val KEY_HIDE_KEYBOARD = "hide_keyboard"
        private const val DEFAULT_POS_URL = "http://192.168.5.21/esb-fnb-pos/en/login"
        private const val DEFAULT_CUSTOMER_URL = "http://192.168.5.21/esb-fnb-pos/en/customer-display"
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Handle incoming intent (esbprint:// URL scheme)
        handleIntent(intent)

        // Request Bluetooth permissions
        requestBluetoothPermissions()

        // Inisialisasi SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Inisialisasi DisplayManager
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

        // Cek apakah ini pertama kali 运行 atau butuh konfigurasi
        if (needsConfiguration()) {
            showSettingsUI()
        } else {
            setupDualScreen()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "esbprint") {
                // Handle esbprint:// URL scheme - load in primary WebView
                val printUrl = uri.toString()
                Toast.makeText(this, "Print request received: $printUrl", Toast.LENGTH_SHORT).show()
                
                // If WebView is already loaded, navigate to the print URL
                primaryWebView?.let { webView ->
                    webView.loadUrl(printUrl)
                } ?: run {
                    // Store the URL to load after WebView is initialized
                    prefs.edit().putString("pending_print_url", printUrl).apply()
                }
            }
        }
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
            
            val permissionsToRequest = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_BLUETOOTH_PERMISSIONS)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            // Permissions handled - app will work with or without Bluetooth permissions
            // WebView will still function for POS and Customer display
        }
    }

    private fun needsConfiguration(): Boolean {
        val posUrl = prefs.getString(KEY_POS_URL, "")
        val customerUrl = prefs.getString(KEY_CUSTOMER_URL, "")
        return posUrl.isNullOrEmpty() || customerUrl.isNullOrEmpty()
    }

    private fun showSettingsUI() {
        val mainLayout = findViewById<FrameLayout>(R.id.primary_layout)
        mainLayout.removeAllViews()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Konfigurasi URL"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }

        val posLabel = TextView(this).apply {
            text = "URL Aplikasi POS (Layar Utama):"
            setPadding(0, 16, 0, 8)
        }

        val posInput = EditText(this).apply {
            hint = "Contoh: http://192.168.5.21/esb-fnb-pos/en/login"
            setText(prefs.getString(KEY_POS_URL, DEFAULT_POS_URL))
        }

        val customerLabel = TextView(this).apply {
            text = "URL Customer Display (Layar Kedua):"
            setPadding(0, 16, 0, 8)
        }

        val customerInput = EditText(this).apply {
            hint = "Contoh: http://192.168.5.21/esb-fnb-pos/en/customer-display"
            setText(prefs.getString(KEY_CUSTOMER_URL, DEFAULT_CUSTOMER_URL))
        }

        // Checkbox untuk menyembunyikan keyboard
        val hideKeyboardCheckBox = CheckBox(this).apply {
            text = "Sembunyikan Native Keyboard"
            isChecked = prefs.getBoolean(KEY_HIDE_KEYBOARD, false)
            setPadding(0, 24, 0, 0)
        }

        val saveButton = Button(this).apply {
            text = "Simpan & Mulai"
            setPadding(0, 32, 0, 0)
            setOnClickListener {
                saveConfiguration(
                    posInput.text.toString(),
                    customerInput.text.toString(),
                    hideKeyboardCheckBox.isChecked
                )
                recreate()
            }
        }

        val loadDefaultButton = Button(this).apply {
            text = "Gunakan Default"
            setPadding(0, 16, 0, 0)
            setOnClickListener {
                saveConfiguration(DEFAULT_POS_URL, DEFAULT_CUSTOMER_URL, hideKeyboardCheckBox.isChecked)
                recreate()
            }
        }

        container.addView(title)
        container.addView(posLabel)
        container.addView(posInput)
        container.addView(customerLabel)
        container.addView(customerInput)
        container.addView(hideKeyboardCheckBox)
        container.addView(saveButton)
        container.addView(loadDefaultButton)

        mainLayout.addView(container)
    }

    private fun saveConfiguration(posUrl: String, customerUrl: String, hideKeyboard: Boolean) {
        prefs.edit().apply {
            putString(KEY_POS_URL, posUrl)
            putString(KEY_CUSTOMER_URL, customerUrl)
            putBoolean(KEY_HIDE_KEYBOARD, hideKeyboard)
            apply()
        }
    }

    private fun getPosUrl(): String {
        return prefs.getString(KEY_POS_URL, DEFAULT_POS_URL) ?: DEFAULT_POS_URL
    }

    private fun getCustomerUrl(): String {
        return prefs.getString(KEY_CUSTOMER_URL, DEFAULT_CUSTOMER_URL) ?: DEFAULT_CUSTOMER_URL
    }

    private fun shouldHideKeyboard(): Boolean {
        return prefs.getBoolean(KEY_HIDE_KEYBOARD, false)
    }

    private fun setupDualScreen() {
        // Setup WebView di layar utama
        setupPrimaryWebView()

        // Setup layar kedua
        setupSecondaryDisplay()
    }

    private fun setupPrimaryWebView() {
        val primaryLayout = findViewById<FrameLayout>(R.id.primary_layout)

        primaryWebView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    url?.let {
                        // Handle esbprint:// custom URL scheme for printing
                        if (it.startsWith("esbprint://") || it.startsWith("intent://") || it.startsWith("market://")) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                                startActivity(intent)
                                return true
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "Unable to handle: $it", Toast.LENGTH_SHORT).show()
                                return true
                            }
                        }
                        // Handle other http/https URLs normally
                    }
                    return false
                }
            }
            
            webChromeClient = WebChromeClient()
            
            loadUrl(getPosUrl())
            setBackgroundColor(Color.WHITE)
            
            // Sembunyikan keyboard jika pengaturan diaktifkan
            if (shouldHideKeyboard()) {
                hideSystemUI()
            }
        }

        primaryLayout.addView(primaryWebView)
        
        // Check for pending print URL
        val pendingPrintUrl = prefs.getString("pending_print_url", null)
        if (!pendingPrintUrl.isNullOrEmpty()) {
            primaryWebView?.loadUrl(pendingPrintUrl)
            prefs.edit().remove("pending_print_url").apply()
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }

    private fun setupSecondaryDisplay() {
        val displays = displayManager.displays
        var secondaryDisplay: Display? = null

        for (display in displays) {
            if (display.displayId != Display.DEFAULT_DISPLAY) {
                secondaryDisplay = display
                break
            }
        }

        if (secondaryDisplay != null) {
            secondaryPresentation = SecondaryDisplayPresentation(this, secondaryDisplay)
            secondaryPresentation?.show()
        } else {
            val primaryLayout = findViewById<FrameLayout>(R.id.primary_layout)
            val fallback = TextView(this).apply {
                text = "⚠️ Tidak ada layar sekunder ditemukan.\n\nTekan tombol menu untuk konfigurasi URL."
                setTextColor(Color.RED)
                textSize = 14f
                setPadding(32, 32, 32, 32)
            }
            primaryLayout.addView(fallback)
        }
    }

    override fun onDestroy() {
        primaryWebView?.destroy()
        secondaryPresentation?.dismiss()
        super.onDestroy()
    }

    private inner class SecondaryDisplayPresentation(
        context: Context,
        display: Display
    ) : Presentation(context, display) {

        private var secondaryWebView: WebView? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.secondary_display)
            setupSecondaryWebView()
        }

        private fun setupSecondaryWebView() {
            val secondaryLayout = findViewById<FrameLayout>(R.id.secondary_layout)

            secondaryWebView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        url?.let {
                            // Handle esbprint:// custom URL scheme for printing
                            if (it.startsWith("esbprint://") || it.startsWith("intent://") || it.startsWith("market://")) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                                    startActivity(intent)
                                    return true
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Unable to handle: $it", Toast.LENGTH_SHORT).show()
                                    return true
                                }
                            }
                        }
                        return false
                    }
                }
                
                webChromeClient = WebChromeClient()
                loadUrl(getCustomerUrl())
                setBackgroundColor(Color.BLACK)
            }

            secondaryLayout.addView(secondaryWebView)
        }
    }
}
