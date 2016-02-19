package et.naruto.process.zk;

import et.naruto.process.base.Processer;
import et.naruto.versioner.Flow;
import et.naruto.versioner.base.Handleable;
import et.naruto.versioner.base.Versionable;


public abstract class ZKProcesser<REQ,RET> implements Processer {
    private final Flow<REQ,RET> flow=new Flow();
    protected final ZKProcess zkprocess;
    public ZKProcesser(final ZKProcess zkprocess) {
        this.zkprocess=zkprocess;
        this.zkprocess.AddProcesser(this);
    }
    public void Close() {
        this.zkprocess.DelProcesser(this);
    }
    public final REQ request() {
        return this.flow.in_request();
    }
    public final RET result() {
        return this.flow.out_result();
    }
    public final Versionable result_versionable() {
        return this.flow.out_result_handleable().versionable;
    }
    public final Handleable<RET> result_handleable() {
        return this.flow.out_result_handleable();
    }
    public final boolean doing() {
        return this.flow.doing();
    }
    public final void Request(REQ req) {
        this.flow.AddIn(req);
        this.zkprocess.Tick();
    }
    public final void ReRequest() {
        this.flow.ReIn();
        this.zkprocess.Tick();
    }
    public final boolean Do() {
        Handleable<REQ> req=this.flow.NeedDo();
        if(req!=null) {
            return DoDo(req);
        } else {
            return false;
        }
    }
    protected final void Done(final Handleable<RET> ret) {
        this.flow.Done(ret);
        this.zkprocess.Tick();
    }
    
    public abstract boolean DoDo(final Handleable<REQ> req);
    
}
