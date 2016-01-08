package et.naruto.versioner;

import et.naruto.versioner.Versioner.IWatch;


public class Handler<RET> {
    private final Versioner versioner;
    private volatile RET result=null;
    public Handler() {
        this.versioner=new Versioner();
    }
    public Handler(final Handler<RET> handler) {
        this.versioner=new Versioner(handler.versioner);
        this.result=handler.result;
    }
    public final String toString() {
        return String.format("Handler(%s,ret=%s)",versioner.version(),result);
    }
    public final Versionable versionable() {
        return this.versioner;
    }
    public final long version() {
        return this.versioner.version();
    }
    public final RET result() {
        return this.result;
    }
    public final void Add(final RET ret) {
        this.result=ret;
        this.versioner.Add();
    }
    public final boolean Assign(final RET ret,final Versionable versionable) {
        final Handler<RET> handler_ref=this;
        if(this.versioner.Watch(
            new IWatch(){
                public void Do() {
                    handler_ref.result=ret;
                }
            },
            versionable
        )) {
            return true;
        }
        return false;
    }
    public final RET Sync(final Handler<RET> handler) {
        if(Assign(handler.result,handler.versioner)) {
            return handler.result;
        }
        return null;
    }
    public final RET Output(final Versioner versioner) {
        if(versioner.Watch(null,this.versioner)) {
            return this.result;
        }
        return null;
    }
}
