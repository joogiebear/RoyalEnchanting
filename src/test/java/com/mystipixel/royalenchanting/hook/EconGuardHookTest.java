package com.mystipixel.royalenchanting.hook;

import com.mystipixel.econguard.api.EconGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconGuardHookTest {

    public static final class Broken {
        private Broken() {
        }

        public static void record(UUID player, String playerName, String source, String action,
                                  double amount, boolean incoming, double balanceAfter,
                                  UUID counterparty, String counterpartyName, String item, String note) {
            throw new IllegalStateException("ledger closed");
        }
    }

    public static final class TooOld {
        private TooOld() {
        }
    }

    private final List<LogRecord> logged = new ArrayList<>();
    private final UUID player = UUID.randomUUID();

    @BeforeEach
    void clear() {
        EconGuard.CALLS.clear();
    }

    private Logger logger() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return logger;
    }

    @Test
    void jobPayIsReportedAsIncomingMoney() throws Exception {
        EconGuardHook hook = EconGuardHook.from(logger(), EconGuard.class);
        hook.reportJobPay(player, "Steve", 37.5);

        assertEquals(1, EconGuard.CALLS.size());
        EconGuard.Call call = EconGuard.CALLS.get(0);
        assertEquals(player, call.player());
        assertEquals("Steve", call.playerName());
        assertEquals("other", call.source());
        assertEquals("enchanting_job", call.action());
        assertEquals(37.5, call.amount());
        assertTrue(call.incoming());
        assertTrue(Double.isNaN(call.balanceAfter()));
        assertNull(call.counterparty());
        assertNull(call.item());
        assertTrue(logged.isEmpty());
    }

    @Test
    void aFailingReportNeverThrowsAndWarnsOnce() throws Exception {
        EconGuardHook hook = EconGuardHook.from(logger(), Broken.class);
        assertDoesNotThrow(() -> hook.reportJobPay(player, "Steve", 10));
        assertDoesNotThrow(() -> hook.reportJobPay(player, "Steve", 10));
        assertEquals(1, logged.size());
        assertEquals(Level.WARNING, logged.get(0).getLevel());
    }

    @Test
    void anEconGuardWithoutTheBridgeIsNotHooked() {
        assertThrows(NoSuchMethodException.class, () -> EconGuardHook.from(logger(), TooOld.class));
    }
}
