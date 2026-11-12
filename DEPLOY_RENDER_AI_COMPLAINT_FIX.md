# BarangayEase Backend – AI and Complaint Classification Fix

## Changes in this package

1. Complaint classification now receives both the complaint title and description.
2. The independent emergency keyword screen also checks both fields. A critical keyword in the title (for example, `rape`) cannot be missed just because the description says only `please help me`.
3. Added regression tests for critical, immediate, and mild title/description combinations.
4. Existing backend-owned classification remains authoritative; the browser-supplied `classificationType` is not trusted.
5. Critical complaints continue to be saved with `classification_type=critical`, `status=Pending AI Review`, and `ai_review_status=PENDING`. Mild and immediate complaints remain in normal complaint handling.

## Render environment variables

In Render, open the deployed Spring Boot Web Service → **Environment** and set the following values. Use your own API keys; never commit them to Git or put them in frontend JavaScript.

| Key | Value |
|---|---|
| `GEMINI_API_KEY` | Your Google AI Studio Gemini API key |
| `GEMINI_MODEL` | `gemini-3.8-flash` |
| `GROQ_API_KEY` | Your Groq API key |
| `GROQ_MODEL` | `openai/gpt-oss-120b` |

You can configure one provider or both. If both are configured, the gateway tries Gemini first and then Groq if Gemini fails. Keep existing database, JWT, upload, and CORS environment variables unchanged unless you have a separate reason to update them.

After saving environment variables, trigger **Manual Deploy → Deploy latest commit** (or deploy the commit after pushing these code changes).

## Verify the deployment in Render Logs

On startup, look for lines similar to:

- `AI provider 'gemini' configured. Model: gemini-3.8-flash`
- `AI provider 'groq' configured. Model: openai/gpt-oss-120b`

If a provider is not fully configured, its startup log says it is not fully configured.

When a request fails, the gateway logs the provider and HTTP status. Interpret common statuses as follows:

- `401`: API key is invalid, revoked, or copied incorrectly.
- `403`: API access/permission is denied for that key or project.
- `400` or `404`: model ID, endpoint, or request configuration is not accepted.
- `429`: provider quota or rate limit was reached.
- timeout / connection error: outbound network, provider availability, or request timeout issue.

Do not share API keys. If AI still shows unavailable, copy only the relevant Render log lines (redact any secrets) and use those to identify the provider failure.

## Deploy code changes

1. Extract this package.
2. Replace/update the files in your backend Git repository with the files from this package.
3. Commit and push the changes to the branch connected to Render.
4. Wait for the Render deployment to finish successfully.
5. Check the startup logs and then test the cases below.

## Regression test cases

| Complaint title | Description | Expected classification | Expected handling |
|---|---|---|---|
| `rape` | `please help me` | Critical | AI Complaint / Pending AI Review |
| `Panggagahasa` | `Tulungan ninyo ako` | Critical | AI Complaint / Pending AI Review |
| `Sunog` | `May nasusunog na bahay` | Immediate | Normal complaint workflow, priority handling |
| `Tambak na basura` | `Hindi nakokolekta ang basura` | Mild | Normal complaint workflow |

The AI provider is not required for the emergency keyword safety screen to classify these clear keyword cases. A provider outage must not downgrade a critical complaint to mild.
