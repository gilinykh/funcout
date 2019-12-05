package org.example.funcout;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class Outcome<R, F> {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<R> success;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<F> failure;

    private static <R, F> Outcome<R, F> asVoid() {
        return new Outcome<R, F>(null, null) {
            @Override
            public boolean isSuccess() {
                return true;
            }

            @Override
            public R success() {
                return null;
            }
        };
    }

    public static <R, F> Outcome<R, F> asSuccess(R success) {
        if (success == null) {
            return asVoid();
        }
        return new Outcome<>(success, null);
    }

    public static <R, F> Outcome<R, F> asFailure(F failure) {
        return new Outcome<>(null, failure);
    }

    private Outcome(R success, F failure) {
        this.success = Optional.ofNullable(success);
        this.failure = Optional.ofNullable(failure);
    }

    public boolean isSuccess() {
        return success.isPresent();
    }

    public boolean isFailure() {
        return failure.isPresent();
    }

    public R success() {
        if (!success.isPresent()) {
            throw new NoSuchElementException("No success value present!");
        }
        return success.get();
    }

    public F failure() {
        if (!failure.isPresent()) {
            throw new NoSuchElementException("No failure value present!");
        }
        return failure.get();
    }

    public <R1> Outcome<R1, F> flatMap(Function<R, ? extends Outcome<? extends R1, F>> mapper) {
        Objects.requireNonNull(mapper);
        if (isFailure()) {
            return new Outcome<>(null, this.failure());
        } else {
            @SuppressWarnings("unchecked")
            Outcome<R1, F> r = (Outcome<R1, F>) mapper.apply(this.success());
            return Objects.requireNonNull(r);
        }
    }

    public R orElse(R other) {
        return this.success.orElse(other);
    }

    public <T> T onSuccess(Function<R, T> mapper) {
        Objects.requireNonNull(mapper);
        return mapper.apply(success());
    }

    public <T> T onFailure(Function<F, T> mapper) {
        Objects.requireNonNull(mapper);
        return mapper.apply(failure());
    }

    public static class Effect<R, F, T> {
        private final Outcome<R, F> outcome;
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<Function<R, T>> successHandler;
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<Function<F, T>> failureHandler;

        public static <R, F, T> Outcome.Effect<R, F, T> of(Outcome<R, F> outcome) {
            return new Outcome.Effect<>(outcome, null, null);
        }

        private Effect(Outcome<R, F> outcome, Function<R, T> successHandler, Function<F, T> failureHandler) {
            this.outcome = Objects.requireNonNull(outcome);
            this.successHandler = Optional.ofNullable(successHandler);
            this.failureHandler = Optional.ofNullable(failureHandler);
        }

        private Effect<R, F, T> withSuccessHandler(Function<R, T> successHandler) {
            return new Effect<>(this.outcome, successHandler, this.failureHandler.orElse(null));
        }

        private Effect<R, F, T> withFailureHandler(Function<F, T> failureHandler) {
            return new Effect<>(this.outcome, this.successHandler.orElse(null), failureHandler);
        }

        public Effect<R, F, T> onSuccess(Predicate<R> successPredicate, Function<R, T> successHandler) {
            Objects.requireNonNull(successPredicate);
            Objects.requireNonNull(successHandler);
            if (this.outcome.isSuccess() && successPredicate.test(this.outcome.success())) {
                return this.withSuccessHandler(successHandler);
            } else {
                return this;
            }
        }

        public Effect<R, F, T> onFailure(Predicate<F> failurePredicate, Function<F, T> failureHandler) {
            Objects.requireNonNull(failurePredicate);
            Objects.requireNonNull(failureHandler);
            if (this.outcome.isFailure() && failurePredicate.test(this.outcome.failure())) {
                return this.withFailureHandler(failureHandler);
            } else {
                return this;
            }
        }

        public T orElse(T other) {
            if (this.successHandler.isPresent()) {
                return this.successHandler.get().apply(this.outcome.success());
            } else if (this.failureHandler.isPresent()) {
                return this.failureHandler.get().apply(this.outcome.failure());
            } else {
                return other;
            }
        }

        public T get() {
            if (this.successHandler.isPresent()) {
                return this.successHandler.get().apply(this.outcome.success());
            } else if (this.failureHandler.isPresent()) {
                return this.failureHandler.get().apply(this.outcome.failure());
            } else {
                throw new NoSuchElementException("Neither success nor failure value is present!");
            }
        }
    }
}
