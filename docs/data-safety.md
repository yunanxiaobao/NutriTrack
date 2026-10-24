# NutriTrack — Play Data Safety mapping

Working reference for the Google Play **Data safety** section. Reflects the
app as of the Firebase migration (PR-A…PR-D). Update this table whenever a
data flow changes, then mirror it into the Play Console form.

| Data type | Collected | Shared with third parties | Purpose | Processor / notes |
|---|---|---|---|---|
| Email address | Yes | No | Account management (sign-up, login, password reset) | Firebase Authentication (Google) |
| Username / display name | Yes | No | App functionality (profile, personalisation) | Firestore (Google) |
| Password | Yes (hashed by provider) | No | Account management | Firebase Authentication; never stored by the app itself |
| Health info — questionnaire answers (meal timing, sleep/wake, food-category preferences, persona) | Yes | No | App functionality, personalisation | Firestore (Google); user can delete with account |
| User-generated content — NutriCoach chat prompts & saved tips | Yes | **Yes** — prompts are sent to the AI provider to generate answers | App functionality | Zhipu AI (GLM API); tips history stored in Firestore |
| Photos — food photos the user chooses to analyse | No (not retained) | **Yes** — the chosen image is sent to the AI provider for fruit identification, not stored by NutriTrack | App functionality | Zhipu AI (GLM vision API) |
| App activity / diagnostics / device IDs | No | No | — | No analytics or crash SDKs are integrated (Analytics was deliberately not enabled on the Firebase project) |
| Location | No | No | — | Not requested |
| Financial info | No | No | — | The app is free; no payments |

Declarations to mirror in Play Console:

- **Data deletion**: users can delete their account and data in-app
  (Settings → Account → Delete Account) and via the web page
  (`docs/account-deletion.html`, to be hosted at the URL given to Play —
  GitHub Pages or Firebase Hosting).
- **Encryption in transit**: Yes (Firebase + HTTPS APIs).
- **Data used for tracking / ads**: None. No ads SDKs.
- Account creation: email + password; account deletion as above.
