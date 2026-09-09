package fr.faction.village;

public enum PostType {
    PORT, GARE;

    public String displayName() {
        return this == PORT ? "Port" : "Gare";
    }
}
