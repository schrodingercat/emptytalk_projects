package et.naruto.versioner.base;

import java.util.concurrent.atomic.AtomicInteger;

public class Versionable {
    private final int g;
    private final long version;
    private final static AtomicInteger g_count=new AtomicInteger(0);
    public Versionable() {
        this(0);
    }
    private Versionable(final long version) {
        this(g_count.getAndIncrement(),version);
    }
    private Versionable(final int g,final long version) {
        this.g=g;
        this.version=version;
    }
    public final String toString() {
        return String.format("Version(%s)",toRawString());
    }
    public final String toRawString() {
        return String.format("%s:%s",g,version);
    }
    private static final boolean IsFallBehind(final int this_g,final long this_v,final int other_g,final long other_v) {
        if(this_v<other_v) {
            return true;
        }
        if(this_g<other_g) {
            return true;
        }
        return false;
    }
    public final boolean IsFallBehind(final Versionable versionable) {
        return IsFallBehind(this.g,this.version,versionable.g,versionable.version);
    }
    public final Versionable Add() {
        return new Versionable(this.version+1);
    }
    public final Versionable Watch(final Versionable... versionables) {
        int t_g=0;
        long t_version=0;
        for(Versionable v:versionables) {
            if(v!=null) {
                t_g+=v.g;
                t_version+=v.version;
            }
        }
        if(IsFallBehind(this.g,this.version,t_g,t_version)) {
            if(versionables.length==1) {
                return versionables[0];
            } else {
                return new Versionable(t_g,t_version);
            }
        }
        return null;
    }
}
