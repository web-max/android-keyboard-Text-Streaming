# Message Templates

## Intake Blocked

```text
Factory pause - Intake blocked

What happened:
Context:
Decision needed:
Questions:
Recommended next step:
```

## 3-Strike Task Failure

```text
Factory pause - Task failed 3 times

What happened:
Task:
Attempts:
Likely cause:
Options:
A. Retry with extra context
B. Narrow task
C. Skip task
D. Escalate to human/manual work
Recommended option:
```

## Drift Unclear

```text
Factory pause - Intent unclear

What happened:
Intent confidence:
Drift confidence:
Specific question:
Options:
Recommended option:
```

## Major Refactor Decision

```text
Factory pause - Architecture decision needed

What happened:
Context:
Risk if we continue:
Options:
A. Approve refactor now
B. Defer and continue
C. Narrow remaining work
Recommended option:
```

## Run Complete

```text
Factory complete

Summary:
Artifacts:
Fix now:
Fix later:
Needs your approval:
```
