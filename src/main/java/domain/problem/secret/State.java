package domain.problem.secret;

import java.util.function.Function;

/**
 * State Monad 구현
 * State<S, A> = S -> (A, S)
 * S = State 타입, A = 결과 타입
 */
class State<S, A> {
    private final Function<S, Pair<A, S>> runState;

    public State(Function<S, Pair<A, S>> runState) {
        this.runState = runState;
    }

    // State 계산 실행
    public Pair<A, S> run(S initialState) {
        return runState.apply(initialState);
    }

    // Functor: map
    public <B> State<S, B> map(Function<A, B> f) {
        return new State<>(s -> {
            Pair<A, S> result = runState.apply(s);
            return new Pair<>(f.apply(result.first()), result.second());
        });
    }

    // Monad: flatMap
    public <B> State<S, B> flatMap(Function<A, State<S, B>> f) {
        return new State<>(s -> {
            Pair<A, S> result = runState.apply(s);
            return f.apply(result.first()).run(result.second());
        });
    }

    // 현재 상태 읽기
    public static <S> State<S, S> get() {
        return new State<>(s -> new Pair<>(s, s));
    }

    // 상태 설정
    public static <S> State<S, Unit> put(S newState) {
        return new State<>(s -> new Pair<>(Unit.INSTANCE, newState));
    }

    // 상태 변환
    public static <S> State<S, Unit> modify(Function<S, S> f) {
        return new State<>(s -> new Pair<>(Unit.INSTANCE, f.apply(s)));
    }

    // Pure value (return)
    public static <S, A> State<S, A> pure(A value) {
        return new State<>(s -> new Pair<>(value, s));
    }
}
