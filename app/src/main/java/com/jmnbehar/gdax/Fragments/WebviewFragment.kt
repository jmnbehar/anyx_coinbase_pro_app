package com.jmnbehar.gdax.Fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_webview.view.*

/**
 * Created by jmnbehar on 11/5/2017.
 */

class WebviewFragment : RefreshFragment() {
    //    var webView: AdvancedWebView? = null
    var webView: WebView? = null
    companion object {
        var url: String? = null
        fun newInstance(url: String?): WebviewFragment {
            this.url = url
            return WebviewFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_webview, container, false)

        webView = rootView.web_view

        webView?.setBackgroundColor(Color.TRANSPARENT)
        webView?.settings?.loadsImagesAutomatically = true
        webView?.settings?.javaScriptEnabled = false
        webView?.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY

        webView?.loadUrl(url ?: "")

        return rootView
    }

}
