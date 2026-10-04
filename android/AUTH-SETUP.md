# StackUp Academy — Production Authentication Setup

## Current implementation status — 30 Sep 2026

The Android/web hybrid currently uses Supabase Auth as the account backend for the implemented Google flow.

### Implemented now

- **Google:** Android Credential Manager obtains a Google ID token and `auth-production.js` exchanges it with Supabase Auth.
- **Biometrics:** Android BiometricPrompt unlocks an already authenticated Supabase session. The app never receives or stores fingerprint/face templates.
- **Academy Coach profile sync:** authenticated users can sync WhatsApp number, consent, frequency, limits and time zone to `public.profiles`.
- **Closed-test bypass:** `TEST_ACCESS=true` currently lets Play closed-test users enter the Academy without account creation.

### Not yet implemented end-to-end

- **WhatsApp OTP login:** still pending. Do not present it as a working production auth method until Supabase Phone Auth + Twilio/Twilio Verify are configured and the flow is wired into `auth-production.js`.
- **Google Play Billing:** plan UI exists, but closed-test billing is currently disabled.

## Supabase project

Android reads:

- `STACKUP_SUPABASE_URL`
- `STACKUP_SUPABASE_ANON_KEY`

The current Gradle build also contains a publishable fallback configuration for the Academy project.

Never place a Supabase service-role/secret key in the Android app or web assets.

## Google provider

In Supabase Dashboard:

Authentication -> Providers -> Google

The Android Web OAuth Client ID used for ID-token audience validation is:

`900430977321-mf76iecc9im76c53mh863shj9b29jk9p.apps.googleusercontent.com`

Android package:

`com.skyare.stackupacademy`

Before production, verify the Google Cloud Android OAuth client uses the correct production signing SHA-1/SHA-256 fingerprints.

## Biometrics

Flow:

1. user authenticates with Google (or, once implemented, WhatsApp);
2. Supabase creates a session;
3. the session is stored locally;
4. on a later entry, BiometricPrompt validates the local device user;
5. the app refreshes/resumes the Supabase session.

Biometrics are not a standalone remote identity provider.

## Academy Coach

`auth-production.js` can upsert the authenticated user's Coach preference into `public.profiles`.

Current fields include:

- WhatsApp number;
- opt-in and opt-in timestamp;
- frequency (`included_2_week` or `daily`);
- daily/weekly limits;
- time zone;
- update timestamps.

The table currently has RLS enabled with own-row SELECT/INSERT/UPDATE/DELETE policies based on `auth.uid() = user_id`.

Message delivery is a separate integration and must not be considered active until a provider is configured.

## WhatsApp OTP — remaining work

When enabling WhatsApp login:

1. enable Phone Auth in Supabase;
2. configure Twilio/Twilio Verify with a WhatsApp-capable sender;
3. add the OTP request/verification calls to the production auth script;
4. enforce rate limits and abuse protection;
5. test E.164 numbers and wrong/correct OTP paths;
6. update privacy policy and Play Data Safety if the final data flow differs.

## Release verification

### Google

- native account picker opens;
- ID token is accepted by Supabase;
- new/existing user appears in Supabase Auth;
- refresh token restores the session.

### Biometrics

- unavailable/failed/cancelled biometric checks do not open the app;
- biometrics cannot be used before a valid Supabase session;
- successful biometric check resumes a valid session.

### Coach

- only the authenticated user's profile row can be written/read;
- invalid WhatsApp numbers are rejected;
- opt-in and frequency persist correctly;
- message delivery remains off until its provider is configured.

### Before production rollout

- decide whether to keep or disable `TEST_ACCESS`;
- complete WhatsApp OTP if it will be offered at launch;
- implement/validate Google Play Billing;
- test the in-app account-deletion request and external deletion page;
- complete Play Data Safety from the exact final build.
