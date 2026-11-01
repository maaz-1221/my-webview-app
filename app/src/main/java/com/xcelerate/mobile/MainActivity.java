package com.xcelerate.mobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Typeface;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    private LinearLayout dashboardLayout;
    private LinearLayout webContainerLayout;
    private TextView webHeaderTitle;
    private WebView xworkingWebView;
    private WebView twitterWebView;
    private TextView logTextView;
    private EditText boardUrlEditText;
    private EditText limitEditText;
    private Button startButton;
    private Button stopButton;

    private Button tabDashboardBtn;
    private Button tabXworkingBtn;
    private Button tabTwitterBtn;

    // ── Live Telemetry Status HUD Cards ──
    private TextView txtTaskCount;
    private TextView txtTimer;
    private TextView txtSound;

    private boolean isRunning = false;
    private int completedTasks = 0;
    private int maxTasks = 8;
    private String engineJs = "";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int currentTab = 0; // 0 = Dashboard, 1 = xworking, 2 = twitter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        loadEngineScript();
        setupUI();
        initWebViews();
    }

    private void loadEngineScript() {
        try {
            InputStream is = getAssets().open("mobile_engine.js");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            engineJs = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0B0E14);
        root.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));

        // ── TOP NAVIGATION TABS ──
        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setBackgroundColor(0xFF151922);
        tabRow.setPadding(8, 28, 8, 8);

        tabDashboardBtn = new Button(this);
        tabDashboardBtn.setText("⚡ Dashboard");
        tabDashboardBtn.setTextSize(11);
        tabDashboardBtn.setTypeface(Typeface.DEFAULT_BOLD);
        tabDashboardBtn.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        tabXworkingBtn = new Button(this);
        tabXworkingBtn.setText("🌐 xworking Login");
        tabXworkingBtn.setTextSize(11);
        tabXworkingBtn.setTypeface(Typeface.DEFAULT_BOLD);
        tabXworkingBtn.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        tabTwitterBtn = new Button(this);
        tabTwitterBtn.setText("🐦 X Login");
        tabTwitterBtn.setTextSize(11);
        tabTwitterBtn.setTypeface(Typeface.DEFAULT_BOLD);
        tabTwitterBtn.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        tabRow.addView(tabDashboardBtn);
        tabRow.addView(tabXworkingBtn);
        tabRow.addView(tabTwitterBtn);
        root.addView(tabRow);

        // ── DASHBOARD CONTAINER ──
        dashboardLayout = new LinearLayout(this);
        dashboardLayout.setOrientation(LinearLayout.VERTICAL);
        dashboardLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        dashboardLayout.setPadding(16, 12, 16, 12);

        // Header Title
        TextView appTitle = new TextView(this);
        appTitle.setText("⚡ MAAZ AUTO XCELERATE");
        appTitle.setTextSize(18);
        appTitle.setTextColor(0xFF00E5FF);
        appTitle.setTypeface(Typeface.DEFAULT_BOLD);
        appTitle.setPadding(0, 0, 0, 2);
        dashboardLayout.addView(appTitle);

        TextView appSub = new TextView(this);
        appSub.setText("100% Video Watch (16s Sound ON) • Safe Boost • Auto-Pilot");
        appSub.setTextSize(10);
        appSub.setTextColor(0xFF8B98A5);
        appSub.setPadding(0, 0, 0, 10);
        dashboardLayout.addView(appSub);

        // ── HUD METRICS ROW ──
        LinearLayout hudRow = new LinearLayout(this);
        hudRow.setOrientation(LinearLayout.HORIZONTAL);
        hudRow.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        hudRow.setPadding(0, 0, 0, 12);

        LinearLayout card1 = createHudCard("TASKS DONE", "0 / 8", 0xFF00E5FF);
        LinearLayout card2 = createHudCard("WATCH TIMER", "0s / 16s", 0xFFFFAB00);
        LinearLayout card3 = createHudCard("SOUND STATE", "STANDBY", 0xFF8B98A5);

        card1.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        card2.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        card3.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        hudRow.addView(card1);
        hudRow.addView(card2);
        hudRow.addView(card3);
        dashboardLayout.addView(hudRow);

        txtTaskCount = (TextView) card1.getChildAt(1);
        txtTimer = (TextView) card2.getChildAt(1);
        txtSound = (TextView) card3.getChildAt(1);

        // Inputs
        TextView lblBoard = new TextView(this);
        lblBoard.setText("Board URL:");
        lblBoard.setTextColor(0xFF8B98A5);
        lblBoard.setTextSize(10);
        dashboardLayout.addView(lblBoard);

        boardUrlEditText = new EditText(this);
        boardUrlEditText.setText("https://xworking.net/#board/C");
        boardUrlEditText.setTextColor(0xFFFFFFFF);
        boardUrlEditText.setBackgroundColor(0xFF151922);
        boardUrlEditText.setTextSize(12);
        boardUrlEditText.setPadding(10, 8, 10, 8);
        dashboardLayout.addView(boardUrlEditText);

        TextView lblLimit = new TextView(this);
        lblLimit.setText("Task Limit:");
        lblLimit.setTextColor(0xFF8B98A5);
        lblLimit.setTextSize(10);
        lblLimit.setPadding(0, 6, 0, 0);
        dashboardLayout.addView(lblLimit);

        limitEditText = new EditText(this);
        limitEditText.setText("8");
        limitEditText.setTextColor(0xFFFFFFFF);
        limitEditText.setBackgroundColor(0xFF151922);
        limitEditText.setTextSize(12);
        limitEditText.setPadding(10, 8, 10, 8);
        dashboardLayout.addView(limitEditText);

        // Control Buttons
        startButton = new Button(this);
        startButton.setText("▶  START AUTO-PILOT");
        startButton.setBackgroundColor(0xFF00E676);
        startButton.setTextColor(0xFF0B0E14);
        startButton.setTypeface(Typeface.DEFAULT_BOLD);
        dashboardLayout.addView(startButton);

        stopButton = new Button(this);
        stopButton.setText("⏹  STOP ENGINE");
        stopButton.setBackgroundColor(0xFFF4212E);
        stopButton.setTextColor(0xFFFFFFFF);
        stopButton.setVisibility(View.GONE);
        dashboardLayout.addView(stopButton);

        // Live Scrolling Log Terminal
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        scroll.setBackgroundColor(0xFF05070A);
        scroll.setPadding(8, 8, 8, 8);

        logTextView = new TextView(this);
        logTextView.setText("⚡ System Ready. Authenticate in tabs above, then Start.\n");
        logTextView.setTextColor(0xFF00E5FF);
        logTextView.setTextSize(10);
        logTextView.setTypeface(Typeface.MONOSPACE);
        scroll.addView(logTextView);

        dashboardLayout.addView(scroll);
        root.addView(dashboardLayout);

        // ── WEB CONTAINER FOR LOGINS ──
        webContainerLayout = new LinearLayout(this);
        webContainerLayout.setOrientation(LinearLayout.VERTICAL);
        webContainerLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        webContainerLayout.setVisibility(View.GONE);

        // Browser Top Action Bar
        LinearLayout browserBar = new LinearLayout(this);
        browserBar.setOrientation(LinearLayout.HORIZONTAL);
        browserBar.setBackgroundColor(0xFF10141D);
        browserBar.setPadding(12, 8, 12, 8);

        Button btnBackToDash = new Button(this);
        btnBackToDash.setText("🏠 Dashboard");
        btnBackToDash.setTextSize(11);
        btnBackToDash.setBackgroundColor(0xFF1C2230);
        btnBackToDash.setTextColor(0xFF00E5FF);
        btnBackToDash.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        browserBar.addView(btnBackToDash);

        webHeaderTitle = new TextView(this);
        webHeaderTitle.setText("Browser");
        webHeaderTitle.setTextColor(0xFFFFFFFF);
        webHeaderTitle.setTextSize(12);
        webHeaderTitle.setPadding(16, 0, 0, 0);
        webHeaderTitle.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        browserBar.addView(webHeaderTitle);

        Button btnReload = new Button(this);
        btnReload.setText("🔄 Reload");
        btnReload.setTextSize(11);
        btnReload.setBackgroundColor(0xFF1C2230);
        btnReload.setTextColor(0xFFFFFFFF);
        btnReload.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        browserBar.addView(btnReload);

        webContainerLayout.addView(browserBar);

        // WebViews
        xworkingWebView = new WebView(this);
        xworkingWebView.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        webContainerLayout.addView(xworkingWebView);

        twitterWebView = new WebView(this);
        twitterWebView.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        webContainerLayout.addView(twitterWebView);

        root.addView(webContainerLayout);

        setContentView(root);

        // Listeners
        tabDashboardBtn.setOnClickListener(v -> showTab(0));
        tabXworkingBtn.setOnClickListener(v -> showTab(1));
        tabTwitterBtn.setOnClickListener(v -> showTab(2));

        btnBackToDash.setOnClickListener(v -> showTab(0));
        btnReload.setOnClickListener(v -> {
            if (currentTab == 1) xworkingWebView.reload();
            else if (currentTab == 2) twitterWebView.reload();
        });

        startButton.setOnClickListener(v -> startAutoPilot());
        stopButton.setOnClickListener(v -> stopAutoPilot());

        updateTabColors();
    }

    private LinearLayout createHudCard(String labelText, String valText, int valColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFF151922);
        card.setPadding(10, 10, 10, 10);

        TextView lbl = new TextView(this);
        lbl.setText(labelText);
        lbl.setTextSize(9);
        lbl.setTextColor(0xFF8B98A5);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(lbl);

        TextView val = new TextView(this);
        val.setText(valText);
        val.setTextSize(12);
        val.setTextColor(valColor);
        val.setTypeface(Typeface.DEFAULT_BOLD);
        val.setPadding(0, 4, 0, 0);
        card.addView(val);

        return card;
    }

    private void showTab(int tabIndex) {
        currentTab = tabIndex;
        updateTabColors();

        if (tabIndex == 0) {
            dashboardLayout.setVisibility(View.VISIBLE);
            webContainerLayout.setVisibility(View.GONE);
            xworkingWebView.setVisibility(View.GONE);
            twitterWebView.setVisibility(View.GONE);
        } else if (tabIndex == 1) {
            dashboardLayout.setVisibility(View.GONE);
            webContainerLayout.setVisibility(View.VISIBLE);
            webHeaderTitle.setText("🌐 xworking.net Login");
            xworkingWebView.setVisibility(View.VISIBLE);
            twitterWebView.setVisibility(View.GONE);
            if (xworkingWebView.getUrl() == null || xworkingWebView.getUrl().isEmpty()) {
                xworkingWebView.loadUrl("https://xworking.net");
            }
        } else if (tabIndex == 2) {
            dashboardLayout.setVisibility(View.GONE);
            webContainerLayout.setVisibility(View.VISIBLE);
            webHeaderTitle.setText("🐦 X (Twitter) Login");
            xworkingWebView.setVisibility(View.GONE);
            twitterWebView.setVisibility(View.VISIBLE);
            if (twitterWebView.getUrl() == null || twitterWebView.getUrl().isEmpty()) {
                twitterWebView.loadUrl("https://x.com/login");
            }
        }
    }

    private void updateTabColors() {
        tabDashboardBtn.setBackgroundColor(currentTab == 0 ? 0xFF00E5FF : 0xFF1C2230);
        tabDashboardBtn.setTextColor(currentTab == 0 ? 0xFF0B0E14 : 0xFF8B98A5);

        tabXworkingBtn.setBackgroundColor(currentTab == 1 ? 0xFF00E5FF : 0xFF1C2230);
        tabXworkingBtn.setTextColor(currentTab == 1 ? 0xFF0B0E14 : 0xFF8B98A5);

        tabTwitterBtn.setBackgroundColor(currentTab == 2 ? 0xFF00E5FF : 0xFF1C2230);
        tabTwitterBtn.setTextColor(currentTab == 2 ? 0xFF0B0E14 : 0xFF8B98A5);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViews() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        configureWebView(xworkingWebView);
        configureWebView(twitterWebView);

        cookieManager.setAcceptThirdPartyCookies(xworkingWebView, true);
        cookieManager.setAcceptThirdPartyCookies(twitterWebView, true);

        xworkingWebView.addJavascriptInterface(new Bridge(), "AndroidBridge");
        twitterWebView.addJavascriptInterface(new Bridge(), "AndroidBridge");

        // ── XWORKING WEBVIEW CLIENT ──
        xworkingWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false; // Load inside WebView
                }
                return true;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed(); // Prevent SSL handshake drops on cellular networks
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
                if (isRunning && url != null && url.contains("xworking.net")) {
                    view.evaluateJavascript(engineJs, null);
                    mainHandler.postDelayed(() -> view.evaluateJavascript("if(window.__xcel_pick_task) window.__xcel_pick_task();", null), 2500);
                }
            }
        });

        // ── TWITTER WEBVIEW CLIENT (Robust Zero-White-Screen Handler) ──
        twitterWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false; // Tells WebView to handle and render the redirect internally
                }
                return true;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed(); // Always proceed on Twitter CDN asset handshakes
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
                if (isRunning && url != null && (url.contains("twitter.com") || url.contains("x.com"))) {
                    view.evaluateJavascript(engineJs, null);
                }
            }
        });
    }

    private void configureWebView(WebView wv) {
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Standard official Chrome Mobile user agent
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36");

        // Handle popup/child auth windows directly inside the same WebView
        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(view);
                resultMsg.sendToTarget();
                return true;
            }
        });
    }

    private void startAutoPilot() {
        isRunning = true;
        completedTasks = 0;
        try {
            maxTasks = Integer.parseInt(limitEditText.getText().toString().trim());
        } catch (Exception e) {
            maxTasks = 8;
        }

        String boardUrl = boardUrlEditText.getText().toString().trim();
        if (boardUrl.isEmpty()) boardUrl = "https://xworking.net/#board/C";

        startButton.setVisibility(View.GONE);
        stopButton.setVisibility(View.VISIBLE);

        txtTaskCount.setText("0 / " + maxTasks);
        txtTimer.setText("Starting...");
        txtSound.setText("INITIALIZING");
        txtSound.setTextColor(0xFF00E5FF);

        appendLog("🚀 Auto-Pilot Starting on: " + boardUrl);
        showTab(0);
        xworkingWebView.loadUrl(boardUrl);
    }

    private void stopAutoPilot() {
        isRunning = false;
        startButton.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.GONE);
        txtSound.setText("STOPPED");
        txtSound.setTextColor(0xFF8B98A5);
        appendLog("⏹️ Auto-Pilot Stopped by user.");
    }

    private void appendLog(String msg) {
        mainHandler.post(() -> {
            logTextView.append(msg + "\n");
        });
    }

    @Override
    public void onBackPressed() {
        if (currentTab == 1 && xworkingWebView.canGoBack()) {
            xworkingWebView.goBack();
        } else if (currentTab == 2 && twitterWebView.canGoBack()) {
            twitterWebView.goBack();
        } else if (currentTab != 0) {
            showTab(0);
        } else {
            super.onBackPressed();
        }
    }

    // Bridge for JavaScript -> Android callbacks
    public class Bridge {
        @JavascriptInterface
        public void onLog(String type, String text) {
            appendLog(text);
        }

        @JavascriptInterface
        public void onVideoTick(int sec, int totalSec, boolean isSoundOn) {
            mainHandler.post(() -> {
                txtTimer.setText(sec + "s / " + totalSec + "s");
                txtTimer.setTextColor(0xFFFFAB00);

                if (isSoundOn) {
                    txtSound.setText("🔊 ON (20%)");
                    txtSound.setTextColor(0xFF00E676);
                } else {
                    txtSound.setText("🔇 Muted");
                    txtSound.setTextColor(0xFFF4212E);
                }
            });
        }

        @JavascriptInterface
        public void onTaskUrlFound(String url, String handle) {
            appendLog("🔗 Opening Twitter task for @" + handle + "...");
            mainHandler.post(() -> {
                txtTimer.setText("Loading...");
                txtSound.setText("CONNECTING");
                txtSound.setTextColor(0xFF00E5FF);
                twitterWebView.loadUrl(url);
            });
        }

        @JavascriptInterface
        public void onTwitterCompleted() {
            appendLog("✅ Twitter actions done. Submitting on xworking.net...");
            mainHandler.post(() -> {
                txtTimer.setText("16s / 16s");
                txtTimer.setTextColor(0xFF00E676);
                txtSound.setText("SUBMITTING");
                txtSound.setTextColor(0xFF00E5FF);
                xworkingWebView.evaluateJavascript("if(window.__xcel_submit_task) window.__xcel_submit_task();", null);
            });
        }

        @JavascriptInterface
        public void onTaskSubmitted() {
            completedTasks++;
            mainHandler.post(() -> {
                txtTaskCount.setText(completedTasks + " / " + maxTasks);
                txtTimer.setText("16s / 16s");
                txtTimer.setTextColor(0xFF00E676);
                txtSound.setText("VERIFIED ✅");
                txtSound.setTextColor(0xFF00E676);
            });

            appendLog("🎉 Task (" + completedTasks + "/" + maxTasks + ") Complete!");
            if (completedTasks >= maxTasks || !isRunning) {
                appendLog("🏁 All " + maxTasks + " tasks finished!");
                mainHandler.post(() -> stopAutoPilot());
            } else {
                appendLog("⏳ 4s rest before next task...");
                mainHandler.postDelayed(() -> {
                    if (isRunning) {
                        xworkingWebView.evaluateJavascript("if(window.__xcel_pick_task) window.__xcel_pick_task();", null);
                    }
                }, 4000);
            }
        }

        @JavascriptInterface
        public void onNoTasksLeft() {
            appendLog("ℹ️ No more tasks left on this board.");
            mainHandler.post(() -> stopAutoPilot());
        }
    }
}
