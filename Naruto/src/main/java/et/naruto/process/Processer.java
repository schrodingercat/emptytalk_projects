package et.naruto.process;

import java.util.concurrent.atomic.AtomicLong;

import et.naruto.versioner.Flow;
import et.naruto.versioner.Handler;


public abstract class Processer<REQ,RET> {
    private final Flow<REQ,RET> flow;
    protected final ZKProcess zkprocess;
    public Processer(final ZKProcess zkprocess,final REQ req) {
        this.zkprocess=zkprocess;
        this.flow=new Flow(req);
        
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
    public final Handler<RET> handler() {
        return flow.handler;
    }
    /*public final RET Fetch() {
        return this.flow.Fetch();
    }*/
    
    public final void ReRequest() {
        this.flow.AddIn();
        this.zkprocess.Tick();
    }
    protected final boolean Do() {
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
