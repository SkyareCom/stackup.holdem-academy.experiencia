package com.skyare.stackupholdemexperiencia;

import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.window.OnBackInvokedDispatcher;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import java.util.concurrent.Executor;

public class MainActivity extends FragmentActivity {
    private WebView webView;
    private static final String SUPABASE_URL = "https://mzlznwnxahixoqyspsdy.supabase.co";
    private static final String SUPABASE_KEY = "sb_publishable_E9cnM9HPU19f9hdFxzjXrg_FkD6clWQ";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        webView.addJavascriptInterface(new AndroidAuth(), "AndroidAuth");
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                () -> { if (webView.canGoBack()) webView.goBack(); else finish(); }
            );
        }
    }

    private void js(String function, String value) {
        String safe = value == null ? "" : value.replace("\\","\\\\").replace("'","\\'").replace("\n"," ");
        runOnUiThread(() -> webView.evaluateJavascript("window."+function+"('"+safe+"')", null));
    }

    public class AndroidAuth {
        @JavascriptInterface public void googleLogin() {
            try {
                String redirect = "stackupexperiencia://auth/callback";
                String authUrl = SUPABASE_URL + "/auth/v1/authorize?provider=google&redirect_to=" +
                    java.net.URLEncoder.encode(redirect, "UTF-8");
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
            } catch(Exception e) { js("authError","Não foi possível iniciar o login Google."); }
        }

        @JavascriptInterface public void stackupLogin(String email, String password) {
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(SUPABASE_URL + "/auth/v1/token?grant_type=password");
                    java.net.HttpURLConnection c = (java.net.HttpURLConnection) url.openConnection();
                    c.setRequestMethod("POST"); c.setDoOutput(true);
                    c.setRequestProperty("apikey", SUPABASE_KEY);
                    c.setRequestProperty("Content-Type", "application/json");
                    String body = "{\"email\":\""+email.replace("\\","\\\\").replace("\"","\\\"")+"\",\"password\":\""+password.replace("\\","\\\\").replace("\"","\\\"")+"\"}";
                    try(java.io.OutputStream os=c.getOutputStream()){os.write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
                    int status=c.getResponseCode();
                    if(status>=200 && status<300) {
                        java.io.InputStream in=c.getInputStream();
                        String json=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                        in.close();
                        org.json.JSONObject o=new org.json.JSONObject(json);
                        String refresh=o.optString("refresh_token","");
                        if(refresh.isEmpty()) js("authError","Sessão inválida. Tente novamente.");
                        else { getSharedPreferences("stackup_auth",MODE_PRIVATE).edit().putString("refresh_token",refresh).apply(); js("authSuccess",""); }
                    } else js("authError","StackUp ID ou senha inválidos.");
                    c.disconnect();
                } catch(Exception e){ js("authError","Não foi possível conectar ao STACKUP ID."); }
            }).start();
        }

        @JavascriptInterface public void biometricLogin() {
            runOnUiThread(() -> {
                BiometricManager manager = BiometricManager.from(MainActivity.this);
                int available = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
                if (available != BiometricManager.BIOMETRIC_SUCCESS) {
                    js("authError","Biometria não disponível neste aparelho."); return;
                }
                Executor executor = ContextCompat.getMainExecutor(MainActivity.this);
                BiometricPrompt prompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
                    @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        android.content.SharedPreferences p=getSharedPreferences("stackup_auth",MODE_PRIVATE);
                        String refresh=p.getString("refresh_token","");
                        if(refresh.isEmpty()){ js("authError","Entre primeiro com Google ou STACKUP ID para ativar a biometria."); return; }
                        refreshSession(refresh);
                    }
                    @Override public void onAuthenticationError(int code, CharSequence msg) { super.onAuthenticationError(code,msg); js("authError",msg.toString()); }
                });
                BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("STACKUP HOLD'EM")
                    .setSubtitle("Confirme sua identidade")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build();
                prompt.authenticate(info);
            });
        }
    }

    private void refreshSession(String refresh) {
        new Thread(() -> {
            try {
                java.net.URL url=new java.net.URL(SUPABASE_URL+"/auth/v1/token?grant_type=refresh_token");
                java.net.HttpURLConnection c=(java.net.HttpURLConnection)url.openConnection();
                c.setRequestMethod("POST"); c.setDoOutput(true);
                c.setRequestProperty("apikey",SUPABASE_KEY); c.setRequestProperty("Content-Type","application/json");
                org.json.JSONObject body=new org.json.JSONObject(); body.put("refresh_token",refresh);
                try(java.io.OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
                if(c.getResponseCode()>=200 && c.getResponseCode()<300){
                    String json=new String(c.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                    org.json.JSONObject o=new org.json.JSONObject(json);
                    String next=o.optString("refresh_token",refresh);
                    getSharedPreferences("stackup_auth",MODE_PRIVATE).edit().putString("refresh_token",next).apply();
                    js("authSuccess","");
                } else { getSharedPreferences("stackup_auth",MODE_PRIVATE).edit().clear().apply(); js("authError","Sessão expirada. Entre novamente com Google ou STACKUP ID."); }
                c.disconnect();
            } catch(Exception e){ js("authError","Não foi possível validar sua sessão."); }
        }).start();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleAuthCallback(intent);
    }

    @Override protected void onResume() {
        super.onResume();
        handleAuthCallback(getIntent());
    }

    private void handleAuthCallback(Intent intent) {
        if(intent==null || intent.getData()==null) return;
        Uri u=intent.getData();
        if(!"stackupexperiencia".equals(u.getScheme()) || !"auth".equals(u.getHost())) return;
        String fragment=u.getFragment();
        if(fragment==null) { js("authError","Retorno do Google sem sessão válida."); return; }
        try {
            java.util.Map<String,String> values=new java.util.HashMap<>();
            for(String part:fragment.split("&")){
                String[] kv=part.split("=",2);
                if(kv.length==2) values.put(java.net.URLDecoder.decode(kv[0],"UTF-8"),java.net.URLDecoder.decode(kv[1],"UTF-8"));
            }
            String refresh=values.get("refresh_token");
            if(refresh!=null && !refresh.isEmpty()){
                getSharedPreferences("stackup_auth",MODE_PRIVATE).edit().putString("refresh_token",refresh).apply();
                intent.setData(null);
                js("authSuccess","");
            } else js("authError","Não foi possível concluir a sessão Google.");
        } catch(Exception e){ js("authError","Não foi possível validar o retorno Google."); }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}