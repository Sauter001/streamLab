package domain.problem.secret;


import java.util.function.Function;

sealed interface Either<L, R> {
    // Helper constructors
    static <L, R> Either<L, R> left(L value) {
        return new Left<>(value);
    }

    static <L, R> Either<L, R> right(R value) {
        return new Right<>(value);
    }

    // Functor: map (Right만 변환)
    default <R2> Either<L, R2> map(Function<R, R2> f) {
        return switch (this) {
            case Right<L, R>(var r) -> new Right<>(f.apply(r));
            case Left<L, R>(var l) -> new Left<>(l);
        };
    }

    // Monad: flatMap
    default <R2> Either<L, R2> flatMap(Function<R, Either<L, R2>> f) {
        return switch (this) {
            case Right<L, R>(var r) -> f.apply(r);
            case Left<L, R>(var l) -> new Left<>(l);
        };
    }

    // mapLeft (에러 변환)
    default <L2> Either<L2, R> mapLeft(Function<L, L2> f) {
        return switch (this) {
            case Left<L, R>(var l) -> new Left<>(f.apply(l));
            case Right<L, R>(var r) -> new Right<>(r);
        };
    }

    // getOrElse
    default R getOrElse(R defaultValue) {
        return switch (this) {
            case Right<L, R>(var r) -> r;
            case Left<L, R>(var l) -> defaultValue;
        };
    }

    // isRight / isLeft
    default boolean isRight() {
        return this instanceof Right<L, R>;
    }

    default boolean isLeft() {
        return this instanceof Left<L, R>;
    }

    record Left<L, R>(L value) implements Either<L, R> {
    }

    record Right<L, R>(R value) implements Either<L, R> {
    }
}