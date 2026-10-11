package com.mystipixel.econguard.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Stand-in for EconGuard's reflective bridge (same signature as EconGuard 2026.41), recording each call.
public final class EconGuard {

    public record Call(UUID player, String playerName, String source, String action, double amount,
                       boolean incoming, double balanceAfter, UUID counterparty, String counterpartyName,
                       String item, String note) {
    }

    public static final List<Call> CALLS = new ArrayList<>();

    private EconGuard() {
    }

    public static void record(UUID player, String playerName, String source, String action,
                              double amount, boolean incoming, double balanceAfter,
                              UUID counterparty, String counterpartyName, String item, String note) {
        CALLS.add(new Call(player, playerName, source, action, amount, incoming, balanceAfter,
                counterparty, counterpartyName, item, note));
    }
}
