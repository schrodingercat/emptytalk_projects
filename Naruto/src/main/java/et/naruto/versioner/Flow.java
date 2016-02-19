package et.naruto.versioner;

import et.naruto.base.Util.DIAG;
import et.naruto.versioner.base.Handleable;
import et.naruto.versioner.base.Handler;
import et.naruto.versioner.base.Versioner;


public class Flow<REQ,RET> {
    private final Handler<REQ> in=new Handler();
    private final Dealer<RET> out=new Dealer();
    public Flow() {
    }
    public final REQ in_request() {
        return this.in.result();
    }
    public final RET out_result() {
        return this.out.result();
    }
    public final Handleable<RET> out_result_handleable() {
        return out.result_handleable();
    }
    public final boolean doing() {
        if(this.out.result_handleable().versionable.IsFallBehind(this.in.versionable())) {
            return true;
        }
        return false;
    }
    public final void AddIn(REQ req) {
        this.in.Add(req);
    }
    public final void ReIn() {
        if(!doing()) {
            this.in.Add(this.in.result());
        }
    }
    public final Handleable<REQ> NeedDo() {
        Handleable<REQ> req=this.in.handleable();
        if(this.out.Watch(req)) {
            return req;
        } else {
            return null;
        }
    }
    public final void Done(final Handleable<RET> ret) {
        this.out.SyncDone(ret);
    }
}
