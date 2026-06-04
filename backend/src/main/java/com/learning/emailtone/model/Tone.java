package com.learning.emailtone.model;

/**
 * Tone enum carrying its own prompt-engineering metadata.
 *
 * <h2>Why the enum holds its own AI instruction</h2>
 *
 * A common mistake in LLM apps is to scatter prompt fragments across the
 * codebase: a switch statement in the service, lookup maps in the controller,
 * "magic strings" in tests. That works for two tones; it falls apart at six.
 *
 * <p>Keeping the AI instruction ON the enum value gives us:
 * <ul>
 *   <li><b>Single source of truth</b> — change the wording in one place and
 *       every code path picks it up.</li>
 *   <li><b>Type safety</b> — invalid tones are caught at compile time, not
 *       at LLM-call time.</li>
 *   <li><b>Testability</b> — unit tests can assert that the right instruction
 *       text reaches the prompt, without having to mock a lookup service.</li>
 *   <li><b>Discoverability</b> — IDE "find usages" shows you everywhere a
 *       tone is referenced, including its prompt fragment.</li>
 * </ul>
 *
 * This pattern (enum carries the prompt-engineering knowledge) is reusable
 * across many AI projects: severity levels for a bug-triage assistant,
 * audiences for a content rewriter, register/style for a translation tool.
 *
 * <p>The {@code displayName} and {@code description} fields are surfaced to
 * the frontend via {@code GET /api/tones} so the UI can render a selector
 * without hardcoding strings of its own. {@code aiInstruction} is NOT exposed
 * — it's prompt internals that should stay server-side (both for prompt
 * security and to give us flexibility to tweak prompts without breaking
 * frontend contracts).
 */
public enum Tone {

    PROFESSIONAL(
            "Professional",
            "Formal and business-appropriate",
            // Concrete behavioral instructions beat abstract labels. "Use formal
            // salutations" is a thing the model can do; "be professional" is
            // not. Each clause names a specific writing decision.
            "Use formal salutations and a closing. Write complete sentences with no contractions. " +
            "Prefer polite, measured phrasing. Avoid slang, exclamation marks, and casual expressions. " +
            "Keep language clear and respectful."
    ),

    FRIENDLY(
            "Friendly",
            "Warm, conversational, approachable",
            "Use a warm greeting and a personable closing. Contractions are welcome. " +
            "Sound human and conversational without being unprofessional. A single, well-placed " +
            "exclamation mark is fine — don't pile them up. Show genuine care without being saccharine."
    ),

    ASSERTIVE(
            "Assertive",
            "Direct, confident, no fluff",
            "Be direct and confident. State the request or point in the first sentence. " +
            "Cut hedging phrases ('I was wondering if maybe', 'sorry to bother you'). " +
            "Use active voice. Keep it short. Stay polite — assertive is not aggressive."
    ),

    APOLOGETIC(
            "Apologetic",
            "Acknowledges fault, takes responsibility",
            "Open with a clear, specific acknowledgement of what went wrong. Use 'I' statements " +
            "(\"I missed the deadline\") rather than passive constructions (\"the deadline was missed\"). " +
            "Avoid empty phrases like 'sorry for any inconvenience'. Offer a concrete next step or remedy. " +
            "Be sincere; don't over-apologize to the point of self-flagellation."
    ),

    CONCISE(
            "Concise",
            "Removes filler, gets to the point in fewer words",
            "Strip every filler phrase: 'I just wanted to', 'I hope this finds you well', " +
            "'as previously discussed', etc. Aim to halve the word count without losing any factual " +
            "content or specific request. Keep one short greeting and one short closing. " +
            "Bullet lists are fine when they help."
    ),

    DIPLOMATIC(
            "Diplomatic",
            "Tactful, careful with disagreement or bad news",
            "Soften disagreement and bad news without obscuring them. Acknowledge the other party's " +
            "perspective before stating yours. Use phrases like 'I understand your point, and...' " +
            "(not 'but'). Be specific about what you can and can't do. Avoid blame language; " +
            "focus on the situation and the path forward."
    );

    private final String displayName;
    private final String description;
    private final String aiInstruction;

    Tone(String displayName, String description, String aiInstruction) {
        this.displayName = displayName;
        this.description = description;
        this.aiInstruction = aiInstruction;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public String aiInstruction() {
        return aiInstruction;
    }
}
