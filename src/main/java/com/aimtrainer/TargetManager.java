package com.aimtrainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Einfacher Manager, der Platzierungs- und Spawn-Logik an die Target-Fabriken delegiert.
 * Hält die RNG-Instanz für konsistente Zufallswerte in der Anwendung.
 */
public class TargetManager {
    private final Xorshift128Plus rng;

    public TargetManager(Xorshift128Plus rng) {
        this.rng = rng;
    }

    /**
     * Erzeugt Ziele für einen Modus, delegiert an die Target-Fabrikmethoden.
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
     * Erzeugt ein "Slug"-Ziel, wenn nach einem Bounce-Treffer ein Ersatz benötigt wird.
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
     * Gibt die RNG-Instanz frei, falls Controller oder andere Systeme sie benötigen.
     */
    public Xorshift128Plus rng() { return rng; }
}
