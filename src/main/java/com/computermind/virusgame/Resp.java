package com.computermind.virusgame;

import com.computermind.sfp.Either;
import lombok.Getter;

@Getter
public class Resp<T> {
    final String error;
    final T success;

    private Resp(String error, T success) {
        this.error = error;
        this.success = success;
    }

    public static <T> Resp<T> error(String error) {
        return new Resp<>(error, null);
    }

    public static <T> Resp<T> success(T success) {
        return new Resp<>(null, success);
    }

    public static <T> Resp<T> from(Either<String, T> k) {
        return k.either(Resp::error, Resp::success);
    }
}
