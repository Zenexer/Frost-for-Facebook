package com.pitchedapps.frost.fragments

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pitchedapps.frost.utils.L
import com.pitchedapps.frost.utils.putString
import com.pitchedapps.frost.web.FrostWebView
import com.pitchedapps.frost.web.FrostWebViewCore

/**
 * Created by Allan Wang on 2017-05-29.
 */


class WebFragment : BaseFragment() {

    companion object {
        private const val ARG_URL = "arg_url"
        fun newInstance(position: Int, url: String) = BaseFragment.newInstance(WebFragment(), position).putString(ARG_URL, url)
    }

    val refresh: SwipeRefreshLayout by lazy { frostWebView.refresh }
    val web: FrostWebViewCore by lazy { frostWebView.web }
    lateinit var url: String
    lateinit private var frostWebView: FrostWebView
    private var firstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        url = arguments.getString(ARG_URL)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        frostWebView = FrostWebView(context)
        frostWebView.position = position
        frostWebView.baseUrl = url
        return frostWebView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firstLoad()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        firstLoad()
    }

    fun firstLoad() {
        if (userVisibleHint && isVisible && firstLoad) {
            web.loadBaseUrl()
            firstLoad = false
        }
    }

    override fun onBackPressed() = frostWebView.onBackPressed()
}