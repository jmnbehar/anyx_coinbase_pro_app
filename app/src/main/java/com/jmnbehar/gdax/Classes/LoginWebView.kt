package com.jmnbehar.gdax.Classes

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import java.lang.ref.WeakReference

/**
 * Created by josephbehar on 2/4/18.
 */

class LoginWebView : WebView {

    interface Listener {
        fun onPageStarted(url: String, favicon: Bitmap?)

        fun onPageFinished(url: String)

        fun onPageError(errorCode: Int, description: String, failingUrl: String)

        fun onDownloadRequested(url: String, suggestedFilename: String, mimeType: String, contentLength: Long, contentDisposition: String, userAgent: String)

        fun onExternalPageRequest(url: String)
    }

    lateinit var mActivity: WeakReference<Activity>

    private var mListener: Listener? = null
    private var mLastError: Long = 0
    private var mCustomWebViewClient: WebViewClient? = null
    private var mCustomWebChromeClient: WebChromeClient? = null

    val DATABASES_SUB_FOLDER = "/databases"


    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }


    protected fun hasError(): Boolean {
        return mLastError + 500 >= System.currentTimeMillis()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun init(context: Context) {
        // in IDE's preview mode
        if (isInEditMode) {
            // do not run the code from this method
            return
        }

//        if (context is Activity) {
//            mActivity = WeakReference(context)
//        }

//        mLanguageIso3 = LoginWebView.languageIso3

        isFocusable = true
        isFocusableInTouchMode = true

        isSaveEnabled = true

        val filesDir = context.filesDir.path
        val databaseDir = filesDir.substring(0, filesDir.lastIndexOf("/")) + DATABASES_SUB_FOLDER

        val webSettings = settings
        webSettings.allowFileAccess = false
//        LoginWebView.setAllowAccessFromFileUrls(webSettings, false)
        webSettings.builtInZoomControls = false
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        if (Build.VERSION.SDK_INT < 18) {
            webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        }
        webSettings.databaseEnabled = true
        if (Build.VERSION.SDK_INT < 19) {
            webSettings.databasePath = databaseDir
        }
        //setMixedContentAllowed(webSettings, true)

        //setThirdPartyCookiesEnabled(true)

        super.setWebViewClient(object : WebViewClient() {

//            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
//                if (!hasError()) {
//                    if (mListener != null) {
//                        mListener!!.onPageStarted(url, favicon)
//                    }
//                }
//
//                if (mCustomWebViewClient != null && favicon != null) {
//                    mCustomWebViewClient!!.onPageStarted(view, url, favicon)
//                }
//            }

            override fun onPageFinished(view: WebView, url: String) {
//                when (url) {
//                    "sdsdsds" ->
//                }
                if (!hasError()) {
                    if (mListener != null) {
                        mListener!!.onPageFinished(url)
                    }
                }

                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.onPageFinished(view, url)
                }
            }

//            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
//                setLastError()
//
//                if (mListener != null) {
//                    mListener!!.onPageError(errorCode, description, failingUrl)
//                }
//
//                if (mCustomWebViewClient != null) {
//                    mCustomWebViewClient!!.onReceivedError(view, errorCode, description, failingUrl)
//                }
//            }

//            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
//                // if the hostname may not be accessed
//                if (!isHostnameAllowed(url)) {
//                    // if a listener is available
//                    if (mListener != null) {
//                        // inform the listener about the request
//                        mListener!!.onExternalPageRequest(url)
//                    }
//
//                    // cancel the original request
//                    return true
//                }
//
//                // if there is a user-specified handler available
//                if (mCustomWebViewClient != null) {
//                    // if the user-specified handler asks to override the request
//                    if (mCustomWebViewClient!!.shouldOverrideUrlLoading(view, url)) {
//                        // cancel the original request
//                        return true
//                    }
//                }
//
//                // route the request through the custom URL loading method
//                view.loadUrl(url)
//
//                // cancel the original request
//                return true
//            }

            override fun onLoadResource(view: WebView, url: String) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.onLoadResource(view, url)
                } else {
                    super.onLoadResource(view, url)
                }
            }

            @SuppressLint("NewApi")
            override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
                if (Build.VERSION.SDK_INT >= 11) {
                    if (mCustomWebViewClient != null) {
                        return mCustomWebViewClient!!.shouldInterceptRequest(view, url)
                    } else {
                        return super.shouldInterceptRequest(view, url)
                    }
                } else {
                    return null
                }
            }

            @SuppressLint("NewApi")
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (Build.VERSION.SDK_INT >= 21) {
                    if (mCustomWebViewClient != null) {
                        return mCustomWebViewClient!!.shouldInterceptRequest(view, request)
                    } else {
                        return super.shouldInterceptRequest(view, request)
                    }
                } else {
                    return null
                }
            }

            override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.onFormResubmission(view, dontResend, resend)
                } else {
                    super.onFormResubmission(view, dontResend, resend)
                }
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.doUpdateVisitedHistory(view, url, isReload)
                } else {
                    super.doUpdateVisitedHistory(view, url, isReload)
                }
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.onReceivedSslError(view, handler, error)
                } else {
                    super.onReceivedSslError(view, handler, error)
                }
            }

            @SuppressLint("NewApi")
            override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
                if (Build.VERSION.SDK_INT >= 21) {
                    if (mCustomWebViewClient != null) {
                        mCustomWebViewClient!!.onReceivedClientCertRequest(view, request)
                    } else {
                        super.onReceivedClientCertRequest(view, request)
                    }
                }
            }

            override fun onReceivedHttpAuthRequest(view: WebView, handler: HttpAuthHandler, host: String, realm: String) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.onReceivedHttpAuthRequest(view, handler, host, realm)
                } else {
                    super.onReceivedHttpAuthRequest(view, handler, host, realm)
                }
            }

            override fun shouldOverrideKeyEvent(view: WebView, event: KeyEvent): Boolean {
                if (mCustomWebViewClient != null) {
                    return mCustomWebViewClient!!.shouldOverrideKeyEvent(view, event)
                } else {
                    return super.shouldOverrideKeyEvent(view, event)
                }
            }

            override fun onUnhandledKeyEvent(view: WebView, event: KeyEvent) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.onUnhandledKeyEvent(view, event)
                } else {
                    super.onUnhandledKeyEvent(view, event)
                }
            }

            override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.onScaleChanged(view, oldScale, newScale)
                } else {
                    super.onScaleChanged(view, oldScale, newScale)
                }
            }

            @SuppressLint("NewApi")
            override fun onReceivedLoginRequest(view: WebView, realm: String, account: String, args: String) {
                if (Build.VERSION.SDK_INT >= 12) {
                    if (mCustomWebViewClient != null) {
                        mCustomWebViewClient!!.onReceivedLoginRequest(view, realm, account, args)
                    } else {
                        super.onReceivedLoginRequest(view, realm, account, args)
                    }
                }
            }

        })

        super.setWebChromeClient(object : WebChromeClient() {

            // file upload callback (Android 2.2 (API level 8) -- Android 2.3 (API level 10)) (hidden method)
            fun openFileChooser(uploadMsg: ValueCallback<Uri>) {
                openFileChooser(uploadMsg, "")
            }

            // file upload callback (Android 3.0 (API level 11) -- Android 4.0 (API level 15)) (hidden method)
            fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String) {
                openFileChooser(uploadMsg, acceptType)
            }

//            // file upload callback (Android 4.1 (API level 16) -- Android 4.3 (API level 18)) (hidden method)
//            fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String, capture: String?) {
//                openFileInput(uploadMsg, null, false)
//            }

//            // file upload callback (Android 5.0 (API level 21) -- current) (public method)
//            override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
//                if (Build.VERSION.SDK_INT >= 21) {
//                    val allowMultiple = fileChooserParams.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE
//
//                    openFileInput(null, filePathCallback, allowMultiple)
//
//                    return true
//                } else {
//                    return false
//                }
//            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onProgressChanged(view, newProgress)
                } else {
                    super.onProgressChanged(view, newProgress)
                }
                view.url
            }

            override fun onReceivedTitle(view: WebView, title: String) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onReceivedTitle(view, title)
                } else {
                    super.onReceivedTitle(view, title)
                }
            }

            override fun onReceivedIcon(view: WebView, icon: Bitmap) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onReceivedIcon(view, icon)
                } else {
                    super.onReceivedIcon(view, icon)
                }
            }

            override fun onReceivedTouchIconUrl(view: WebView, url: String, precomposed: Boolean) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onReceivedTouchIconUrl(view, url, precomposed)
                } else {
                    super.onReceivedTouchIconUrl(view, url, precomposed)
                }
            }

            override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onShowCustomView(view, callback)
                } else {
                    super.onShowCustomView(view, callback)
                }
            }

            @SuppressLint("NewApi")
            override fun onShowCustomView(view: View, requestedOrientation: Int, callback: WebChromeClient.CustomViewCallback) {
                if (Build.VERSION.SDK_INT >= 14) {
                    if (mCustomWebChromeClient != null) {
                        mCustomWebChromeClient!!.onShowCustomView(view, requestedOrientation, callback)
                    } else {
                        super.onShowCustomView(view, requestedOrientation, callback)
                    }
                }
            }

            override fun onHideCustomView() {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onHideCustomView()
                } else {
                    super.onHideCustomView()
                }
            }

            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient!!.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
                } else {
                    return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
                }
            }

            override fun onRequestFocus(view: WebView) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onRequestFocus(view)
                } else {
                    super.onRequestFocus(view)
                }
            }

            override fun onCloseWindow(window: WebView) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onCloseWindow(window)
                } else {
                    super.onCloseWindow(window)
                }
            }

            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient!!.onJsAlert(view, url, message, result)
                } else {
                    return super.onJsAlert(view, url, message, result)
                }
            }

            override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient!!.onJsConfirm(view, url, message, result)
                } else {
                    return super.onJsConfirm(view, url, message, result)
                }
            }

            override fun onJsPrompt(view: WebView, url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient!!.onJsPrompt(view, url, message, defaultValue, result)
                } else {
                    return super.onJsPrompt(view, url, message, defaultValue, result)
                }
            }

            override fun onJsBeforeUnload(view: WebView, url: String, message: String, result: JsResult): Boolean {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient!!.onJsBeforeUnload(view, url, message, result)
                } else {
                    return super.onJsBeforeUnload(view, url, message, result)
                }
            }

//            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
//                if (mGeolocationEnabled) {
//                    callback.invoke(origin, true, false)
//                } else {
//                    if (mCustomWebChromeClient != null) {
//                        mCustomWebChromeClient!!.onGeolocationPermissionsShowPrompt(origin, callback)
//                    } else {
//                        super.onGeolocationPermissionsShowPrompt(origin, callback)
//                    }
//                }
//            }

            override fun onGeolocationPermissionsHidePrompt() {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onGeolocationPermissionsHidePrompt()
                } else {
                    super.onGeolocationPermissionsHidePrompt()
                }
            }

            @SuppressLint("NewApi")
            override fun onPermissionRequest(request: PermissionRequest) {
                if (Build.VERSION.SDK_INT >= 21) {
                    if (mCustomWebChromeClient != null) {
                        mCustomWebChromeClient!!.onPermissionRequest(request)
                    } else {
                        super.onPermissionRequest(request)
                    }
                }
            }

            @SuppressLint("NewApi")
            override fun onPermissionRequestCanceled(request: PermissionRequest) {
                if (Build.VERSION.SDK_INT >= 21) {
                    if (mCustomWebChromeClient != null) {
                        mCustomWebChromeClient!!.onPermissionRequestCanceled(request)
                    } else {
                        super.onPermissionRequestCanceled(request)
                    }
                }
            }

            override fun onJsTimeout(): Boolean {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient!!.onJsTimeout()
                } else {
                    return super.onJsTimeout()
                }
            }

            override fun onConsoleMessage(message: String, lineNumber: Int, sourceID: String) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onConsoleMessage(message, lineNumber, sourceID)
                } else {
                    super.onConsoleMessage(message, lineNumber, sourceID)
                }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient!!.onConsoleMessage(consoleMessage)
                } else {
                    return super.onConsoleMessage(consoleMessage)
                }
            }

            override fun getDefaultVideoPoster(): Bitmap {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient!!.defaultVideoPoster
                } else {
                    return super.getDefaultVideoPoster()
                }
            }

            override fun getVideoLoadingProgressView(): View {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient!!.videoLoadingProgressView
                } else {
                    return super.getVideoLoadingProgressView()
                }
            }

            override fun getVisitedHistory(callback: ValueCallback<Array<String>>) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.getVisitedHistory(callback)
                } else {
                    super.getVisitedHistory(callback)
                }
            }

            override fun onExceededDatabaseQuota(url: String, databaseIdentifier: String, quota: Long, estimatedDatabaseSize: Long, totalQuota: Long, quotaUpdater: WebStorage.QuotaUpdater) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater)
                } else {
                    super.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater)
                }
            }

            override fun onReachedMaxAppCacheSize(requiredStorage: Long, quota: Long, quotaUpdater: WebStorage.QuotaUpdater) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater)
                } else {
                    super.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater)
                }
            }

        })

        setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val suggestedFilename = URLUtil.guessFileName(url, contentDisposition, mimeType)

            if (mListener != null) {
                mListener!!.onDownloadRequested(url, suggestedFilename, mimeType, contentLength, contentDisposition, userAgent)
            }
        }
    }
}