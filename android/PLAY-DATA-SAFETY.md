# Google Play Data Safety — release preparation

This file is a release checklist, not a substitute for the declarations made in Play Console.

## Current Android shell

The Android package requests only:

- `android.permission.INTERNET`

The shell does not request contacts, location, microphone, camera, storage, SMS, phone, advertising ID, or background location permissions.

Android backup is disabled and Android 12+ data extraction rules exclude application data from cloud backup and device-to-device transfer.

## Current production services present in the build

The Android build currently contains an active Supabase project URL and publishable client key.

Implemented paths:

- Google Credential Manager obtains a Google ID token.
- `auth-production.js` exchanges that token with Supabase Auth when Google sign-in is used.
- Supabase access/refresh session data is stored locally under `stackup.supabase.session.v1`.
- Android BiometricPrompt only unlocks an existing authenticated session; raw biometric templates are never available to the app or Supabase.
- Academy Coach can sync the authenticated user's WhatsApp number, opt-in, frequency, limits and time zone to `public.profiles`.
- `public.profiles` has RLS enabled and ownership policies restrict SELECT/INSERT/UPDATE/DELETE to `auth.uid() = user_id`.

The current closed-test web build uses `TEST_ACCESS=true`, so testers can enter without creating an account and billing remains disabled. Data Safety declarations must nevertheless be reviewed again from the exact release candidate before production, especially if `TEST_ACCESS` is disabled.

## WhatsApp login status

WhatsApp/phone OTP is documented as a target authentication method, but it is **not yet wired into the current `auth-production.js` login flow**. Do not declare WhatsApp OTP/Twilio as active until it is actually enabled and tested.

Academy Coach is separate from login: its phone/consent preference may already be stored in Supabase for an authenticated user, while message delivery itself must not be described as active until a messaging provider is configured.

## Purchases

The web app currently displays plan information but closed-test billing is disabled. When Google Play subscriptions are enabled, update the Data Safety and Payments declarations based on the exact Billing implementation.

## Data categories to review before production

At minimum review:

- name and email from Google identity, when Google sign-in is enabled;
- Supabase user/account identifiers and authentication session data;
- WhatsApp phone number, opt-in, selected Coach frequency and time zone when Academy Coach is configured;
- local study progress/history/preferences (currently local to the device);
- purchase/subscription status once Google Play Billing is enabled;
- technical connection data processed by hosting/auth providers.

## Account deletion

The app now distinguishes:

1. **Delete data from this device** — clears Academy/Wraps local data and the local Supabase session token.
2. **Request account deletion** — opens a deletion request addressed to `skyarecompany@gmail.com` for removal of the Supabase account and synced profile data.

The external privacy/deletion page must expose the same process and be used in the Play Console account-deletion field.

## Release gate

Do not submit the Data Safety form from assumptions. Complete it from the exact production release candidate after checking:

- Android permissions and dependencies;
- web scripts/services actually reachable in production;
- authentication providers enabled in Supabase;
- RLS and table grants;
- Academy Coach messaging provider, if activated;
- analytics/crash SDKs;
- Google Play Billing;
- privacy policy and both local/cloud deletion paths.
