// SlugUtil.java
package in.mapmytour.blog.utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class SlugUtil {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("(^-|-$)");

    public static String generateSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        String slug = input.toLowerCase(Locale.ENGLISH);
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD);
        slug = WHITESPACE.matcher(slug).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = EDGE_HYPHENS.matcher(slug).replaceAll("");

        return slug;
    }

    public static String ensureUniqueSlug(String baseSlug, java.util.function.Function<String, Boolean> existsFunction) {
        String slug = baseSlug;
        int counter = 1;

        while (existsFunction.apply(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }
}