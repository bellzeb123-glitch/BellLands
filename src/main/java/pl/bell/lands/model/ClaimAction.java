package pl.bell.lands.model;

/**
 * Distinct actions a player can attempt on a claim. Used both for guest permissions
 * and for per-trusted permissions (named claims, via the Pro addon).
 */
public enum ClaimAction {
    BUILD,        // place / break blocks
    DOORS,        // doors, gates, trapdoors
    CONTAINERS,   // chests, barrels, shulkers, hoppers (inventory access)
    USE,          // other interactive blocks (buttons, levers, workstations…)
    ANIMALS,      // mounting / leashing / interacting with animals & vehicles
    FRAMES        // taking from / rotating item frames, armor stands, paintings
}
