package et.naruto.versioner;

import et.naruto.base.Util.DIAG;
import et.naruto.versioner.base.Handleable;
import et.naruto.versioner.base.Handler;
import et.naruto.versioner.base.Versionable;
import et.naruto.versioner.base.Versioner;


public class Dealer<RET> {
    private final Versioner watch;
    private final Handler<RET> result;
    public Dealer() {
        this.watch=new Versioner();
        this.result=new Handler();
    }
    public final RET result() {
        return result.result();
    }
    public final Versionable result_versionable() {
        return this.result.versionable();
    }
    public final Handleable<RET> result_handleable() {
        return this.result.handleable();
    }
    public final String toString() {
        return String.format(
            "Dealer(in=%s,out=%s,ret=%s)",
            watch.versionable().toRawString(),
            result.versionable().toRawString(),
            result.result()
        );
    }
    public final boolean Watch(final Versionable... versionables) {
        for(Versionable v:versionables) {
            if(v==result.versionable()) {
                DIAG.Log.d.Error(toString());
            }
        }
        return watch.Watch(versionables);
    }
    public final boolean Watch(final Handleable... handleables) {
        Versionable[] vs=new Versionable[handleables.length];
        int i=0;
        for(Handleable h:handleables) {
            if(h!=null) {
                if(h==result.handleable()) {
                    DIAG.Log.d.Error(toString());
                }
                vs[i]=h.versionable;
            } else {
                vs[i]=null;
            }
            i++;
        }
        return Watch(vs);
    }
    
    public static interface IMap<SRC,RET> {
        public RET map(final RET ret,final SRC src);
    }
    public final <SRC> boolean Map(final IMap<SRC,RET> map,final Handleable<SRC> handleable) {
        if(this.watch.Watch(handleable.versionable)) {
            RET ret=map.map(this.result.result(),handleable.result);
            if(ret!=null) {
                this.result.Add(ret);
                return true;
            }
        }
        return false;
    }
    
    public final void Done(final RET ret) {
        this.result.Add(ret);
    }
    public final RET SyncDone(final Handleable<RET> handleable) {
        return this.result.Sync(handleable);
    }
}
