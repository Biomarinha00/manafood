package com.manafood.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String PREFS_NAME = "ManaFoodPrefs";
    private static final String KEY_SERVER_IP = "server_ip";
    private static final int PORT = 5000;
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private ValueCallback<Uri[]> fileChooserCallback;
    private boolean hasLoadedOnce = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        webView = findViewById(R.id.webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setDatabaseEnabled(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        webView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                hasLoadedOnce = true;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (failingUrl != null && failingUrl.contains("fonts.googleapis.com")) {
                    return;
                }
                if (failingUrl != null && failingUrl.contains(":" + PORT)) {
                    if (hasLoadedOnce) {
                        view.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
                        view.loadUrl(failingUrl);
                    } else {
                        view.loadData(
                            "<html><body style='font-family:sans-serif;text-align:center;padding:60px 20px;'>" +
                            "<h2 style='color:#e67e22'>Sem conexao</h2>" +
                            "<p style='color:#666;margin:16px 0'>Nao foi possivel conectar ao servidor.<br>" +
                            "Verifique se o PC esta ligado e na mesma rede WiFi.</p>" +
                            "<p style='color:#999;font-size:13px'>Toque nos 3 pontinhos > Mudar IP</p>" +
                            "</body></html>",
                            "text/html", "UTF-8"
                        );
                    }
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage msg) {
                Log.d("ManaFood", msg.sourceId() + ":" + msg.lineNumber() + " " + msg.message());
                return super.onConsoleMessage(msg);
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams params) {
                if (fileChooserCallback != null) {
                    fileChooserCallback.onReceiveValue(null);
                }
                fileChooserCallback = callback;
                Intent intent = params.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    fileChooserCallback = null;
                    Toast.makeText(MainActivity.this, "Nao foi possivel abrir o seletor de arquivos", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });

        setupConnectivityListener();

        clearCacheIfUpdated();

        String ip = getSavedIP();
        if (ip.isEmpty()) {
            showIPDialog(true);
        } else {
            loadServer(ip);
        }
    }

    private void clearCacheIfUpdated() {
        try {
            int currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            int lastVersion = prefs.getInt("last_version", 0);
            if (lastVersion != currentVersion) {
                webView.clearCache(true);
                prefs.edit().putInt("last_version", currentVersion).apply();
            }
        } catch (Exception e) { /* ignora */ }
    }

    private void setupConnectivityListener() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build();

        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                });
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> {
                    webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
                });
            }
        });
    }

    private String getSavedIP() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_SERVER_IP, "");
    }

    private void saveIP(String ip) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVER_IP, ip)
            .apply();
    }

    private void loadServer(String ip) {
        webView.loadUrl("http://" + ip + ":" + PORT);
    }

    private void showIPDialog(boolean firstTime) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Configurar servidor");

        String msg = firstTime
            ? "Digite o IP do PC onde o Mana Food esta rodando:"
            : "IP atual: " + getSavedIP() + "\n\nDigite o novo IP do servidor:";
        builder.setMessage(msg);

        final EditText input = new EditText(this);
        input.setHint("ex: 192.168.1.100");
        if (!firstTime) input.setText(getSavedIP());
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        input.setSelectAllOnFocus(true);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Conectar", (dialog, which) -> {
            String ip = input.getText().toString().trim();
            if (ip.isEmpty()) {
                Toast.makeText(this, "Digite o IP do servidor", Toast.LENGTH_SHORT).show();
                showIPDialog(firstTime);
                return;
            }
            saveIP(ip);
            loadServer(ip);
        });

        if (!firstTime) {
            builder.setNegativeButton("Cancelar", null);
        }
        builder.setCancelable(!firstTime);
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Mudar IP do servidor");
        menu.add(0, 2, 0, "Recarregar pagina");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            showIPDialog(false);
            return true;
        } else if (item.getItemId() == 2) {
            webView.reload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST && fileChooserCallback != null) {
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                results = new Uri[]{data.getData()};
            }
            fileChooserCallback.onReceiveValue(results);
            fileChooserCallback = null;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
