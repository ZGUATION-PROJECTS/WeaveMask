package io.github.seyud.weave.ui.webui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import io.github.seyud.weave.core.Const
import io.github.seyud.weave.core.utils.RootUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
internal suspend fun prepareWebView(
    activity: Activity,
    moduleId: String,
    webUIState: WebUIState,
) {
    withContext(Dispatchers.IO) {
        val modDir = "${Const.MODULE_PATH}/${moduleId}"
        webUIState.modDir = modDir

        val shell = com.topjohnwu.superuser.Shell.Builder.create()
            .setFlags(com.topjohnwu.superuser.Shell.FLAG_MOUNT_MASTER)
            .build("su")
        webUIState.rootShell = shell

        withContext(Dispatchers.Main) {
            val webView = WebView(activity)
            webView.setBackgroundColor(Color.TRANSPARENT)

            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
            }

            val webRoot = File("${modDir}/webroot")
            val webViewAssetLoader = WebViewAssetLoader.Builder()
                .setDomain("mui.kernelsu.org")
                .addPathHandler(
                    "/",
                    SuFilePathHandler(
                        activity,
                        webRoot,
                        RootUtils.fs,
                        { webUIState.currentInsets },
                        { enable ->
                            webUIState.isInsetsEnabled = enable
                            (activity as? WebUIActivity)?.enableEdgeToEdge(enable)
                        }
                    )
                )
                .build()

            webView.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    return webViewAssetLoader.shouldInterceptRequest(request.url)
                }

                override fun doUpdateVisitedHistory(
                    view: WebView?,
                    url: String?,
                    isReload: Boolean
                ) {
                    webUIState.webCanGoBack = view?.canGoBack() ?: false
                    if (webUIState.isInsetsEnabled) {
                        webUIState.webView?.evaluateJavascript(
                            webUIState.currentInsets.js,
                            null
                        )
                    }
                    super.doUpdateVisitedHistory(view, url, isReload)
                }
            }

            webView.webChromeClient = object : WebChromeClient() {
                override fun onJsAlert(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    result: JsResult?
                ): Boolean {
                    if (message == null || result == null) return false
                    webUIState.uiEvent = WebUIEvent.ShowAlert(message, result)
                    return true
                }

                override fun onJsConfirm(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    result: JsResult?
                ): Boolean {
                    if (message == null || result == null) return false
                    webUIState.uiEvent = WebUIEvent.ShowConfirm(message, result)
                    return true
                }

                override fun onJsPrompt(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    defaultValue: String?,
                    result: JsPromptResult?
                ): Boolean {
                    if (message == null || result == null || defaultValue == null) return false
                    webUIState.uiEvent = WebUIEvent.ShowPrompt(message, defaultValue, result)
                    return true
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    webUIState.filePathCallback?.onReceiveValue(null)
                    webUIState.filePathCallback = filePathCallback
                    val intent = fileChooserParams?.createIntent()
                        ?: Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                    if (fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    webUIState.uiEvent = WebUIEvent.ShowFileChooser(intent)
                    return true
                }
            }

            val webviewInterface = WebViewInterface(webUIState)
            webUIState.webView = webView
            webView.addJavascriptInterface(webviewInterface, "ksu")
            webUIState.uiEvent = WebUIEvent.WebViewReady
        }
    }
}
