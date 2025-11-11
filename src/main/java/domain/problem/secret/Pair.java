package domain.problem.secret;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 불변 Pair (Tuple2)
 *
 * @type <A> 첫 번째 값의 타입
 * @type <S> 두 번째 값의 타입
 */
public record Pair<A, S>(A first, S second) {
}
