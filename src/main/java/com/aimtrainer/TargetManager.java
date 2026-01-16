package com.aimtrainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal manager that delegates placement and spawn logic to Target factories.
 * Keeps the RNG instance for consistent randomness across the app.
 */
public class TargetManager {
    private final Xorshift128Plus rng;

    public TargetManager(Xorshift128Plus rng) {
        this.rng = rng;
    }

    /**
     * Spawn targets for a mode by delegating to Target factory methods.
     */
    public List<Target> spawnForMode(
        Target.Mode mode,
        double width, double height,
        double margin, double radius
    ) {
        List<Target> list = new ArrayList<>();
        switch (mode) {
            case SNIPER -> list.addAll(Target.createSniperTargets(rng, width, height, margin, radius));
            case RADIAL -> list.addAll(Target.createRadialTargets(rng, width, height, margin, radius));
            case BOUNCE -> list.add(Target.createBounceTarget(rng, width, height, margin, radius));
        }
        return list;
    }

    /**
     * Spawn a slug after a bounce target is hit.
     */
    public Target spawnSlug(
        double width, double height,
        double largerMargin,
        double radius,
        List<Target> existing
    ) {
        return Target.createSlug(rng, width, height, largerMargin, radius, existing);
    }

    /**
     * Expose RNG if controller or other systems need it.
     */
    public Xorshift128Plus rng() { return rng; }
}
