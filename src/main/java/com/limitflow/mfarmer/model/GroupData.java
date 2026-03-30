package com.limitflow.mfarmer.model;

import org.jetbrains.annotations.NotNull;

public record GroupData(@NotNull String permission, double price, int capacity) {
}
