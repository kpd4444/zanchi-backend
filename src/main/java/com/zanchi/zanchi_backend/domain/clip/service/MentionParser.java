package com.zanchi.zanchi_backend.domain.clip.service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MentionParser {
    // @[이름](id:123)
    private static final Pattern P = Pattern.compile("@\\[([^\\]]+)]\\(id:(\\d+)\\)");

    private MentionParser(){}

    public static List<Long> parseIdsFromCaption(String caption) {
        if (caption == null || caption.isBlank()) return List.of();
        Matcher m = P.matcher(caption);
        List<Long> ids = new ArrayList<>();
        while (m.find()) {
            try {
                ids.add(Long.parseLong(m.group(2)));
            } catch (NumberFormatException ignore) {}
        }
        return ids;
    }
}