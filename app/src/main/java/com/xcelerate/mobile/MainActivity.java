package com.xcelerate.mobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    private WebView xworkingWebView;
    private WebView twitterWebView;
    private TextView logTextView;
    private EditText boardUrlEditText;
    private EditText limitEditText;
    private Button startButton;
    private Button stopButton;

    private boolean isRunning = false;
    private int completedTasks = 0;
    private int maxTasks = 8;
    private String engineJs = "";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
        // Simple programmatic UI for instant portability
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0B0E14);
        root.setPadding(24, 40, 24, 24);

        TextView title = new TextView(this);
        title.setText("⚡ XCELERATE MOBILE BOOSTER");
        title.setTextSize(18);
        title.setTextColor(0xFF00E5FF);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, 16);
        root.addView(title);

        TextView boardLabel = new TextView(this);
        boardLabel.setText("COMMUNITY BOARD URL:");
        boardLabel.setTextSize(11);
        boardLabel.setTextColor(0xFF8B98A5);
        root.addView(boardLabel);

        boardUrlEditText = new EditText(this);
        boardUrlEditText.setText("https://xworking.net/#board/C");
        boardUrlEditText.setTextColor(0xFFFFFFFF);
        boardUrlEditText.setBackgroundColor(0xFF151922);
        boardUrlEditText.setPadding(16, 16, 16, 16);
        root.addView(boardUrlEditText);

        TextView limitLabel = new TextView(this);
        limitLabel.setText("TASK LIMIT:");
        limitLabel.setTextSize(11);
        limitLabel.setTextColor(0xFF8B98A5);
        limitLabel.setPadding(0, 16, 0, 4);
        root.addView(limitLabel);

        limitEditText = new EditText(this);
        limitEditText.setText("8");
        limitEditText.setTextColor(0xFFFFFFFF);
        limitEditText.setBackgroundColor(0xFF151922);
        limitEditText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        limitEditText.setPadding(16, 16, 16, 16);
        root.addView(limitEditText);

        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 24, 0, 24);

        startButton = new Button(this);
        startButton.setText("START AUTO-PILOT");
        startButton.setBackgroundColor(0xFF00E5FF);
        startButton.setTextColor(0xFF0B0E14);
        startButton.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        startButton.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1f));
        btnRow.addView(startButton);

        stopButton = new Button(this);
        stopButton.setText("STOP");
        stopButton.setBackgroundColor(0xFFF4212E);
        stopButton.setTextColor(0xFFFFFFFF);
        stopButton.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        stopButton.setVisibility(View.GONE);
        stopButton.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1f));
        btnRow.addView(stopButton);

        root.addView(btnRow);

        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, 0, 1f));
        sv.setBackgroundColor(0xFF05070A);
        sv.setPadding(16, 16, 16, 16);

        logTextView = new TextView(this);
        logTextView.setText("[Ready] Xcelerate Mobile Booster Initialized.\n");
        logTextView.setTextColor(0xFF8B98A5);
        logTextView.setTextSize(12);
        logTextView.setTypeface(android.graphics.Typeface.MONOSPACE);
        sv.addView(logTextView);
        root.addView(sv);

        // Hidden WebViews for background processing
        xworkingWebView = new WebView(this);
        xworkingWebView.setVisibility(View.GONE);
        root.addView(xworkingWebView);

        twitterWebView = new WebView(this);
        twitterWebView.setVisibility(View.GONE);
        root.addView(twitterWebView);

        setContentView(root);

        startButton.setOnClickListener(v -> startAutoPilot());
        stopButton.setOnClickListener(v -> stopAutoPilot());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViews() {
        configureWebView(xworkingWebView);
        configureWebView(twitterWebView);

        xworkingWebView.addJavascriptInterface(new Bridge(), "AndroidBridge");
        twitterWebView.addJavascriptInterface(new Bridge(), "AndroidBridge");

        xworkingWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (isRunning && url != null && url.contains("xworking.net")) {
                    view.evaluateJavascript(engineJs, null);
                    mainHandler.postDelayed(() -> view.evaluateJavascript("if(window.__xcel_pick_task) window.__xcel_pick_task();", null), 2500);
                }
            }
        });

        twitterWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
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
