package et.naruto.versioner;

public class Versioner {
    private volatile Versionable versionable;
    public Versioner() {
        this.versionable=new Versionable();
    }
    public Versioner(final Versioner versioner) {
        this.versionable=versioner.versionable;
    }
    public void Add() {
        this.versionable=this.versionable.Add();
    }
    public boolean Watch(final Versionable... versionables) {
        final Versionable v=this.versionable.Watch(versionables);
        if(v!=null) {
            this.versionable=v;
            return true;
        }
        return false;
    }
    public final Versionable versionable() {
        return this.versionable;
    }
    public final long version() {
        return this.versionable.version;
    }
    /*public static interface IWatch {
        public void Do();
    }
    public boolean Watch(final IWatch watch,final Versionable... versionables) {
        long t_version=0;
        for(Versionable v:versionables) {
            if(v!=null) {
                t_version+=v.version;
            }
        }
        if(t_version>this.version) {
            if(t_version>1000) {
                int i=0;
                i++;
            }
            if(watch!=null) {
                watch.Do();
            }
            this.version=t_version;
            return true;
        }
        return false;
    }*/
}

