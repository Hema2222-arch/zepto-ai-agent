# Zepto AI Agent — Vercel deployment

This version is prepared for Vercel's Docker container deployment.

## Deployment

1. Push the project files to GitHub.
2. In Vercel choose Add New -> Project.
3. Import `Hema2222-arch/zepto-ai-agent`.
4. Vercel detects `Dockerfile.vercel`.
5. Deploy.
6. Open the generated HTTPS URL on phone or laptop.

## Runtime

Vercel runs the Spring Boot HTTP server on `$PORT`.
The Playwright browser is headless in the cloud through:
`PLAYWRIGHT_HEADLESS=true`

For local development, set `PLAYWRIGHT_HEADLESS=false`.

## Important

The cloud browser has no desktop window for the user to see. Zepto authentication,
location selection, CAPTCHA, and similar security controls are not bypassed.
Checkout/payment remains manual.

The current agent is an HTTP-triggered browser automation prototype. For a production
version with live browser progress and persistent Zepto login, use a dedicated browser
worker/session service rather than relying on a background thread in a serverless
container.
