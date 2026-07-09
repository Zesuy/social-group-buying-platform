package com.zesuy.groupshop;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebViewClient;

public class MainActivity extends BridgeActivity {

    @Override
    protected void load() {
        super.load();
        bridge.setWebViewClient(new AlipayBridgeWebViewClient());
    }

    private class AlipayBridgeWebViewClient extends BridgeWebViewClient {
        AlipayBridgeWebViewClient() {
            super(bridge);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (isAlipayUrl(url)) {
                return openExternalPaymentUrl(url);
            }
            if (isAlipayWebUrl(url)) {
                return false;
            }
            return super.shouldOverrideUrlLoading(view, request);
        }
    }

    private boolean isAlipayUrl(String url) {
        return url.startsWith("alipay")
                || url.startsWith("intent://");
    }

    private boolean isAlipayWebUrl(String url) {
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        return host != null && (host.endsWith("alipay.com")
                || host.endsWith("alipaydev.com")
                || host.endsWith("alipayobjects.com"));
    }

    private boolean openExternalPaymentUrl(String url) {
        try {
            Intent intent;
            if (url.startsWith("intent://")) {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setComponent(null);
                intent.setSelector(null);
            } else {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            }
            startActivity(intent);
            return true;
        } catch (Exception firstError) {
            return openIntentFallbackUrl(url);
        }
    }

    private boolean openIntentFallbackUrl(String url) {
        if (!url.startsWith("intent://")) {
            return false;
        }
        try {
            Intent parsed = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            String fallbackUrl = parsed.getStringExtra("browser_fallback_url");
            if (fallbackUrl == null || fallbackUrl.trim().isEmpty()) {
                return true;
            }
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)));
            return true;
        } catch (ActivityNotFoundException ignored) {
            return true;
        } catch (Exception ignored) {
            return true;
        }
    }
}
