package com.fungame.songquiz.client.melon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class MelonPages {

    static final String CHART_URL = "https://www.melon.com/chart/index.htm";
    static final String DETAIL_URL = "https://www.melon.com/song/detail.htm?songId=";
    private static final String LIKES_URL = "https://www.melon.com/commonlike/getSongLike.json?contsIds=";

    private static final String RELEASE_DATE_LABEL = "발매일";
    private static final String GENRE_LABEL = "장르";
    private static final DateTimeFormatter RELEASE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MelonPages() {
    }

    record ChartRow(long songId, String title, List<String> singers) {
    }

    static List<ChartRow> chartRows(String html) {
        return Jsoup.parse(html).select("tr[data-song-no]").stream()
                .map(MelonPages::chartRow)
                .toList();
    }

    static String likesUrl(List<Long> songIds) {
        return LIKES_URL + songIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    static List<MelonChartEntry> chartEntries(String chartHtml, String likesJson) {
        return withLikes(chartRows(chartHtml), likesJson);
    }

    static List<MelonChartEntry> withLikes(List<ChartRow> rows, String likesJson) {
        Map<Long, Long> likes = likes(likesJson);

        return rows.stream()
                .map(row -> new MelonChartEntry(row.songId(), row.title(), row.singers(),
                        likes.getOrDefault(row.songId(), 0L)))
                .toList();
    }

    static MelonSongDetail detail(String html) {
        Map<String, String> fields = new HashMap<>();
        for (Element term : Jsoup.parse(html).select("div.meta dl.list > dt")) {
            Element description = term.nextElementSibling();
            if (description != null) {
                fields.put(term.text().strip(), description.text().strip());
            }
        }

        return new MelonSongDetail(releaseDate(fields.get(RELEASE_DATE_LABEL)), genres(fields.get(GENRE_LABEL)));
    }

    private static ChartRow chartRow(Element row) {
        Element titleCell = row.selectFirst("div.ellipsis.rank01");
        Element titleLink = titleCell == null ? null : titleCell.selectFirst("a");
        String title = titleLink != null ? titleLink.text() : titleCell == null ? "" : titleCell.text();

        List<String> singers = row.select("div.ellipsis.rank02 > a").stream()
                .map(Element::text)
                .toList();

        return new ChartRow(Long.parseLong(row.attr("data-song-no")), title.strip(), singers);
    }

    private static Map<Long, Long> likes(String json) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            Map<Long, Long> likes = new HashMap<>();
            root.path("contsLike").forEach(node -> likes.put(node.path("CONTSID").asLong(), node.path("SUMMCNT").asLong()));

            return likes;
        } catch (JsonProcessingException e) {
            throw new MelonUnavailableException("멜론 좋아요 응답을 읽지 못했다", e);
        }
    }

    private static LocalDate releaseDate(String text) {
        if (text == null) {
            throw new MelonUnavailableException("멜론 상세 페이지에 발매일이 없다");
        }

        try {
            return LocalDate.parse(text, RELEASE_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new MelonUnavailableException("멜론 발매일을 읽지 못했다: " + text, e);
        }
    }

    private static List<String> genres(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        return Arrays.stream(text.split(","))
                .map(String::strip)
                .filter(genre -> !genre.isEmpty())
                .toList();
    }
}
