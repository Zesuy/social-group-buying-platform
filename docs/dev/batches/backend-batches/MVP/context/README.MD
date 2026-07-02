# Context Notes Rule

This folder is used to keep development context between batches.

Before starting any batch, always read:

* `AGENTS.md`
* the current batch file in `docs/dev/batches/`
* `context/latest-context.md`
* related docs in `docs/`

## Required workflow for every batch

Before editing any project code or docs, you must first copy the current context file:

```bash
cp context/latest-context.md context/batch-xxx-short-name.md
```

Replace `xxx-short-name` with the current batch name.

Examples:

```bash
cp context/latest-context.md context/batch-00-project-foundation.md
cp context/latest-context.md context/batch-01-auth-current-user.md
cp context/latest-context.md context/batch-03b-groupbuy-publish.md
```

After copying the old context, update `context/latest-context.md` for the current batch.

## What to write in latest-context.md

At the end of each batch, update `context/latest-context.md` with:

1. Current batch name
2. What was implemented in this batch
3. Files created or modified
4. Database migrations created or modified
5. Tests added or modified
6. Test commands executed and results
7. API or documentation changes
8. Frontend integration notes
9. Problems found
10. Problems fixed
11. Remaining issues, or write `None`
12. Suggestions for the next batch

## Important rules

* Do not delete old context files.
* Do not overwrite old `batch-xxx-xxx.md` files.
* Do not start coding before copying `latest-context.md`.
* Do not leave known problems undocumented.
* If there are no remaining issues, write `Remaining issues: None`.
* If the next batch needs special attention, write it clearly in `Suggestions for next batch`.

## latest-context.md template

````md
# Latest Context

## Current Batch

Batch X: batch title

## What was done

- ...

## Files created or modified

- ...

## Database migrations

- ...

## Tests added or modified

- ...

## Test commands and results

```bash
mvn test
````

Result:

```text
Passed / Failed
```

## API or documentation changes

* ...

## Frontend integration notes

* ...

## Problems found

* ...

## Problems fixed

* ...

## Remaining issues

None

## Suggestions for next batch

* ...

```
```
