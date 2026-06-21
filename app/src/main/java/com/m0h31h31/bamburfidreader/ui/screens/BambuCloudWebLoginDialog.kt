package com.m0h31h31.bamburfidreader.ui.screens

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.m0h31h31.bamburfidreader.R

/** Bambu 账号网页登录入口（用于完成人机验证 captcha）。 */
private const val BAMBU_WEB_LOGIN_URL_CN = "https://bambulab.cn/zh/sign-in"
private const val BAMBU_WEB_LOGIN_URL_GLOBAL = "https://bambulab.com/en/sign-in"

/** 中文环境使用国内登录页，其它使用国际登录页。 */
private fun resolveWebLoginUrl(context: android.content.Context): String {
    val language = androidx.core.os.ConfigurationCompat
        .getLocales(context.resources.configuration)
        .get(0)
        ?.language
        .orEmpty()
    return if (language.equals("zh", ignoreCase = true)) {
        BAMBU_WEB_LOGIN_URL_CN
    } else {
        BAMBU_WEB_LOGIN_URL_GLOBAL
    }
}

/**
 * 出现人机验证时引导用户在官方网页登录一次以通过验证。
 * 网页登录成功后（拦截到登录接口返回 accessToken）回调 [onVerified]，
 * 由调用方引导用户返回 App 重新登录。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BambuCloudWebLoginDialog(
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cloud_web_login_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(stringResource(R.string.action_close))
                    }
                }
                Text(
                    text = stringResource(R.string.cloud_web_login_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)
                )
                AndroidView(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            addJavascriptInterface(
                                object {
                                    @JavascriptInterface
                                    fun onLoginSuccess() {
                                        post { onVerified() }
                                    }
                                },
                                "BrrLogin"
                            )
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: android.graphics.Bitmap?
                                ) {
                                    super.onPageStarted(view, url, favicon)
                                    view?.evaluateJavascript(LOGIN_INTERCEPT_JS, null)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    view?.evaluateJavascript(LOGIN_INTERCEPT_JS, null)
                                }
                            }
                            loadUrl(resolveWebLoginUrl(ctx))
                        }
                    }
                )
                androidx.compose.material3.Button(
                    onClick = onVerified,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(stringResource(R.string.cloud_web_login_done))
                }
            }
        }
    }
}

private const val LOGIN_INTERCEPT_JS = """
(function(){
  if (window.__brrHooked) return; window.__brrHooked = true;
  function maybeSuccess(url, text){
    try {
      if (url && url.indexOf('user/login') !== -1 && text && text.indexOf('accessToken') !== -1) {
        BrrLogin.onLoginSuccess();
      }
    } catch(e){}
  }
  try {
    var of = window.fetch;
    if (of) {
      window.fetch = function(){
        var args = arguments;
        var url = (args[0] && args[0].url) ? args[0].url : ('' + args[0]);
        return of.apply(this, args).then(function(resp){
          try { resp.clone().text().then(function(t){ maybeSuccess(url, t); }); } catch(e){}
          return resp;
        });
      };
    }
  } catch(e){}
  try {
    var oOpen = XMLHttpRequest.prototype.open;
    var oSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.open = function(m, u){ this.__brrUrl = u; return oOpen.apply(this, arguments); };
    XMLHttpRequest.prototype.send = function(){
      var self = this;
      this.addEventListener('load', function(){
        try { maybeSuccess(self.__brrUrl, self.responseText); } catch(e){}
      });
      return oSend.apply(this, arguments);
    };
  } catch(e){}
})();
"""
