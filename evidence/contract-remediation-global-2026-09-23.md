# Global Contract Gate RETURN remediation

Date: 2026-09-23  
Input: `evidence/contract-review-global-2026-09-23.md`

## Finding closure

The owner-confirmed M02 scope already defers repository content access under HD-004 and external integrations under HD-006. Therefore the contract was corrected by removing `GET /projects/{projectId}/resources/{resourceId}/content` and all associated binary/text content response schemas. The M02 API retains resource metadata listing and repository references only. The security contract now explicitly states that M02 does not expose repository content read/download APIs; any future content access requires a separate Contract defining retrieval, project authorization, and security boundaries.

The previously referenced M02 scoped review is now tracked at `evidence/contract-review-M02-2026-09-23.md`; its addendum distinguishes the scoped PASS from later owner-approved deferrals and confirms it does not approve the Global Contract Gate.

## Verification

- `git diff --check`: passed.
- OpenAPI YAML parsing: passed; resource content endpoint is absent and no binary/text content response remains.
- No build, tests, or database migration were run.

## Gate status

The blocking finding is addressed for independent re-review. The latest review decision remains RETURN until an independent reviewer records a new decision. Global Contract Gate remains pending; Development is not unlocked.
