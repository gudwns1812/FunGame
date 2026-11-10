package com.fungame.songquiz.domain.quiz;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record SongTitle(String name, List<String> aliases) {

    private static final Pattern VERSION_SUFFIX =
            Pattern.compile("\\s+-\\s+[^-]*\\bVer\\.?\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREDIT =
            Pattern.compile("\\s*\\(\\s*(?:feat|prod|narr|with)\\b[^()]*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALTERNATIVE = Pattern.compile("^(.*?\\S)\\s*\\(([^()]*)\\)$");
    private static final Pattern NESTED = Pattern.compile("^(.*?\\S)\\s*\\(.*\\)$");

    public SongTitle {
        aliases = List.copyOf(aliases);
    }

    public static SongTitle parse(String raw) {
        String title = normalize(raw);
        title = VERSION_SUFFIX.matcher(title).replaceFirst("");
        title = CREDIT.matcher(title).replaceAll("").strip();

        Matcher alternative = ALTERNATIVE.matcher(title);
        if (alternative.matches()) {
            return new SongTitle(alternative.group(1).strip(), List.of(alternative.group(2).strip()));
        }

        Matcher nested = NESTED.matcher(title);
        if (nested.matches()) {
            return new SongTitle(nested.group(1).strip(), List.of());
        }

        return new SongTitle(title, List.of());
    }

    private static String normalize(String raw) {
        return raw.replace('′', '\'')
                .replace('‘', '\'')
                .replace('’', '\'')
                .replace(' ', ' ')
                .replaceAll("\\s+", " ")
                .strip();
    }
}
