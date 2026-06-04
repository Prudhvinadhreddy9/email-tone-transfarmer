package com.learning.emailtone.controller;

import com.learning.emailtone.dto.ToneInfo;
import com.learning.emailtone.model.Tone;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * GET /api/tones — lets the React frontend populate its tone selector
 * without hardcoding strings.
 *
 * <p>WHY an endpoint instead of a static JSON file: when we add or rename a
 * tone server-side, the UI picks up the change automatically — no frontend
 * deploy needed.
 *
 * <p>WHY {@link ToneInfo} instead of returning the raw enum: the enum carries
 * an {@code aiInstruction} field that is prompt internals. We don't want to
 * leak prompt internals over the wire (security + freedom to iterate).
 */
@RestController
@RequestMapping("/api")
public class ToneController {

    @GetMapping("/tones")
    public List<ToneInfo> tones() {
        return Arrays.stream(Tone.values())
                .map(t -> new ToneInfo(t.name(), t.displayName(), t.description()))
                .toList();
    }
}
