# Gemini rendering backend (developer setup)

The Android app does **not** contain a Gemini API key. The backend receives five non-personal car configuration fields, sends a prompt to Gemini, and returns image bytes. The app stores a generated image locally and only displays it when it matches the selected car configuration.

This is a Cloudflare Worker template, not a deployed service. A developer or authorized organization must deploy it and configure environment secrets `GEMINI_API_KEY` and `APP_ACCESS_TOKEN`. Only a HTTPS endpoint should be entered in the Android app. Never commit credentials to GitHub or share them through chat.

Endpoint:
- `GET /health`: backend status
- `POST /generate`: JSON `{"brand":"Skoda","model":"Kodiaq","year":"2023","color":"Silver","finish":"Glossy"}`, `Authorization: Bearer <server-access-token>`
- Returns PNG/JPEG, or an HTTP error when provider is unavailable.

Gemini may not accurately reproduce every model/year/paint style. Generated artwork is decorative, not an authoritative vehicle identification. Depending on Google model and billing entitlements, a request may incur a charge.
