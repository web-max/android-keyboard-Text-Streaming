# Prevent splitting UTF-16 surrogate pairs in context prompt

Severity: high

Do not use `takeLast(200)` blindly as it can split emojis or other surrogate pairs. Adjust the truncation logic to either find a safe word boundary (like a space) or ensure the starting character is not an unpaired surrogate, before passing the prompt to the native model.
