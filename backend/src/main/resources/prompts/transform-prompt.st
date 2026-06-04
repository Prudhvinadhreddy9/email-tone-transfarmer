{!
  transform-prompt.st — per-request user prompt.

  Placeholders rendered by Spring AI's StTemplateRenderer (StringTemplate):
    {tone}              the tone NAME (PROFESSIONAL, FRIENDLY, ...)
    {toneDescription}   one-line description from the Tone enum
    {toneInstruction}   the detailed behavioral instruction from Tone.aiInstruction()
    {contextSection}    pre-rendered "Context: ..." block, or empty string
    {lengthInstruction} pre-rendered length rule, or empty string
    {originalEmail}     the user's draft
    {format}            JSON schema injected by BeanOutputConverter

  PROMPT ENGINEERING DECISIONS:

  1. TASK STATEMENT FIRST. Models weight early tokens heavily. State the
     job in the first sentence — don't bury it.

  2. TONE GUIDANCE comes from the enum, not the prompt. Each tone has its
     own concrete instruction (see Tone.java). That keeps prompt tuning in
     one place: edit the enum, all paths pick it up.

  3. NUMBERED RULES, RESTATED. "Preserve facts" and "do not invent" are
     ALSO in the system prompt. Critical rules deserve repetition, and the
     user prompt is closer to the answer in attention terms. Belt and braces.

  4. CONTEXT IS OPTIONAL AND CLEARLY MARKED. When present it goes in its
     own block. When absent, the section is omitted entirely (we don't
     emit "Context: none" — empty trumps placeholder).

  5. {format} IS LAST. BeanOutputConverter generates a JSON schema and
     stuffs it into this placeholder. Putting format last places it
     closest to the answer in the model's attention window — minimizes
     schema drift.

  6. NO MARKDOWN, NO FENCES, EXPLICITLY. Without this, ~30% of responses
     come back wrapped in ```json fences which permissive parsers can
     handle but strict ones (or strict-ish ones like BeanOutputConverter
     before its tolerance pass) can choke on. Cheap to ask, costly to debug.
!}
Rewrite the following email in a {tone} tone. {toneDescription}.

Tone guidance:
{toneInstruction}
{contextSection}
Rules:
1. Preserve all factual content and explicit requests from the original.
2. Do not invent recipient names, dates, numbers, or details not in the original.
3. {lengthInstruction}
4. List the meaningful changes you made in `changesNotes` — short bullet points, 3-6 items, each one a concrete edit (e.g. "Replaced 'ASAP' with a polite request").
5. Return ONLY valid JSON matching the schema below. No markdown, no code fences, no prose outside the JSON.

Original email:
"""
{originalEmail}
"""

OUTPUT FORMAT:
{format}
