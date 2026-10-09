## UI standard (all Netra apps)

Every screen follows these rules. The `UI standard check` workflow (`scripts/ui-standard-check.sh`) fails a pull request that breaks the rules it can test by text search.

1. One header, 56 dp: app name, installed version, date and time. Nothing else.
2. Only the header and the footer (bottom bar) stay fixed. Everything else scrolls with the page.
3. One line per tab or chip label (`maxLines = 1`). No letter-by-letter wrapping; long values wrap on the right.
4. No overlays or see-through panels over content.
5. No developer or internal wording in text the user sees (no module codes, "score weight", "telemetry", "session id").
6. The same fact is shown once on a screen.
7. Light and dark theme both readable; no dark card on a light screen.
8. If a value has no evidence, show "Unavailable". Never invent data.

The check script tests rules 3 (filter chip and tab labels) and 5 (a list of banned internal words). The rest are a review checklist: tick them in the pull request.

## Security review before release
Check each of these on every Player release. Mark FIXED with tested changes, ALREADY covered with evidence, or NOT APPLICABLE with a reason. Use UNVERIFIED for missing evidence; it is not a pass. Repeat the review when a backend, account or cloud feature is added.

1. SQL injection
2. XSS
3. CSRF
4. File-upload validation
5. SSRF
6. Object-level authorization
7. Server rate limiting
8. Password hashing
9. MFA
10. Server permissions and Android exported components
11. Row-level/database rules
12. JWT signing secrets
13. API secrets server-side
14. No authentication tokens in local storage
15. No production default credentials
16. Restricted credentialed CORS
17. Webhook signatures
18. No exposed web source maps
19. No sensitive data in logs
20. Vulnerable dependencies addressed

Do not equate local input checks or client preferences with server enforcement. Debug CI signing credentials are disposable development-only keys, never production keys. Do not edit shared site or Firebase settings as part of a Player-only change.
