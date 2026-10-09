package com.fungame.songquiz.support.availability;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class TrafficStateFile {

    private static final String ACCEPTING = "true";
    private static final String REFUSING = "false";

    private final Optional<Path> location;

    private TrafficStateFile(Optional<Path> location) {
        this.location = location;
    }

    static TrafficStateFile at(String location) {
        if (location.isBlank()) {
            return new TrafficStateFile(Optional.empty());
        }
        return new TrafficStateFile(Optional.of(Path.of(location)));
    }

    Optional<Boolean> read() {
        return location.filter(Files::exists).flatMap(this::readFrom);
    }

    private Optional<Boolean> readFrom(Path path) {
        try {
            String content = Files.readString(path).strip();
            if (content.equals(ACCEPTING) || content.equals(REFUSING)) {
                return Optional.of(content.equals(ACCEPTING));
            }
            log.warn("트래픽 상태 파일 {} 의 내용 '{}' 을 알아볼 수 없다. 기동 설정을 따른다", path, content);
        } catch (IOException e) {
            log.warn("트래픽 상태 파일 {} 을 읽지 못했다. 기동 설정을 따른다", path, e);
        }
        return Optional.empty();
    }

    void write(boolean accepting) {
        location.ifPresent(path -> writeTo(path, accepting ? ACCEPTING : REFUSING));
    }

    private void writeTo(Path path, String content) {
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Path pending = Files.createTempFile(path.toAbsolutePath().getParent(), "accepting", ".tmp");
            Files.writeString(pending, content);
            Files.move(pending, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.error("트래픽 상태 {} 를 {} 에 남기지 못했다. 이 인스턴스가 다시 뜨면 기동 설정으로 돌아간다",
                    content, path, e);
        }
    }
}
