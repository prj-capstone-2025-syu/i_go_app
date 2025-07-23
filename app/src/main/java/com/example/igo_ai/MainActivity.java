package com.example.igo_ai;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String TAG = "MainActivity";

    private ValueCallback<Uri[]> uploadMessage;
    private static final int REQUEST_SELECT_FILE = 100;
    private static final int PERMISSION_REQUEST_CODE = 200;

    // 뒤로가기 콜백
    private OnBackPressedCallback onBackPressedCallback;

    // FCM 토큰 업데이트를 받기 위한 BroadcastReceiver
    private BroadcastReceiver fcmTokenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.igo_ai.FCM_TOKEN_UPDATED".equals(intent.getAction())) {
                String token = intent.getStringExtra("fcm_token");
                if (token != null && webView != null) {
                    Log.d(TAG, "브로드캐스트로 FCM 토큰 수신: " + token);
                    sendTokenToWebView(token);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupImmersiveMode();

        setContentView(R.layout.activity_main);

        // BroadcastReceiver 등록 (Android 13+ 호환)
        IntentFilter filter = new IntentFilter("com.example.igo_ai.FCM_TOKEN_UPDATED");

        // Android 13 (API 33) 이상에서는 플래그 명시 필요
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // 앱 내부 브로드캐스트이므로 RECEIVER_NOT_EXPORTED 사용
            registerReceiver(fcmTokenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            // 기존 방식 (Android 12 이하)
            registerReceiver(fcmTokenReceiver, filter);
        }

        // 뒤로가기 콜백 설정
        setupOnBackPressedCallback();

        // 권한 확인
        checkPermissions();

        webView = findViewById(R.id.webview);

        setupWebView();

        // Firebase 토큰 가져오기 (WebView 설정 후)
        getFirebaseToken();

        // 실제 EC2 도메인
        webView.loadUrl("https://igo.ai.kr");
    }

    private void setupImmersiveMode() {
        // 테마에서 기본 설정을 했으므로 추가적인 Immersive Sticky 모드만 적용
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11 이상
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    // 상태바와 네비게이션 바 숨기기
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());

                    // Immersive Sticky 모드 - 스와이프하면 일시적으로 나타남
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                // Android 10 이하 - SystemUiVisibility 사용
                int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

                getWindow().getDecorView().setSystemUiVisibility(uiOptions);
            }

            Log.d(TAG, "Immersive Mode 설정 완료 (테마 기반)");
        } catch (Exception e) {
            Log.w(TAG, "Immersive Mode 설정 중 오류 발생: " + e.getMessage());
        }
    }

    // 시스템 UI 가시성 변경 감지 (Android 10 이하용)
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // 포커스를 다시 얻었을 때 Immersive Mode 재적용
            setupImmersiveMode();
        }
    }

    private void setupOnBackPressedCallback() {
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    // 앱 종료 확인 다이얼로그 표시
                    showExitConfirmDialog();
                }
            }
        };

        // 콜백 등록
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    private void showExitConfirmDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("앱 종료")
                .setMessage("IGO를 종료하시겠습니까?")
                .setPositiveButton("네", (dialog, which) -> {
                    // 앱 종료
                    finish();
                })
                .setNegativeButton("아니요", (dialog, which) -> {
                    // 다이얼로그만 닫기
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    private void sendTokenToWebView(String token) {
        if (webView != null) {
            webView.post(() -> {
                // 여러 방법으로 토큰 전달 시도
                String script = String.format(
                        // 방법 1: setFCMToken 함수 호출
                        "if(typeof window.setFCMToken === 'function') { " +
                                "  window.setFCMToken('%s'); " +
                                "  console.log('토큰이 setFCMToken으로 전달됨'); " +
                                "} else if(typeof window.updateFCMToken === 'function') { " +
                                "  window.updateFCMToken('%s'); " +
                                "  console.log('토큰이 updateFCMToken으로 전달됨'); " +
                                "} else { " +
                                // 방법 2: localStorage에 토큰 저장
                                "  localStorage.setItem('fcm_token', '%s'); " +
                                "  console.log('토큰이 localStorage에 저장됨: %s'); " +
                                // 방법 3: Custom Event 발생
                                "  var event = new CustomEvent('fcmTokenReceived', { detail: '%s' }); " +
                                "  window.dispatchEvent(event); " +
                                "  console.log('fcmTokenReceived 이벤트 발생'); " +
                                // 방법 4: 전역 변수로 설정
                                "  window.ANDROID_FCM_TOKEN = '%s'; " +
                                "  console.log('전역 변수 ANDROID_FCM_TOKEN 설정 완료'); " +
                                "}",
                        token, token, token, token, token, token);

                webView.evaluateJavascript(script, result -> {
                    Log.d(TAG, "FCM 토큰 전달 스크립트 실행 결과: " + result);
                });
            });
        }
    }

    private void getFirebaseToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "토큰 가져오기 실패", task.getException());
                        return;
                    }

                    // 토큰 획득
                    String token = task.getResult();
                    Log.d(TAG, "FCM 토큰: " + token);

                    // 웹뷰로 토큰 전달
                    sendTokenToWebView(token);
                });
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 구글 소셜 로그인을 위한 User-Agent 설정 (403 disallowed_useragent 해결)
        String userAgent = webSettings.getUserAgentString();
        String newUserAgent = userAgent.replace("wv", "").replace("Version/4.0", "Chrome/94.0.4606.85");
        webSettings.setUserAgentString(newUserAgent);
        Log.d(TAG, "User-Agent 변경됨: " + newUserAgent);

        // 구글 OAuth를 위한 추가 설정
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        // JavaScript 인터페이스 추가
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 페이지 로드 완료 후 토큰 전달 재시도
                getFirebaseToken();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // OAuth 리다이렉트 URL 처리
                if (url.contains("oauth") || url.contains("accounts.google.com")) {
                    Log.d(TAG, "OAuth URL 감지: " + url);
                    view.loadUrl(url);
                    return false;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        // WebChromeClient 설정 (파일 업로드, 위치 정보 등)
        webView.setWebChromeClient(new WebChromeClient() {

            // 파일 선택 처리 (개선된 버전)
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                uploadMessage = filePathCallback;

                // FileChooserParams에서 제공하는 createIntent 사용 (권장)
                Intent intent;
                try {
                    intent = fileChooserParams.createIntent();
                } catch (Exception e) {
                    // Fallback to basic intent
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                }

                try {
                    startActivityForResult(Intent.createChooser(intent, "파일 선택"), REQUEST_SELECT_FILE);
                } catch (Exception e) {
                    uploadMessage = null;
                    Log.e(TAG, "파일 선택기 실행 실패", e);
                    return false;
                }
                return true;
            }

            // 위치 정보 권한 처리 (사용자 동의 추가)
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                                                           GeolocationPermissions.Callback callback) {
                // 실제 앱에서는 사용자에게 다이얼로그로 확인받는 것이 좋습니다
                // 현재는 자동 허용으로 설정
                callback.invoke(origin, true, false);
            }
        });
    }

    // JavaScript 인터페이스
    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void requestFCMToken() {
            // 메인 스레드에서 실행
            runOnUiThread(() -> getFirebaseToken());
        }
    }

    // 권한 확인 (Android 버전별 대응)
    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        // Android 13 이상에서는 알림 권한도 필요
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            String[] permissionsWithNotification = {
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            };
            permissions = permissionsWithNotification;
        }

        boolean needsPermission = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                needsPermission = true;
                break;
            }
        }

        if (needsPermission) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Log.d(TAG, "모든 권한이 허용되었습니다.");
            } else {
                Log.w(TAG, "일부 권한이 거부되었습니다. 일부 기능이 제한될 수 있습니다.");
            }
        }
    }

    // 파일 선택 결과 처리 (개선된 버전)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_SELECT_FILE) {
            if (uploadMessage == null) return;

            Uri[] results = null;

            if (resultCode == Activity.RESULT_OK && intent != null) {
                // 단일 파일 선택
                if (intent.getData() != null) {
                    results = new Uri[]{intent.getData()};
                }
                // 다중 파일 선택 (Android 18 이상)
                else if (intent.getClipData() != null) {
                    int count = intent.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = intent.getClipData().getItemAt(i).getUri();
                    }
                }
            }

            // 콜백 호출 (성공, 실패, 취소 모든 경우에 호출)
            uploadMessage.onReceiveValue(results);
            uploadMessage = null;
        }
    }

    // 액티비티 종료 시 정리
    @Override
    protected void onDestroy() {
        // OnBackPressedCallback 제거
        if (onBackPressedCallback != null) {
            onBackPressedCallback.remove();
        }

        // BroadcastReceiver 해제
        if (fcmTokenReceiver != null) {
            unregisterReceiver(fcmTokenReceiver);
        }

        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
