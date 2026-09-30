# Zepto AI Agent — Browser Agent Version

This version is closer to the workflow shown in the supplied video:

Voice command -> AI interpretation -> browser agent -> Zepto -> product search -> cart preparation -> user confirmation.

## Run

Set Java for your current PowerShell:

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
```

Then:

```powershell
.\mvnw.cmd spring-boot:run
```

Open:

http://localhost:8080

## Browser agent

The backend uses Playwright Java to open a Chromium browser. The browser is launched visibly so you can see what the agent is doing.

The agent does NOT:
- bypass login
- steal cookies
- bypass CAPTCHA or anti-bot controls
- handle payment credentials
- silently submit an order

You log in yourself if the site asks. The project is intended for an authorized integration/testing environment.

## Important Zepto integration note

Website selectors and checkout behavior can change. The browser agent therefore keeps selectors in one class (`ZeptoBrowserAgent`) so an authorized integration can update them.

Use automated ordering only where you have permission to do so and where the service's terms/API permit it.

## Optional OpenAI

Set:

```powershell
$env:OPENAI_API_KEY="YOUR_API_KEY"
```

If no key is present, the built-in parser handles common grocery commands.

## Example

"Order two milk, one bread and one kg tomatoes."

The agent:
1. parses the command
2. opens the browser
3. navigates to the Zepto homepage
4. searches each requested item
5. reports each browser step
6. stops before payment/final submission
