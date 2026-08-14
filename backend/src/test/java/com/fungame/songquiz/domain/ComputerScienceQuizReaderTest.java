package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.ComputerScienceEntity;
import com.fungame.songquiz.storage.ComputerScienceRepository;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ComputerScienceQuizReaderTest {

    @Mock
    private ComputerScienceRepository computerScienceRepository;

    @InjectMocks
    private ComputerScienceQuizReader reader;

    private static ComputerScienceEntity quizOf(CSQuizDifficulty difficulty) {
        return ComputerScienceEntity.builder()
                .field("네트워크")
                .content("문제")
                .answers(List.of("답"))
                .explanation("해설")
                .difficulty(difficulty)
                .build();
    }

    @Test
    @DisplayName("고른 난이도 이하만 조회 조건으로 넘긴다.")
    void queriesOnlyChosenDifficultyAndEasier() {
        given(computerScienceRepository.findByDifficultyIn(any())).willReturn(List.of());

        reader.getRandomCSQuizWithCount(5, CSQuizDifficulty.NORMAL);

        ArgumentCaptor<Collection<CSQuizDifficulty>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(computerScienceRepository).findByDifficultyIn(captor.capture());
        assertThat(captor.getValue())
                .containsExactlyInAnyOrder(CSQuizDifficulty.EASY, CSQuizDifficulty.NORMAL);
    }

    @Test
    @DisplayName("후보가 넉넉하면 요청한 문제 수만큼 돌려준다.")
    void limitsToRequestedCount() {
        given(computerScienceRepository.findByDifficultyIn(any())).willReturn(List.of(
                quizOf(CSQuizDifficulty.EASY),
                quizOf(CSQuizDifficulty.EASY),
                quizOf(CSQuizDifficulty.NORMAL),
                quizOf(CSQuizDifficulty.NORMAL)
        ));

        assertThat(reader.getRandomCSQuizWithCount(3, CSQuizDifficulty.NORMAL)).hasSize(3);
    }

    @Test
    @DisplayName("후보가 요청한 문제 수보다 적으면 있는 만큼만 돌려준다.")
    void shrinksWhenCandidatesRunShort() {
        given(computerScienceRepository.findByDifficultyIn(any())).willReturn(List.of(
                quizOf(CSQuizDifficulty.EASY),
                quizOf(CSQuizDifficulty.EASY)
        ));

        assertThat(reader.getRandomCSQuizWithCount(10, CSQuizDifficulty.EASY)).hasSize(2);
    }

    @Test
    @DisplayName("엔티티의 모든 필드를 도메인으로 옮긴다.")
    void mapsEveryField() {
        given(computerScienceRepository.findByDifficultyIn(any())).willReturn(List.of(
                ComputerScienceEntity.builder()
                        .field("네트워크")
                        .content("TCP와 UDP의 차이는?")
                        .answers(List.of("연결성", "신뢰성"))
                        .explanation("TCP는 연결 지향이고 UDP는 비연결 지향이다.")
                        .difficulty(CSQuizDifficulty.NORMAL)
                        .build()
        ));

        ComputerScienceQuiz quiz = reader.getRandomCSQuizWithCount(1, CSQuizDifficulty.NORMAL).getFirst();

        assertThat(quiz.getField()).isEqualTo("네트워크");
        assertThat(quiz.getQuestion()).isEqualTo("TCP와 UDP의 차이는?");
        assertThat(quiz.getExplain()).isEqualTo("TCP는 연결 지향이고 UDP는 비연결 지향이다.");
        assertThat(quiz.getDifficulty()).isEqualTo(CSQuizDifficulty.NORMAL);
        assertThat(quiz.getAnswers()).containsExactly("연결성", "신뢰성");
    }

    @Test
    @DisplayName("조회 결과가 불변 리스트여도 섞다가 깨지지 않는다.")
    void survivesImmutableQueryResult() {
        given(computerScienceRepository.findByDifficultyIn(any())).willReturn(List.of(
                quizOf(CSQuizDifficulty.EASY),
                quizOf(CSQuizDifficulty.NORMAL),
                quizOf(CSQuizDifficulty.HARD)
        ));

        assertThat(reader.getRandomCSQuizWithCount(3, CSQuizDifficulty.HARD)).hasSize(3);
    }
}
