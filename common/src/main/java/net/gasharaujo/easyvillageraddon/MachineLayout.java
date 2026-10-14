package net.gasharaujo.easyvillageraddon;

/** Screen geometry shared with the actual inventory slots. */
public final class MachineLayout {
    private MachineLayout() { }
    public static boolean picker(MachineKind kind, int pane) { return pane == 1 && (kind == MachineKind.ADVANCED_TRADER || kind == MachineKind.LEVELER); }
    public static int width(MachineKind kind, int pane) { return picker(kind, pane) || kind == MachineKind.ADVANCED_TRADER && pane == 0 ? 276 : 176; }
    public static int height(MachineKind kind, int pane) {
        if (picker(kind, pane)) return 212;
        return switch (kind) { case DESNEURALIZER -> 164; case LEVELER -> 224; default -> 166; };
    }
    public static int inventoryX(MachineKind kind, int pane) { return kind == MachineKind.ADVANCED_TRADER && pane == 0 ? 107 : 8; }
    public static int inventoryY(MachineKind kind, int pane) { return height(kind, pane) - 82; }
}
