package et.naruto.versioner;


public class Handler<RET> {
    public final Versioner versioner;
    public volatile RET result=null;
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
    public final void Add(final RET ret) {
        this.result=ret;
        this.versioner.Add();
    }
    public final boolean Assign(final RET ret,final Versioner versioner) {
        if(this.versioner.Watch(versioner)) {
            this.result=ret;
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
    public static interface IMap<SRC,RET> {
        public RET map(final SRC src);
        public void close(final RET ret);
    }
    public final <SRC> boolean Map(final IMap<SRC,RET> map,final Handler<SRC> handler) {
        if(this.versioner.Watch(handler.versioner)) {
            map.close(this.result);
            RET temp=map.map(handler.result);
            if(temp!=null) {
                this.result=temp;
                return true;
            }
        }
        return false;
    }
    public final RET Output(final Versioner versioner) {
        if(versioner.Watch(this.versioner)) {
            return this.result;
        }
        return null;
    }
}
