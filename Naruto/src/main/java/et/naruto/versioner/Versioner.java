package et.naruto.versioner;

public class Versioner {
    private volatile long version;
    public Versioner() {
        this.version=0;
    }
    public Versioner(final Versioner versioner) {
        this.version=versioner.version;
    }
    public final String toString() {
        return String.format("Versioner(%s)",version);
    }
    public final long version() {
        return this.version;
    }
    public void Add() {
        this.version++;
        if(this.version>1000) {
            int i=0;
            i++;
        }
    }
    public boolean Watch(final Versioner... versioners) {
        long t_version=0;
        for(Versioner v:versioners) {
            if(v!=null) {
                t_version+=v.version;
            }
        }
        if(t_version>this.version) {
            if(t_version>1000) {
                int i=0;
                i++;
            }
            this.version=t_version;
            return true;
        }
        return false;
    }
}

