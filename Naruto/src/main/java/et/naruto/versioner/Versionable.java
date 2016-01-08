package et.naruto.versioner;

public class Versionable {
    protected volatile long version;
    public Versionable() {
        this.version=0;
    }
    public Versionable(final Versionable version) {
        this.version=version.version;
    }
    public final String toString() {
        return String.format("Version(%s)",version);
    }
    public final long version() {
        return this.version;
    }
}
