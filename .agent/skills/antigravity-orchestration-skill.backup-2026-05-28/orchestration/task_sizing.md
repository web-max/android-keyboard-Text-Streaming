# Task Sizing

## Small

Use for isolated, low-risk work.

Signals:

- One file or one narrow area.
- Clear definition of done.
- No new architecture pattern.
- Verification is obvious.

## Medium

Use for bounded multi-file work.

Signals:

- Multiple files in related areas.
- Some judgment required.
- Moderate regression risk.
- Verification needs tests plus an artifact or manual check.

## Large

Large tasks do not go to Builder.

Signals:

- Touches unrelated systems.
- Introduces a new pattern.
- Definition of done is fuzzy.
- Requires decisions not already in the spec or decisions doc.
- Could be interpreted multiple ways.
- High chance of breaking existing behavior.

## Split Rule

Every large task must be split into small or medium tasks before execution.

A split task should have:

- A single outcome.
- A clear boundary.
- A verification method.
- An artifact expectation when user-facing.
