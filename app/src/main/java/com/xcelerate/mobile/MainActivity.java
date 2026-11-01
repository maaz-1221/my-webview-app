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
        title.setText("⚡ MAAZ AUTO XCELERATE");
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
        btnRow.setPadding(0, 14, 0, 14);

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

        // ── LIVE TELEMETRY STATUS HUD CARDS ──
        LinearLayout hudRow = new LinearLayout(this);
        hudRow.setOrientation(LinearLayout.HORIZONTAL);
        hudRow.setPadding(0, 0, 0, 12);

        LinearLayout taskCard = createHudCard("TASKS DONE", "0 / 8", 0xFF00E5FF);
        txtTaskCount = (TextView) taskCard.getChildAt(1);
        taskCard.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        hudRow.addView(taskCard);

        View sp1 = new View(this);
        sp1.setLayoutParams(new LinearLayout.LayoutParams(8, 1));
        hudRow.addView(sp1);

        LinearLayout timerCard = createHudCard("WATCH TIMER", "0s / 16s", 0xFFFFAB00);
        txtTimer = (TextView) timerCard.getChildAt(1);
        timerCard.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.1f));
        hudRow.addView(timerCard);

        View sp2 = new View(this);
        sp2.setLayoutParams(new LinearLayout.LayoutParams(8, 1));
        hudRow.addView(sp2);

        LinearLayout soundCard = createHudCard("SOUND STATE", "WAITING", 0xFF8B98A5);
        txtSound = (TextView) soundCard.getChildAt(1);
        soundCard.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.2f));
        hudRow.addView(soundCard);

        dashboardLayout.addView(hudRow);

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
                txtTimer.setTextColor(0xFFFFAB00); // Amber while watching

                if (isSoundOn) {
                    txtSound.setText("🔊 ON (20%)");
                    txtSound.setTextColor(0xFF00E676); // Glowing Emerald Green
                } else {
                    txtSound.setText("🔇 Muted");
                    txtSound.setTextColor(0xFFF4212E); // Red if muted
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
