package com.skyare.stackupacademy;

final class AuthConfig {
    private AuthConfig() {}
    static final String SUPABASE_URL = "https://mzlznwnxahixoqyspsdy.supabase.co";
    static String publishableKey() {
        return BuildConfig.SUPABASE_PUBLISHABLE_KEY;
    }
}
