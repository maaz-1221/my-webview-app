package com.xcelerate.mobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
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

        // ── TOP NAVIGATION TABS (Dashboard, xworking Login, Twitter Login) ──
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
        tabXworkingBtn.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.1f));

        tabTwitterBtn = new Button(this);
        tabTwitterBtn.setText("🐦 X Login");
        tabTwitterBtn.setTextSize(11);
        tabTwitterBtn.setTypeface(Typeface.DEFAULT_BOLD);
        tabTwitterBtn.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 0.9f));

        tabRow.addView(tabDashboardBtn);
        tabRow.addView(tabXworkingBtn);
        tabRow.addView(tabTwitterBtn);
        root.addView(tabRow);

        // ── 1. DASHBOARD VIEW ──
        dashboardLayout = new LinearLayout(this);
        dashboardLayout.setOrientation(LinearLayout.VERTICAL);
        dashboardLayout.setPadding(20, 16, 20, 20);
        dashboardLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));

        TextView title = new TextView(this);
        title.setText("⚡ XCELERATE MOBILE BOOSTER");
        title.setTextSize(16);
        title.setTextColor(0xFF00E5FF);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, 12);
        dashboardLayout.addView(title);

        TextView boardLabel = new TextView(this);
        boardLabel.setText("COMMUNITY BOARD URL:");
        boardLabel.setTextSize(11);
        boardLabel.setTextColor(0xFF8B98A5);
        dashboardLayout.addView(boardLabel);

        boardUrlEditText = new EditText(this);
        boardUrlEditText.setText("https://xworking.net/#board/C");
        boardUrlEditText.setTextColor(0xFFFFFFFF);
        boardUrlEditText.setBackgroundColor(0xFF151922);
        boardUrlEditText.setPadding(14, 12, 14, 12);
        dashboardLayout.addView(boardUrlEditText);

        TextView limitLabel = new TextView(this);
        limitLabel.setText("DAILY TASKS LIMIT:");
        limitLabel.setTextSize(11);
        limitLabel.setTextColor(0xFF8B98A5);
        limitLabel.setPadding(0, 12, 0, 4);
        dashboardLayout.addView(limitLabel);

        limitEditText = new EditText(this);
        limitEditText.setText("8");
        limitEditText.setTextColor(0xFFFFFFFF);
        limitEditText.setBackgroundColor(0xFF151922);
        limitEditText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        limitEditText.setPadding(14, 12, 14, 12);
        dashboardLayout.addView(limitEditText);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 16, 0, 16);

        startButton = new Button(this);
        startButton.setText("START AUTO-PILOT");
        startButton.setBackgroundColor(0xFF00E5FF);
        startButton.setTextColor(0xFF0B0E14);
        startButton.setTypeface(Typeface.DEFAULT_BOLD);
        startButton.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        btnRow.addView(startButton);

        stopButton = new Button(this);
        stopButton.setText("STOP");
        stopButton.setBackgroundColor(0xFFF4212E);
        stopButton.setTextColor(0xFFFFFFFF);
        stopButton.setTypeface(Typeface.DEFAULT_BOLD);
        stopButton.setVisibility(View.GONE);
        stopButton.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        btnRow.addView(stopButton);

        dashboardLayout.addView(btnRow);

        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        sv.setBackgroundColor(0xFF05070A);
        sv.setPadding(12, 12, 12, 12);

        logTextView = new TextView(this);
        logTextView.setText("[Ready] Pehli dafa chalane se pehle upar se 'xworking Login' aur 'X Login' karke apne accounts login karein.\nPhir 'Start Auto-Pilot' dabayein.\n");
        logTextView.setTextColor(0xFF8B98A5);
        logTextView.setTextSize(12);
        logTextView.setTypeface(Typeface.MONOSPACE);
        sv.addView(logTextView);
        dashboardLayout.addView(sv);

        root.addView(dashboardLayout);

        // ── 2. WEBVIEWS FOR LOGIN & BACKGROUND RUNNING ──
        xworkingWebView = new WebView(this);
        xworkingWebView.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        xworkingWebView.setVisibility(View.GONE);
        root.addView(xworkingWebView);

        twitterWebView = new WebView(this);
        twitterWebView.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        twitterWebView.setVisibility(View.GONE);
        root.addView(twitterWebView);

        setContentView(root);

        // Tab click listeners
        tabDashboardBtn.setOnClickListener(v -> showTab(0));
        tabXworkingBtn.setOnClickListener(v -> showTab(1));
        tabTwitterBtn.setOnClickListener(v -> showTab(2));

        startButton.setOnClickListener(v -> startAutoPilot());
        stopButton.setOnClickListener(v -> stopAutoPilot());

        updateTabColors();
    }

    private void showTab(int tabIndex) {
        currentTab = tabIndex;
        updateTabColors();

        if (tabIndex == 0) {
            dashboardLayout.setVisibility(View.VISIBLE);
            xworkingWebView.setVisibility(View.GONE);
            twitterWebView.setVisibility(View.GONE);
        } else if (tabIndex == 1) {
            dashboardLayout.setVisibility(View.GONE);
            xworkingWebView.setVisibility(View.VISIBLE);
            twitterWebView.setVisibility(View.GONE);
            if (xworkingWebView.getUrl() == null || xworkingWebView.getUrl().isEmpty()) {
                xworkingWebView.loadUrl("https://xworking.net");
            }
        } else if (tabIndex == 2) {
            dashboardLayout.setVisibility(View.GONE);
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

        xworkingWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
                if (isRunning && url != null && url.contains("xworking.net")) {
                    view.evaluateJavascript(engineJs, null);
                    mainHandler.postDelayed(() -> view.evaluateJavascript("if(window.__xcel_pick_task) window.__xcel_pick_task();", null), 2500);
                }
            }
        });

        twitterWebView.setWebViewClient(new WebViewClient() {
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
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36");
        wv.setWebChromeClient(new WebChromeClient());
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

        appendLog("🚀 Auto-Pilot Starting on: " + boardUrl);
        showTab(0); // Ensure dashboard is visible while running
        xworkingWebView.loadUrl(boardUrl);
    }

    private void stopAutoPilot() {
        isRunning = false;
        startButton.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.GONE);
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
        public void onTaskUrlFound(String url, String handle) {
            appendLog("🔗 Opening Twitter task for @" + handle + "...");
            mainHandler.post(() -> twitterWebView.loadUrl(url));
        }

        @JavascriptInterface
        public void onTwitterCompleted() {
            appendLog("✅ Twitter actions done. Submitting on xworking.net...");
            mainHandler.post(() -> {
                xworkingWebView.evaluateJavascript("if(window.__xcel_submit_task) window.__xcel_submit_task();", null);
            });
        }

        @JavascriptInterface
        public void onTaskSubmitted() {
            completedTasks++;
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
