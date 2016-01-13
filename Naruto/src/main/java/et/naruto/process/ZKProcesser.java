package et.naruto.process;

import et.naruto.versioner.Flow;
import et.naruto.versioner.Handler;
import et.naruto.versioner.Versionable;

public abstract class ZKProcesser<REQ,RET> implements Processer {
    private final Flow<REQ,RET> flow;
    protected final ZKProcess zkprocess;
    public ZKProcesser(final ZKProcess zkprocess,final REQ req) {
        this.zkprocess=zkprocess;
        this.flow=new Flow(req);
        
        this.zkprocess.AddProcesser(this);
    }
    public ZKProcesser(final ZKProcess zkprocess,final ZKProcesser<REQ,RET> old,final REQ req) {
        this.zkprocess=zkprocess;
        this.flow=new Flow(old.flow,req);
        
        this.zkprocess.AddProcesser(this);
    }
    public void Close() {
        this.zkprocess.DelProcesser(this);
    }
    public final REQ request() {
        return this.flow.request;
    }
    public final RET result() {
        return this.flow.result();
    }
    public final Versionable result_versionable() {
        return flow.handler.versionable();
    }
    public final boolean doing() {
        return flow.doing();
    }
    public final Handler<RET> handler() {
        return flow.handler;
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
