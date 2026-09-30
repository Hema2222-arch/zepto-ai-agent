# Zepto AI Agent — Railway deployment

1. Push this project to GitHub or upload the project to Railway.
2. In Railway, create a new project and deploy the repository.
3. Railway detects the Dockerfile automatically.
4. Wait for the build/deploy to finish.
5. Open the generated HTTPS domain.
6. On mobile, open the domain in Chrome/Edge and allow microphone access.
7. The voice command is posted to `/api/run`.
8. Playwright runs Chromium in headless cloud mode (`PLAYWRIGHT_HEADLESS=true`).

For local development, set `PLAYWRIGHT_HEADLESS=false`.

Important: the current agent is designed to stop short of payment/checkout and does not bypass authentication, CAPTCHA, or other security controls.
