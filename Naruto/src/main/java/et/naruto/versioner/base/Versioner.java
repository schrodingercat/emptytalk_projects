package et.naruto.versioner.base;


public class Versioner {
    private volatile Versionable versionable;
    public Versioner() {
        this.versionable=new Versionable();
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
    public final <RET> RET Fetch(final Handleable<RET> handleable) {
        Handleable<RET> r=handleable.Output(this.versionable);
        if(r!=null) {
            this.versionable=r.versionable;
            return r.result;
        }
        return null;
    }
}

