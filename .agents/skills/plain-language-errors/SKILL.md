---
name: plain-language-errors
description: Explain errors, blocked checks, and recovery steps in plain language during work, with enough detail for the user to understand the impact.
---

# Plain-language error updates

When a meaningful error affects the task, explain it to the user in one to three short sentences. Say what failed, what it means for their goal, and what you are doing about it. Use the feature or service name the user recognizes. Mention an error code or exact log line only when it helps them identify the failure.

Distinguish confirmed causes from things you are still checking. If the failure is recoverable, continue working and report when recovery succeeds. If a verification step is still running, say so. If you need the user's help, say exactly what they need to do and why.

A setup error update could read “GitHub started the check, but the runner was missing Android’s SDK installer. I added a pinned installer and pushed the fix. GitHub has started a fresh run on the same pull request.”
