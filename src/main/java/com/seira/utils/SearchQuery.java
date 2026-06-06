package com.seira.utils;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class SearchQuery {
    private final String keywords;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public SearchQuery(String query) {
        String DATE_PATTERN = "(\\d{4})(?:-(\\d{2})(?:-(\\d{2}))?)?";
        {   // parse from:2026-06-06
            Pattern regex = Pattern.compile("from:\\s?" + DATE_PATTERN);
            AtomicReference<LocalDate> startDate = new AtomicReference<>();

            query = regex.matcher(query).replaceAll(match -> {
                int year, month, day;
                year = Integer.parseInt(match.group(1));
                if (match.group(2) == null)
                    month = 1;
                else
                    month = Integer.parseInt(match.group(2));
                if (match.group(3) == null)
                    day = 1;
                else
                    day = Integer.parseInt(match.group(3));

                startDate.set(LocalDate.of(year, month, day));
                return "";
            });
            this.startDate = startDate.get();
        }
        {   // parse to:2026-06-06
            Pattern regex = Pattern.compile("to:\\s?" + DATE_PATTERN);
            AtomicReference<LocalDate> endDate = new AtomicReference<>();

            query = regex.matcher(query).replaceAll(match -> {
                int year, month, day;
                year = Integer.parseInt(match.group(1));
                if (match.group(2) == null)
                    month = 12;
                else
                    month = Integer.parseInt(match.group(2));
                if (match.group(3) == null)
                    day = Month.of(month).length(Year.isLeap(year));
                else
                    day = Integer.parseInt(match.group(3));

                endDate.set(LocalDate.of(year, month, day));
                return "";
            });
            this.endDate = endDate.get();
        }
        // IDEA: also scan the reference numbers?

        // whatever is left will become the keywords
        this.keywords = query.replaceAll("\\s+", " ");
    }

    public LocalDate getEndDate() {
        return endDate;
    }
    public LocalDate getStartDate() {
        return startDate;
    }
    public String getKeywords() {
        return keywords;
    }
}
