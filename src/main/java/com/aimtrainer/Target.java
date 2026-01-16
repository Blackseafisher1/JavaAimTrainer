package com.aimtrainer;

import java.util.ArrayList;
import java.util.List;

public final class Target {

  

    public static enum Mode {  BOUNCE , RADIAL, SNIPER }

    public record Position(double x, double y) {}

    private Position pos;
    private final double radius;
    private double vx;
    private double vy;
    private double friction;
    private Mode mode;
    private boolean alive = true;
    private boolean slug = false;

    public Target(Position pos, double radius, Mode mode) {
        this.pos = pos;
        this.radius = radius;
        this.mode = mode;
        this.vx = 0;
        this.vy = 0;
        this.friction = 0.99;
    }

    // --- Movement update (bounce only) ---
    public void update(double areaWidth, double areaHeight, Xorshift128Plus rng) {
        if (mode != Mode.BOUNCE) return;
    
    // === ADD SUBSTEPPING ===
    int substeps = 4;
    for (int i = 0; i < substeps; i++) {
        double subVx = vx / substeps;
        double subVy = vy / substeps;
        
        pos = new Position(pos.x() + subVx, pos.y() + subVy);
        
        boolean hitwall=false; 
        // Wall collisions
        if (pos.x() - radius < 0) { 
            pos = new Position(radius, pos.y()); 
            vx = -vx; 
            hitwall=true;
        } else if (pos.x() + radius > areaWidth) { 
            pos = new Position(areaWidth - radius, pos.y()); 
            vx = -vx; 
            hitwall=true;
        }
        
        if (pos.y() - radius < 0) { 
            pos = new Position(pos.x(), radius); 
            vy = -vy; 
            hitwall=true;
        } else if (pos.y() + radius > areaHeight) { 
            pos = new Position(pos.x(), areaHeight - radius); 
            vy = -vy; 
            hitwall=true;
        }
        if (hitwall) {
            com.aimtrainer.SoundManager.getInstance().playWallHit();
        }

    }
    
    vx *= friction;
    vy *= friction;
}

    // --- Factories and placement helpers ---
    private static Position randomInBounds(Xorshift128Plus rng, double width, double height, double margin, double radius) {
        double minX = margin + radius;
        double minY = margin + radius;
        double maxX = width - margin - radius;
        double maxY = height - margin - radius;
        if (maxX < minX || maxY < minY) return new Position(width/2.0, height/2.0);
        double x = minX + rng.nextDouble() * (maxX - minX);
        double y = minY + rng.nextDouble() * (maxY - minY);
        return new Position(x, y);
    }

    private static boolean overlaps(Position a, double ra, Position b, double rb) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        double r = ra + rb;
        return (dx*dx + dy*dy) < (r*r);
    }

    private static boolean hasOverlap(Position candidate, double radius, List<Target> targets) {
        for (Target t : targets) if (overlaps(candidate, radius, t.pos, t.radius)) return true;
        return false;
    }

    public static List<Target> createSniperTargets(Xorshift128Plus rng, double width, double height, double margin, double radius) {
        List<Target> list = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Position p = randomInBounds(rng, width, height, margin, radius);
            int tries = 0;
            while (hasOverlap(p, radius, list) && tries++ < 200) p = randomInBounds(rng, width, height, margin, radius);
            list.add(new Target(p, radius, Mode.SNIPER));
        }
        return list;
    }

    public static List<Target> createRadialTargets(Xorshift128Plus rng, double width, double height, double margin, double radius) {
        List<Target> list = new ArrayList<>();
        double centerX = width/2.0, centerY = height/2.0;
        double maxR = Math.max(0.0, Math.min(width, height)/2.0 - margin - radius);
        for (int i = 0; i < 3; i++) {
            Position p = null;
            int tries = 0;
            while (tries++ < 300) {
                double angle = rng.nextDouble() * 2.0 * Math.PI;
                double dist = rng.nextDouble() * maxR;
                p = new Position(centerX + Math.cos(angle)*dist, centerY + Math.sin(angle)*dist);
                if (!hasOverlap(p, radius, list)) break;
            }
            if (p == null) p = randomInBounds(rng, width, height, margin, radius);
            list.add(new Target(p, radius, Mode.RADIAL));
        }
        return list;
    }

    private static double speed = 20.0;
    public static Target createBounceTarget(Xorshift128Plus rng, double width, double height, double margin, double radius) {
        Position p = randomInBounds(rng, width, height, margin, radius);
        Target t = new Target(p, radius, Mode.BOUNCE);
      
        double angle = rng.nextDouble() * 2.0 * Math.PI;
        t.setVelocity(Math.cos(angle)*speed, Math.sin(angle)*speed);
        t.setFriction(0.995);
        return t;
    }

    public static Target createSlug(Xorshift128Plus rng, double width, double height, double largerMargin, double radius, List<Target> existing) {
        Position p = randomInBounds(rng, width, height, largerMargin, radius);
        int tries = 0;
        while (hasOverlap(p, radius, existing) && tries++ < 300) p = randomInBounds(rng, width, height, largerMargin, radius);
        Target slug = new Target(p, radius, Mode.BOUNCE);
        slug.setSlug(true);
        
        double angle = rng.nextDouble() * 2.0 * Math.PI;
        slug.setVelocity(Math.cos(angle)*speed, Math.sin(angle)*speed);
        slug.setFriction(0.995);
        return slug;
    }

    public static Target createSniperReplacement(Xorshift128Plus rng, double width, double height, double margin, double radius, List<Target> existing) {
        Position p = randomInBounds(rng, width, height, margin, radius);
        int tries = 0;
        while (hasOverlap(p, radius, existing) && tries++ < 300) p = randomInBounds(rng, width, height, margin, radius);
        return new Target(p, radius, Mode.SNIPER);
    }

    public static Target createRadialReplacement(Xorshift128Plus rng, double width, double height, double margin, double radius, List<Target> existing) {
        double centerX = width/2.0, centerY = height/2.0;
        double maxR = Math.max(0.0, Math.min(width, height)/2.0 - margin - radius);
        Position p = null;
        int tries = 0;
        while (tries++ < 400) {
            double angle = rng.nextDouble() * 2.0 * Math.PI;
            double dist = rng.nextDouble() * maxR;
            p = new Position(centerX + Math.cos(angle)*dist, centerY + Math.sin(angle)*dist);
            if (!hasOverlap(p, radius, existing)) break;
        }
        if (p == null) p = randomInBounds(rng, width, height, margin, radius);
        return new Target(p, radius, Mode.RADIAL);
    }

    // --- Accessors / mutators ---
    public void markHit() { alive = false; }
    public boolean isAlive() { return alive; }
    public Position pos() { return pos; }
    public double radius() { return radius; }
    public Mode mode() { return mode; }
    public void setMode(Mode m) { this.mode = m; }
    public void setVelocity(double vx, double vy) { this.vx = vx; this.vy = vy; }
    public void setFriction(double f) { this.friction = f; }
    public boolean isSlug() { return slug; }
    public void setSlug(boolean slug) { this.slug = slug; }
    public static double getSpeed(){return speed;}
    public static void  setSpeed(double pS){ speed =  Math.max(0.1, Math.min(200.0, pS));}    
    }

