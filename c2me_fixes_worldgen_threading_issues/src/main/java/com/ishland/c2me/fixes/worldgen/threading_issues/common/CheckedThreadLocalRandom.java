package com.ishland.c2me.fixes.worldgen.threading_issues.common;

import com.ishland.c2me.fixes.worldgen.threading_issues.common.debug.SMAPSourceDebugExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;

public class CheckedThreadLocalRandom extends SingleThreadedRandomSource {

    private static final Logger LOGGER = LoggerFactory.getLogger("CheckedThreadLocalRandom");

    static {
        if (Config.enforceSafeWorldRandomAccess) {
            LOGGER.warn("Safe world random access is ENFORCED - unsafe access will cause crashes");
        } else {
            LOGGER.info("Safe world random access is DISABLED - unsafe access will use fallback");
        }
    }

    private final Supplier<Thread> owner;
    private final long originalSeed;
    private boolean hasWarned = false;
    // Per-instance fallback that's thread-local
    private final ThreadLocal<SingleThreadedRandomSource> fallback = ThreadLocal.withInitial(() -> {
        // Use original seed mixed with thread name for deterministic but unique per-thread behavior
        long threadSeed = originalSeed ^ Thread.currentThread().getName().hashCode();
        return new SingleThreadedRandomSource(threadSeed);
    });

    public CheckedThreadLocalRandom(long seed, Supplier<Thread> owner) {
        super(seed);
        this.owner = Objects.requireNonNull(owner);
        this.originalSeed = seed;
    }

    private boolean isSafe() {
        Thread owner = this.owner != null ? this.owner.get() : null;
        return owner == null || Thread.currentThread() == owner;
    }

    private void handleNotOwner() {
        // Only warn once per instance to avoid log spam
        if (hasWarned) {
            return;
        }
        hasWarned = true;
        
        StringBuilder builder = new StringBuilder();
        final String exceptionMessage = "ThreadLocalRandom accessed from a different thread (owner: %s, current: %s)"
                .formatted(this.owner.get().getName(), Thread.currentThread().getName());
        builder.append(exceptionMessage).append('\n');
        builder.append("This is usually NOT a bug in C2ME, but a bug in another mod or in vanilla code. \n");
        builder.append("Possible solutions: \n");
        builder.append("  - Find possible causes in the stack trace below and \n");
        builder.append("    - if caused by another mod, report this to the corresponding mod authors \n");
        builder.append("    - if no other mods are involved, report this to C2ME \n");
        ConcurrentModificationException exception = new ConcurrentModificationException(exceptionMessage);
        try {
            SMAPSourceDebugExtension.enhanceStackTrace(exception, false);
        } catch (Throwable t) {
            LOGGER.error("Error occurred while processing error stack trace", t);
            exception = new ConcurrentModificationException(exceptionMessage);
        }

        final String s = builder.toString();
        LOGGER.error(s, exception);
        if (Config.enforceSafeWorldRandomAccess) {
            throw new RuntimeException(String.format("%s \n (You may make this a fatal warning instead of a hard crash with fixes.enforceSafeWorldRandomAccess setting in c2me.toml)", s), exception) {
                @Override
                public synchronized Throwable fillInStackTrace() {
                    return this;
                }
            };
        }
    }

    @Override
    public void setSeed(long seed) {
        if (isSafe()) {
            super.setSeed(seed);
        } else {
            // Log warning (but don't throw yet)
            handleNotOwner();
            // Use fallback with deterministic seed
            fallback.get().setSeed(seed);
        }
    }

    @Override
    public int next(int bits) {
        if (isSafe()) {
            return super.next(bits);
        } else {
            // Log warning (but don't throw yet)
            handleNotOwner();
            // Use fallback with deterministic seed
            return fallback.get().next(bits);
        }
    }
    
    // Override other common methods to ensure they also use the fallback
    @Override
    public boolean nextBoolean() {
        if (isSafe()) {
            return super.nextBoolean();
        } else {
            handleNotOwner();
            return fallback.get().nextBoolean();
        }
    }
    
    @Override
    public float nextFloat() {
        if (isSafe()) {
            return super.nextFloat();
        } else {
            handleNotOwner();
            return fallback.get().nextFloat();
        }
    }
    
    @Override
    public double nextDouble() {
        if (isSafe()) {
            return super.nextDouble();
        } else {
            handleNotOwner();
            return fallback.get().nextDouble();
        }
    }
    
    @Override
    public int nextInt() {
        if (isSafe()) {
            return super.nextInt();
        } else {
            handleNotOwner();
            return fallback.get().nextInt();
        }
    }
    
    @Override
    public int nextInt(int bound) {
        if (isSafe()) {
            return super.nextInt(bound);
        } else {
            handleNotOwner();
            return fallback.get().nextInt(bound);
        }
    }
    
    @Override
    public long nextLong() {
        if (isSafe()) {
            return super.nextLong();
        } else {
            handleNotOwner();
            return fallback.get().nextLong();
        }
    }
    
    @Override
    public double nextGaussian() {
        if (isSafe()) {
            return super.nextGaussian();
        } else {
            handleNotOwner();
            return fallback.get().nextGaussian();
        }
    }
}
