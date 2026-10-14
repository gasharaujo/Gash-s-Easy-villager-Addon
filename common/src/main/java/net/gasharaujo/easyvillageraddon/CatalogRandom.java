package net.gasharaujo.easyvillageraddon;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

/** Enumerates bounded random choices without inventing a trade outside its factory. */
final class CatalogRandom implements RandomSource {
    private long choices;
    private final RandomSource fallback;
    private long combinations = 1;

    CatalogRandom(long variant) {
        choices = variant;
        fallback = RandomSource.create(variant * 6364136223846793005L + 1442695040888963407L);
    }

    @Override public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("Non-positive bound");
        int result = (int) (choices % bound);
        combinations = Math.min(4096, combinations * bound);
        choices /= bound;
        return result;
    }
    int combinations() { return (int) combinations; }
    @Override public RandomSource fork() { return fallback.fork(); }
    @Override public PositionalRandomFactory forkPositional() { return fallback.forkPositional(); }
    @Override public void setSeed(long seed) { fallback.setSeed(seed); }
    @Override public int nextInt() { return fallback.nextInt(); }
    @Override public long nextLong() { return fallback.nextLong(); }
    @Override public boolean nextBoolean() { return fallback.nextBoolean(); }
    @Override public float nextFloat() { return fallback.nextFloat(); }
    @Override public double nextDouble() { return fallback.nextDouble(); }
    @Override public double nextGaussian() { return fallback.nextGaussian(); }
}
