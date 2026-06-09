package com.seira.utils;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class SearchQuery {
    private final String keywords;
    private final List<String> references = new ArrayList<>();

    public SearchQuery(String query) {
        {   // parse #REF-(number)
            Pattern regex = Pattern.compile("#REF-(\\d*)");
            query = regex.matcher(query).replaceAll(match -> {
                references.add(match.group(1));
                return ""; // don't include this on the search query
            });
        }

        // whatever is left will become the keywords
        this.keywords = query.replaceAll("\\s+", " ");
    }

    public String getKeywords() {
        return keywords;
    }
    public List<String> getReferences() {
        return references;
    }
}
