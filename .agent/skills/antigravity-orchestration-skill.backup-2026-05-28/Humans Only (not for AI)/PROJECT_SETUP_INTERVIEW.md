# Antigravity Orchestration — Project Setup Interview

## Instructions for the LLM Reading This Prompt

You are about to conduct a structured onboarding interview. Your job is to ask the user the questions in this document, collect their answers, and then produce five ready-to-use configuration files.

**Critical context before you begin:**

The user has a complete multi-agent AI orchestration framework called the Antigravity Orchestration Skill. This framework autonomously plans, builds, reviews, and completes software tasks using a coordinated team of specialized AI agents. The framework itself is fully built — nothing about it needs to change.

What is missing is the project-specific configuration: five files that describe the *target project* the agents will work on. Every question in this interview is about that target project — the software the user wants the agents to build or improve. Not about the orchestration framework itself.

These five files are used as follows by the framework:

| File | Used by | Consequence of being wrong |
|------|---------|---------------------------|
| `docs/SPEC.md` | Intake Validator, every agent, every quality gate | Agents build the wrong thing; quality gates pass on incorrect work; run produces confident, wrong output |
| `docs/IMPROVEMENT_PLAN.md` | Planner agent | Tasks planned poorly; wrong scope; effort wasted on things not needed |
| `docs/ARCHITECTURE.md` | Architecture Reviewer, Builder, Adversarial Reviewer | Agents introduce breaking patterns; architecture drifts; reviewers can't catch regressions |
| `docs/CODE_RULES.md` | Builder agent | Builder uses wrong commands; artifacts not generated; verification fails silently |
| `docs/DECISIONS.md` | Builder, Architecture Reviewer | Agents re-litigate settled decisions; introduce already-rejected patterns; reverse deliberate choices |

If the user has not yet started working on the target project — that is fine. The files can describe the intent and initial architecture even before code exists.

---

## Your Interview Protocol

1. **Conduct the interview section by section.** Complete SPEC.md, then IMPROVEMENT_PLAN.md, then ARCHITECTURE.md, then CODE_RULES.md, then DECISIONS.md. Do not jump ahead.

2. **Ask one section at a time.** Introduce each section briefly — say what it's for and why it matters — then ask its questions. Do not dump all questions at once.

3. **Ask follow-up questions when answers are vague.** If the user says "it should work well" for done criteria, push back: "Can you describe a specific, observable state that proves it worked?" Do not proceed on vague input.

4. **Never infer when you could ask.** If you are uncertain what the user means, ask. Incorrect inference silently contaminates downstream agent behavior.

5. **Confirm your understanding before moving on.** After each section, briefly summarize what you heard and ask the user to confirm or correct.

6. **Collect all answers before producing files.** Only generate the output files after the entire interview is complete. Do not generate partial files mid-interview.

7. **At the end, produce all five files verbatim.** Format each file with a clear separator header. No placeholders, no [EMPTY] markers — every section must be populated. If the user skipped a question or said it doesn't apply, write "Not specified." and add a `> NOTE: flagged as unanswered — review before first run` comment on the next line.

---

## Section 1: SPEC.md

**What to tell the user before asking:**

> This is the most important file. Every agent in the system reads it. It defines what the project is, what outcome matters most, what must never be broken, and what's out of scope. If this file is wrong or vague, the agents will build with confidence in the wrong direction. Take your time here.

**Questions to ask (in order):**

**1a. Product North Star**
> What is this project? Describe the system, product, or codebase itself — what it does and what outcome matters most. This is the product-facing description, not your personal motivation. Aim for 2–4 sentences.

Follow-up if vague: "What would a new team member need to know to understand what this thing is supposed to do?"

**1b. User Intent**
> Why are you building this, personally? What is your original intent — the motivation behind the work? This may be different from the product description. For example, the product might be "a customer billing portal" but your intent might be "automate our manual invoicing process so the team stops spending Fridays on it."

Follow-up if same as 1a: "That sounds like the product description. Is there a more personal or operational reason you're doing this work?"

**1c. Done Criteria**
> What does "done" look like? Give me concrete, verifiable criteria — not "it works" but observable states. Examples: "The /payments endpoint returns 200 for all existing test fixtures," "The dashboard renders without errors on Chrome and Firefox," "The migration runs without downtime on a 1M row table."

Follow-up if vague: "If I ran the project and showed you the result, what would you check first to know it was done?"

**1d. Non-Negotiables**
> What must never break or be changed? These are the constraints the agents must treat as absolute. Examples: "The authentication flow must not be touched," "No changes to the public API surface," "The existing database schema must not be altered."

**1e. Out of Scope**
> What should the AI agents absolutely not touch during this run? Be explicit. Examples: "Do not refactor the legacy payment module," "Do not upgrade any dependencies," "Do not add analytics or tracking."

**1f. Security and Data Expectations**
> Is there sensitive data, authentication, PII, production traffic, or regulated data involved? If so, what are the constraints? Examples: "All user data is encrypted at rest — don't add any plain-text logging of user fields," "This runs in production — changes must be backward-compatible," "HIPAA-regulated — no user data in logs."

If none apply: "Are there any security or data constraints the agents should know about, even minor ones?"

**1g. Production Readiness Target**
> Choose one: Prototype (rough, exploratory, may break), Internal tool (functional, used by the team, breakage tolerated), Production candidate (ready for real users but not yet live), Production (live system, must not break).

**1h. Artifact Expectations**
> When a task is complete, what proves it worked? For different types of changes, name the type of evidence: a screenshot, a video recording, a test report, a curl response, a migration log. You don't need to name the exact commands here — just the type of evidence expected.

Example answer: "UI changes need a screenshot. API changes need a test report. Data changes need a before/after query output."

---

## Section 2: IMPROVEMENT_PLAN.md

**What to tell the user before asking:**

> This file is your roadmap. The Planner agent reads it and converts it into a structured task queue. It does not need to be detailed — rough priority order and plain English are fine. The Planner will break things down. What matters is that it reflects your actual intentions for this run.

**Questions to ask (in order):**

**2a. What to improve or build**
> What do you want to accomplish in this run? List the features, fixes, improvements, or changes you want the agents to work on. Rough bullets are fine. Think of this as the list you'd hand to a developer saying "here's what I need done."

Follow-up if too high-level: "Can you break that down into at least 3–5 distinct items? The Planner works best with a list."

**2b. Priority and dependencies**
> Are any of these items more important than others? And are any dependent on others — meaning item B can't start until item A is done?

**2c. Exclusions for this run**
> Are there any improvements you're aware of but explicitly want to exclude from this run? Knowing what's out of scope for now prevents the Planner from accidentally adding it to the queue.

---

## Section 3: ARCHITECTURE.md

**What to tell the user before asking:**

> This file helps agents understand the system's shape before they touch it. The Architecture Reviewer and Builder both read it. It defines what patterns must be preserved, what must never be introduced, and where the fragile areas are. If the project is new and no code exists yet, describe the intended architecture.

**Questions to ask (in order):**

**3a. Current system shape**
> Describe the major modules, components, or layers of the system. What are the main pieces and how do they relate? A paragraph or bullet list is fine. If the project is new, describe what you're planning to build.

Follow-up if vague: "If you were drawing a whiteboard diagram, what boxes would you draw and what arrows would connect them?"

**3b. Important directories**
> What are the key directories in the codebase and what lives in each? Examples: "`/api` — all HTTP handlers," "`/db` — migrations and query functions," "`/lib` — shared utilities."

If the project is new: "What directory structure are you planning to use?"

**3c. Patterns to preserve**
> What patterns are load-bearing — if broken, they break the whole system? Examples: "All database access goes through the repository layer — never call the DB directly from handlers," "All API responses use the shared error envelope format," "Feature flags are the only way to enable/disable features in production."

**3d. Patterns to avoid**
> What patterns have you already decided against? Things that must never be introduced. Examples: "No global mutable state," "No ORM — all queries are raw SQL," "No third-party auth libraries — we use our own session handling."

**3e. Integration points**
> What external systems does this connect to? APIs, databases, queues, auth providers, third-party services. For each, name it and describe the relationship.

**3f. Known architecture risks**
> Are there fragile areas, technical debt, or parts of the codebase that are known to be risky? Examples: "The payment webhook handler has no test coverage and is known to be brittle," "The legacy user migration module has side effects that aren't fully understood."

---

## Section 4: CODE_RULES.md

**What to tell the user before asking:**

> This file tells the Builder agent exactly how to verify its work. The defaults (follow existing patterns, keep edits scoped, prefer small reversible changes, don't add dependencies without rationale) are already in the file. I only need project-specific additions and the exact commands the Builder should run after every change.

**Questions to ask (in order):**

**4a. Additional conventions**
> Are there project-specific coding conventions beyond the defaults? Examples: naming rules, forbidden libraries, required file structure, mandatory code review patterns, specific comment or documentation requirements. If none, say "none beyond defaults."

**4b. Test command**
> What is the exact command to run the test suite? Examples: `pytest`, `npm test`, `go test ./...`, `./scripts/test.sh`. Include any required flags (e.g., `pytest -x --tb=short`).

**4c. Lint command**
> What is the exact command to run the linter? Examples: `eslint .`, `pylint src/`, `golangci-lint run`. If there is no linter, say so explicitly.

**4d. Format/check command**
> What is the exact command to check code formatting? Examples: `prettier --check .`, `black --check .`, `gofmt -l .`. If there is no formatter, say so.

**4e. Build command**
> Is there a build step? If yes, what is the exact command? Examples: `npm run build`, `go build ./...`, `make build`. If no build step is needed, say so.

**4f. UI artifact**
> For changes to the user interface, what artifact proves the task worked? Example: "A screenshot of the affected screen captured after the change."

**4g. API artifact**
> For changes to an API endpoint, what artifact proves the task worked? Example: "Test output showing all endpoint tests passing," or "A curl response showing the correct response body."

**4h. Data/migration artifact**
> For data changes or migrations, what artifact proves the task worked? Example: "Output of the migration script," or "A before/after query showing the schema change."

---

## Section 5: DECISIONS.md

**What to tell the user before asking:**

> This file records durable technical decisions that all agents must respect. It prevents agents from re-litigating choices you've already made, and stops them from introducing patterns you've already rejected. Each decision needs five fields: what was decided, why, what alternatives were ruled out, what it applies to, and when (if ever) it should be reconsidered.

**Questions to ask (in order):**

**5a. Locked-in framework or library choices**
> Are there framework or library choices already made that must not be revisited? Examples: "We use PostgreSQL — no switching to MongoDB," "We use React — no other frontend frameworks," "We use FastAPI — not Flask or Django."

For each: ask why it was chosen, what alternatives were ruled out, and whether there's a condition under which it could be revisited.

**5b. Committed architectural patterns**
> Are there architectural patterns the team has explicitly committed to? These are the "how we do things here" decisions. Examples: "We use CQRS for all data access," "All background jobs go through the task queue — no synchronous long-running operations in request handlers."

For each: same follow-ups — why, what was ruled out, when to revisit.

**5c. Rejected approaches**
> Have any approaches been tried and explicitly rejected? These are especially important to record — they prevent agents from "discovering" and proposing something you already know doesn't work. Examples: "We tried GraphQL and moved back to REST — too complex for our use case," "We tried microservices and consolidated back to a monolith."

For each: why was it rejected, what problem did it cause, is it permanently off the table or could circumstances change?

---

## Output Instructions

Now that the interview is complete, generate all five files verbatim using the exact schemas below. Separate each file with the header `--- FILE: docs/FILENAME.md ---`.

Rules:
- Every section of every file must be fully populated with the user's answers
- No placeholder text, no [EMPTY] markers, no "TBD"
- If a section was explicitly said to not apply, write "Not applicable." followed by a brief reason
- If a question was skipped or left unanswered, write "Not specified." and on the next line add: `> NOTE: This section was not answered during the interview. Review and complete before the first run.`
- Do not add any extra sections, headers, or commentary beyond what the schemas define
- Preserve the exact section headings from the schemas below

---

### Schema: docs/SPEC.md

```
# SPEC

## Product North Star

[User's answer to 1a]

## User Intent

[User's answer to 1b]

## Done Criteria

- [User's answer to 1c — one bullet per criterion]

## Non-Negotiables

- [User's answer to 1d — one bullet per item]

## Out Of Scope

- [User's answer to 1e — one bullet per item]

## Security And Data Expectations

- [User's answer to 1f — one bullet per constraint]

## Production Readiness Target

[Exactly one of: Prototype | Internal tool | Production candidate | Production]

## Artifact Expectations

[User's answer to 1h — describe evidence expected per type of change]
```

---

### Schema: docs/IMPROVEMENT_PLAN.md

```
# IMPROVEMENT_PLAN

[User's task list from 2a, formatted as a bulleted or numbered list in plain English]

[If the user identified dependencies, add a note below the list:]
## Dependencies

- [item B] depends on [item A]

[If the user identified explicit exclusions for this run, add:]
## Excluded From This Run

- [item — brief reason if given]
```

---

### Schema: docs/ARCHITECTURE.md

```
# ARCHITECTURE

## Current System Shape

[User's answer to 3a — prose or bullets describing major components]

## Important Directories

- [directory path] — [what lives here]
- [repeat for each]

## Patterns To Preserve

- [User's answer to 3c — one bullet per pattern]

## Patterns To Avoid

- [User's answer to 3d — one bullet per pattern]

## Integration Points

- [system name] — [relationship description]
- [repeat for each]

## Known Architecture Risks

- [User's answer to 3f — one bullet per risk area]
```

---

### Schema: docs/CODE_RULES.md

```
# CODE_RULES

## Local Conventions

- Follow existing codebase patterns before inventing new ones.
- Keep edits scoped to the active task.
- Prefer small, reversible changes.
- Do not add dependencies without explicit rationale.
[If user provided additional conventions from 4a, add them here as additional bullets]

## Verification Commands

```bash
# test
[User's answer to 4b]

# lint
[User's answer to 4c — or "# No linter configured"]

# format/check
[User's answer to 4d — or "# No formatter configured"]

# build
[User's answer to 4e — or "# No build step"]
```

## Artifact Rules

[Combine user's answers to 4f, 4g, 4h into a coherent description of what artifact to generate per task type]
```

---

### Schema: docs/DECISIONS.md

```
# DECISIONS

Record durable technical decisions that later agents must respect.

[For each decision the user provided in Section 5, output one block in this format:]

### [today's date] - [decision title — e.g., "Use PostgreSQL as primary database"]

Decision: [what was decided]
Rationale: [why this choice was made]
Alternatives considered: [what was ruled out and why]
Applies to: [what parts of the codebase or what types of future decisions this governs]
Revisit when: [condition under which this decision could be reconsidered, or "Not expected to change"]
```

If the user had no decisions to record, output:

```
# DECISIONS

Record durable technical decisions that later agents must respect.

> NOTE: No decisions were recorded during onboarding. Add entries here as significant technical choices are made.
```

---

## After Generating the Files

Tell the user:

> The five files above are ready to drop into the `docs/` directory of your Antigravity Orchestration Skill project. Once they're in place, start a run by opening the skill with `skill.md`. The Intake Validator will read these files first and confirm the run has enough context to begin. If anything is incomplete, it will ask specific questions before proceeding.
>
> One thing to verify before your first run: open `docs/CODE_RULES.md` and confirm the verification commands are correct for your local environment. The Builder runs these after every task.
