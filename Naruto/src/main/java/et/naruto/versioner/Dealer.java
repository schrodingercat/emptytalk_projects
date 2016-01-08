package et.naruto.versioner;

import et.naruto.base.Util.DIAG;


public class Dealer<RET> {
    private final Versioner in;
    public final Handler<RET> handler;
    public Dealer(final Dealer dealer) {
        this.in=new Versioner(dealer.in);
        this.handler=new Handler(dealer.handler);
    }
    public Dealer() {
        this.in=new Versioner();
        this.handler=new Handler();
    }
    public final RET result() {
        return handler.result();
    }
    public final Versionable result_versionable() {
        return this.handler.versionable();
    }
    public final String toString() {
        return String.format("Dealer(in=%s,out=%s,ret=%s)",in.version(),handler.version(),handler.result());
    }
    public final boolean Watch(final Versionable... versionables) {
        for(Versionable v:versionables) {
            if(v==handler.versionable()) {
                DIAG.Get.d.Error(toString());
            }
        }
        return in.Watch(null,versionables);
    }
    
    public static interface IMap<SRC,RET> {
        public RET map(final RET ret,final SRC src);
    }
    public final <SRC> boolean Map(final IMap<SRC,RET> map,final Handler<SRC> handler) {
        if(this.in.Watch(null,handler.versionable())) {
            RET ret=map.map(this.handler.result(),handler.result());
            if(ret!=null) {
                this.handler.Add(ret);
                return true;
            }
        }
        return false;
    }
    
    public final void Done(final RET ret) {
        this.handler.Add(ret);
    }
}
