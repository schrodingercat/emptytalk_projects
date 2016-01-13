package et.naruto.versioner;

public class Versionable {
    public final long version;
    public Versionable() {
        this.version=0;
    }
    private Versionable(long version) {
        this.version=version;
    }
    public final String toString() {
        return String.format("Version(%s)",version);
    }
    public final Versionable Add() {
        return new Versionable(this.version+1);
    }
    public final Versionable Watch(final Versionable... versionables) {
        long t_version=0;
        for(Versionable v:versionables) {
            if(v!=null) {
                t_version+=v.version;
            }
        }
        if(t_version>version) {
            if(versionables.length==1) {
                return versionables[0];
            } else {
                return new Versionable(t_version);
            }
        }
        return null;
    }
}
