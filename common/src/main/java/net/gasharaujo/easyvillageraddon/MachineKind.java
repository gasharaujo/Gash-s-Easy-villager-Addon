package net.gasharaujo.easyvillageraddon;

public enum MachineKind {
    ADVANCED_TRADER("advanced_trader", "Advanced Trader"),
    DESNEURALIZER("desneuralizer", "Desneuralizer"),
    BRIBER("briber", "Briber"),
    LEVELER("leveler", "Leveler");

    public final String id;
    public final String title;

    MachineKind(String id, String title) {
        this.id = id;
        this.title = title;
    }
}
