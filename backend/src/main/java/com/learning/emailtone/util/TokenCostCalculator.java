package com.learning.emailtone.util;

import com.learning.emailtone.dto.TokenUsageDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts raw token counts to an approximate USD cost.
 *
 * <p>WHY constants live here AND in application.yml: the YAML is for human
 * operator visibility (you can see what prices the app assumes); the constants
 * are for the runtime calculation. They MUST stay in sync — see the comment
 * in application.yml.
 *
 * <p>Pricing source: <a href="https://openai.com/api/pricing/">OpenAI pricing</a>.
 * As of 2026-01, gpt-4o-mini is $0.15 / 1M input tokens and $0.60 / 1M output
 * tokens. When pricing changes, update:
 * <ol>
 *   <li>The two constants below.</li>
 *   <li>{@code email-tone.pricing.*} in application.yml.</li>
 *   <li>The cost note in README.md.</li>
 * </ol>
 *
 * <p>WHY BigDecimal: the typical cost is single-digit micros. Floating-point
 * accumulates noise at that scale ($0.000299999... in JSON looks unprofessional).
 */
@Component
public class TokenCostCalculator {

    private static final BigDecimal INPUT_PRICE_PER_1M = new BigDecimal("0.15");
    private static final BigDecimal OUTPUT_PRICE_PER_1M = new BigDecimal("0.60");
    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    public TokenUsageDto toUsage(int promptTokens, int completionTokens) {
        BigDecimal input = INPUT_PRICE_PER_1M
                .multiply(BigDecimal.valueOf(promptTokens))
                .divide(ONE_MILLION, 8, RoundingMode.HALF_UP);
        BigDecimal output = OUTPUT_PRICE_PER_1M
                .multiply(BigDecimal.valueOf(completionTokens))
                .divide(ONE_MILLION, 8, RoundingMode.HALF_UP);
        double estimatedCostUsd = input.add(output)
                .setScale(6, RoundingMode.HALF_UP)
                .doubleValue();
        return new TokenUsageDto(
                promptTokens,
                completionTokens,
                promptTokens + completionTokens,
                estimatedCostUsd);
    }
}
