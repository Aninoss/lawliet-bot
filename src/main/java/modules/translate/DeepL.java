package modules.translate;

import com.deepl.api.*;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DeepL {

    private static final Translator translator = new Translator(System.getenv("DEEPL_AUTH_KEY"));

    private static final LoadingCache<LanguageType, List<Language>> languageCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<>() {
                @Override
                public List<Language> load(@NotNull LanguageType languageType) throws Exception {
                    return translator.getLanguages(languageType).stream()
                            .sorted(Comparator.comparing(Language::getName))
                            .collect(Collectors.toList());
                }
            });

    private static final LoadingCache<TranslationRequest, TextResult> translationCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<>() {
                @Override
                public TextResult load(@NotNull TranslationRequest translationRequest) throws Exception {
                    TextTranslationOptions textTranslationOptions = new TextTranslationOptions();
                    if (translationRequest.formality != null) {
                        textTranslationOptions.setFormality(translationRequest.formality);
                    }
                    return translator.translateText(translationRequest.textSource, translationRequest.sourceLanguageCode, translationRequest.targetLanguageCode, textTranslationOptions);
                }
            });

    public static Language getSourceLanguage(String languageCode) throws Exception {
        return getSourceLanguages().stream().filter(l -> l.getCode().equals(languageCode)).findFirst().orElse(null);
    }

    public static List<Language> getSourceLanguages() throws Exception {
        return languageCache.get(LanguageType.Source);
    }

    public static Language getTargetLanguage(String languageCode) throws Exception {
        return getTargetLanguages().stream().filter(l -> languageCode != null && (l.getCode().startsWith(languageCode) || languageCode.startsWith(l.getCode()))).findFirst().orElse(null);
    }

    public static List<Language> getTargetLanguages() throws Exception {
        return languageCache.get(LanguageType.Target);
    }

    public static TextResult translate(String textSource, String sourceLanguageCode, String targetLanguageCode, Formality formality) throws ExecutionException {
        return translationCache.get(new TranslationRequest(textSource, sourceLanguageCode, targetLanguageCode, formality));
    }


    private static class TranslationRequest {

        private final String textSource;
        private final String sourceLanguageCode;
        private final String targetLanguageCode;
        private final Formality formality;

        public TranslationRequest(String textSource, String sourceLanguageCode, String targetLanguageCode, Formality formality) {
            this.textSource = textSource;
            this.sourceLanguageCode = sourceLanguageCode;
            this.targetLanguageCode = targetLanguageCode;
            this.formality = formality;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TranslationRequest that = (TranslationRequest) o;
            return Objects.equals(textSource, that.textSource) && Objects.equals(sourceLanguageCode, that.sourceLanguageCode) && Objects.equals(targetLanguageCode, that.targetLanguageCode) && formality == that.formality;
        }

        @Override
        public int hashCode() {
            return Objects.hash(textSource, sourceLanguageCode, targetLanguageCode, formality);
        }

    }

}
