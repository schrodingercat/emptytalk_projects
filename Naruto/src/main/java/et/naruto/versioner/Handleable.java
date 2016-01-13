package et.naruto.versioner;

public class Handleable<RET> {
    public final Versionable versionable;
    public final RET result;
    public Handleable() {
        this.versionable=new Versionable();
        this.result=null;
    }
    private Handleable(final RET result,final Versionable versionable) {
        this.result=result;
        this.versionable=versionable;
    }
    public final long version() {
        return this.versionable.version;
    }
    public final String toString() {
        return String.format("Handleable(%s,ret=%s)",versionable.version,result);
    }
    public final Handleable<RET> Output(final Versionable versionable) {
        return Watch(versionable,this);
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
    public static final <RET> Handleable<RET> Watch(final Versionable versionable,final Handleable<RET> handle) {
        Versionable v=versionable.Watch(handle.versionable);
        if(v!=null) {
            return handle;
        }
        return null;
    }
}
