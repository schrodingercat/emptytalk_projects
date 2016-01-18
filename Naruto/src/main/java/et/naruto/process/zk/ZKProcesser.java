package et.naruto.process.zk;

import et.naruto.process.base.Processer;
import et.naruto.versioner.Flow;
import et.naruto.versioner.base.Handleable;
import et.naruto.versioner.base.Versionable;

public abstract class ZKProcesser<REQ,RET> implements Processer {
    private final Flow<REQ,RET> flow;
    protected final ZKProcess zkprocess;
    public ZKProcesser(final ZKProcess zkprocess,final ZKProcesser<REQ,RET> old,final REQ req) {
        this.zkprocess=zkprocess;
        this.flow=(old==null)?new Flow(req):old.flow.cont(req);
        this.zkprocess.AddProcesser(this);
    }
    public void Close() {
        this.zkprocess.DelProcesser(this);
    }
    public final REQ request() {
        return this.flow.request;
    }
    public final RET result() {
        return this.flow.out_result();
    }
    public final Versionable result_versionable() {
        return flow.out_handleable().versionable;
    }
    public final boolean doing() {
        return flow.doing();
    }
    public final Handleable<RET> handleable() {
        return flow.out_handleable();
    }
    
    public final void ReRequest() {
        this.flow.AddIn();
        this.zkprocess.Tick();
    }
    public final boolean Do() {
        REQ req=flow.NeedDoing();
        if(req!=null) {
            return DoDo(req);
        }
        return false;
    }
    protected final void Done(final RET ret) {
        flow.Out(ret);
        this.zkprocess.Tick();
    }
    
    public abstract boolean DoDo(final REQ req);
    
}
