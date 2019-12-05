package org.example.funcout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OutcomeTests {

    @Test
    public void givenSuccess_whenProcessed_thenSuccessEffect() {
        Outcome<String, Integer> outcome = Outcome.asSuccess("");

        assertEquals("OK", Outcome.Effect.of(outcome)
                .onSuccess(v -> true, v -> "OK")
                .onFailure(f -> f.equals(1), f -> "ERROR 1")
                .onFailure(f -> f.equals(2), f -> "ERROR 2")
                .onFailure(f -> f.equals(3), f -> "ERROR 3")
                .get());
    }

    @Test
    public void givenFailure_whenProcessed_thenCorrectErrorEffect() {
        Outcome<Void, Integer> outcome = Outcome.asFailure(2);

        assertEquals("ERROR 2", Outcome.Effect.of(outcome)
                .onSuccess(v -> true, v -> "OK")
                .onFailure(f -> f.equals(1), f -> "ERROR 1")
                .onFailure(f -> f.equals(2), f -> "ERROR 2")
                .onFailure(f -> f.equals(3), f -> "ERROR 3")
                .get());
    }

    @Test
    public void testRepackProductionOfEnvelopeChain() {
        Outcome<Boolean, String> failedOutcome = Outcome.<Integer, String>asSuccess(1)
                .flatMap(v -> Outcome.asSuccess(v))
                .flatMap(v -> false ? Outcome.asSuccess(true) : Outcome.asFailure("abc"));

        assertTrue(failedOutcome.isFailure());
        assertFalse(failedOutcome.isSuccess());
        assertEquals("abc", failedOutcome.failure());

        Outcome<Boolean, String> successfulOutcome = Outcome.<Integer, String>asSuccess(1)
                .flatMap(v -> Outcome.asSuccess(v))
                .flatMap(v -> true ? Outcome.asSuccess(true) : Outcome.asFailure("abc"));

        assertTrue(successfulOutcome.isSuccess());
        assertFalse(successfulOutcome.isFailure());
        assertEquals(true, successfulOutcome.success());
    }

    @Test
    public void givenNestedSuccessfulOutcome_thenCorrectSuccess() {
        Outcome<String, String> outcome = Outcome.<Object, String>asSuccess(new Object())
                .flatMap(v -> nestedSuccessfulOutcome(2));

        assertFalse(outcome.isFailure());
        assertEquals("2", outcome.success());
    }


    private static Outcome<String, String> nestedSuccessfulOutcome(int value) {
        return Outcome.<Integer, String>asSuccess(value)
                .flatMap(v -> Outcome.asSuccess(String.valueOf(v)));
    }

    @Test
    public void givenNestedFailedOutcome_thenCorrectFailure() {
        Outcome<Void, String> outcome = Outcome.<Object, String>asSuccess(new Object())
                .flatMap(v -> nestedFailedOutcome())
                .flatMap(v -> Outcome.asFailure("ERROR"));

        assertTrue(outcome.isFailure());
        assertEquals("ERROR 5", outcome.failure());
    }

    private static Outcome<Void, String> nestedFailedOutcome() {
        return Outcome.<Integer, String>asSuccess(1)
                .flatMap(v -> Outcome.asSuccess(String.valueOf(v)))
                .flatMap(v -> Outcome.asFailure("ERROR 5"));
    }

    @Test
    public void givenNestedResult_assertErrorResultRepackFlatten() {
        Outcome<Void, String> outcome = Outcome.<Object, String>asSuccess(new Object())
                .flatMap(v -> Outcome.asFailure(getNestedFailRepackResult().failure()));

        assertTrue(outcome.isFailure());
        assertEquals("ERROR 5", outcome.failure());
        assertFalse(outcome.isSuccess());
    }

    private static Outcome<String, String> getNestedFailRepackResult() {
        return Outcome.<Integer, String>asSuccess(1)
                .flatMap(v -> Outcome.asSuccess(String.valueOf(v)))
                .flatMap(v -> Outcome.asFailure("ERROR 5"));
    }

    @Test
    public void givenMultipleChainedNestedResults_assertExecutionStopsOnFirstFail() {
        Outcome<Integer, String> outcome = Outcome.<Integer, String>asSuccess(1)
                .flatMap(v -> nestedSuccessfulOutcome(2))
                .flatMap(v -> nestedFailedOutcome())
                .flatMap(v -> failingResult())
                .flatMap(v -> Outcome.asFailure(failingResult().failure()));
        assertTrue(outcome.isFailure());
        assertFalse(outcome.isSuccess());
    }

    private static Outcome<String, String> failingResult() {
        return Outcome.<String, String>asFailure("")
                .flatMap(v -> {
                    throw new RuntimeException();
                });
    }

    @Test
    public void givenMultipleNestedSuccessFlatCalls_asservValueAffectedByAll() {
        Outcome<Long, String> outcome = Outcome.<Integer, String>asSuccess(20)
                .flatMap(v -> okStep1(v))
                .flatMap(v -> okStep2(v))
                .flatMap(v -> okStep3(v));

        assertFalse(outcome.isFailure());
        assertEquals(40, outcome.success().longValue());
    }
    private static Outcome<Integer, String> okStep1(int value) {
        return Outcome.<Integer, String>asSuccess(value)
                .flatMap(v -> Outcome.asSuccess(v * 2));
    }

    private static Outcome<String, String> okStep2(int value) {
        return Outcome.<Integer, String>asSuccess(value)
                .flatMap(v -> Outcome.asSuccess(String.valueOf(v)));
    }

    private static Outcome<Long, String> okStep3(String value) {
        return Outcome.<String, String>asSuccess(value)
                .flatMap(v -> Outcome.asSuccess(Long.parseLong(v)));
    }

    @Test
    public void givenVoidSuccess_whenEvaluating_thenCorrect() {
        Outcome<Void, String> outcome = Outcome.asSuccess(null);

        Outcome<Integer, String> outcome2 = outcome
                .flatMap(v -> Outcome.asSuccess(1));

        Outcome<Void, String> outcome3 = outcome2
                .flatMap(v -> Outcome.asSuccess(null));

        assertTrue(outcome.isSuccess());
        assertEquals(null, outcome.success());

        assertTrue(outcome2.isSuccess());
        assertEquals(1, outcome2.success().intValue());

        assertTrue(outcome3.isSuccess());
        assertEquals(null, outcome3.success());
    }
}
