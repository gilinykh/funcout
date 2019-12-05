# funcout

Either monad implemented in Java for control flow processing in functional style.

Example: 

```Java
public void method1() {
    Outcome<Long, String> outcome = Outcome.<Integer, String>asSuccess(20)
            .flatMap(v -> step1(v))
            .flatMap(v -> step2(v))
            .flatMap(v -> step3(v));

    int result = Outcome.Effect.of(outcome)
                    .onSuccess(v -> true, v -> v * 10)
                    .onFailure(f -> f.equals("ERROR 1"), f -> -1)
                    .onFailure(f -> f.equals("ERROR 2"), f -> -10)
                    .onFailure(f -> f.equals("ERROR 3"), f -> -100)
                    .orElse(0);
                    
    // result == 400
}

private static Outcome<Integer, String> step1(int value) {
    return Outcome.<Integer, String>asSuccess(value)
            .flatMap(v -> Outcome.asSuccess(v * 2))
            .flatMap(v -> v < 100 ? Outcome.asSuccess(v) : Outcome.asFailure("ERROR 1"));
}

private static Outcome<String, String> step2(int value) {
    return Outcome.<Integer, String>asSuccess(value)
            .flatMap(v -> Outcome.asSuccess(String.valueOf(v)))
            .flatMap(v -> v.length() < 100 ? Outcome.asSuccess(v) : Outcome.asFailure("ERROR 2"));
}

private static Outcome<Long, String> step3(String value) {
    return Outcome.<String, String>asSuccess(value)
            .flatMap(v -> Outcome.asSuccess(Long.parseLong(v)))
            .flatMap(v -> v < 100L ? Outcome.asSuccess(v) : Outcome.asFailure("ERROR 3"));
}
```
