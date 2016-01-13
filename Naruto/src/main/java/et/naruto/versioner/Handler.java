package et.naruto.versioner;



public class Handler<RET> {
    private volatile Handleable<RET> handleable;
    public Handler() {
        this.handleable=new Handleable();
    }
    public Handler(final Handler<RET> handler) {
        this.handleable=handler.handleable;
    }
    public final String toString() {
        return String.format("Handler(%s,ret=%s)",handleable.versionable.version,handleable.result);
    }
    public final Versionable versionable() {
        return handleable.versionable;
    }
    public final long version() {
        return this.handleable.version();
    }
    public final RET result() {
        return handleable.result;
    }
    public final Handleable<RET> handleable() {
        return handleable;
    }
    public final void Add(final RET ret) {
        this.handleable=Handleable.Add(ret,this.handleable.versionable);
    }
    public final boolean Assign(final RET ret,final Versionable versionable) {
        Handleable h=Handleable.Watch(ret,this.handleable.versionable,versionable);
        if(h!=null) {
            this.handleable=h;
            return true;
        }
        return false;
    }
    public final RET Sync(final Handleable<RET> handleable) {
        Handleable<RET> h=Handleable.Watch(this.handleable.versionable,handleable);
        if(h!=null) {
            this.handleable=h;
            return h.result;
        }
        return null;
    }
    public final RET Output(final Versioner versioner) {
        if(versioner.Watch(this.handleable.versionable)) {
            return this.handleable.result;
        }
        return null;
    }
}
