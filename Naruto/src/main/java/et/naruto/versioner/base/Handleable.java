package et.naruto.versioner.base;


public class Handleable<RET> {
    public final Versionable versionable;
    public final RET result;
    public Handleable() {
        this.versionable=new Versionable();
        this.result=null;
    }
    public Handleable(final RET result,final Versionable versionable) {
        this.result=result;
        this.versionable=versionable;
    }
    /*public final long version() {
        return this.versionable.version;
    }*/
    public final String toString() {
        return String.format("Handleable(%s,ret=%s)",versionable.toRawString(),result);
    }
    public final Handleable<RET> Output(final Versionable versionable) {
        Versionable v=versionable.Watch(this.versionable);
        if(v!=null) {
            return this;
        }
        return null;
    }
    public static final <RET> Handleable<RET> Add(final RET ret,final Versionable versionable) {
        return new Handleable(ret,versionable.Add());
    }
    public static final <RET> Handleable<RET> Watch(final RET ret,final Versionable versionable_this,final Versionable versionable_object) {
        Versionable v=versionable_this.Watch(versionable_object);
        if(v!=null) {
            return new Handleable(ret,v);
        }
        return null;
    }
}
