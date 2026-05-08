package com.omnimerchant.agent.tool;

import com.omnimerchant.agent.language.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Spring AI Tool: text translation for cross-lingual customer support.
 * Wraps the existing TranslationService as an LLM-callable tool.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslationTools {

    private final TranslationService translationService;

    @Tool(description = """
            Translate text from one language to another. \
            Use this tool when you need to understand a customer's message in a \
            foreign language, or when you want to convey information to the customer \
            in their native language. Supports 12 languages: en, es, pt, ja, de, fr, \
            it, ko, ar, zh, vi, th. Returns the translated text.
            """)
    public String translate(
            @ToolParam(description = "The text to translate")
            String text,
            @ToolParam(description = "Source language ISO 639-1 code (e.g., en, es, zh)")
            String sourceLang,
            @ToolParam(description = "Target language ISO 639-1 code (e.g., en, es, zh)")
            String targetLang) {
        try {
            log.info("translate: {}->{}, text length={}", sourceLang, targetLang, text.length());
            return translationService.translate(text, sourceLang, targetLang);
        } catch (Exception e) {
            log.error("translate failed: {}", e.getMessage());
            return text; // fallback: return original text
        }
    }
}
