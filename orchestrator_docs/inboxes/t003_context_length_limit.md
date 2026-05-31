# Limit beforeCursor text length to prevent Whisper prompt token overflow
Severity: medium

Whisper's initial_prompt parameter has a strict token limit (typically 224 tokens). Because `MultiModelRunner` now appends the glossary to the end of the context prompt, passing an extremely long `beforeCursor` string from the input field could push the glossary out of the context window or cause native assertion failures. In T003, you must ensure that the extracted text context is truncated to a safe length (e.g., the last 50-100 words or 200 characters) before passing it into `recognizerView.start()`.
