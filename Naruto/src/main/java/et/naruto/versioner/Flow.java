package et.naruto.versioner;

import et.naruto.base.Util.DIAG;
import et.naruto.versioner.base.Handleable;
import et.naruto.versioner.base.Handler;
import et.naruto.versioner.base.Versioner;


public class Flow<REQ,RET> {
    private final Handler<REQ> in;
    private final Versioner doing;
    private final Handler<RET> out;
    private Flow(final Handler<REQ> in,final Versioner doing,final Handler<RET> out) {
        this.in=in;
        this.doing=doing;
        this.out=out;
    }
    public Flow() {
        this(new Handler(),new Versioner(),new Handler());
    }
    public final Handleable<REQ> NeedDoing() {
        Handleable<REQ> handleable=this.in.handleable();
        if(this.doing.Watch(handleable.versionable)) {
            return handleable;
        }
        return null;
    }
    public final void AddIn(REQ req) {
        this.in.Add(req);
    }
    public final void ReIn() {
        if(!doing()) {
            this.in.Add(this.in.result());
        }
    }
    public final boolean doing() {
        if(out.versionable().IsFallBehind(in.versionable())) {
            return true;
        }
        return false;
    }
    public final void Out(final Handleable<RET> ret) {
        this.out.Assign(ret);
    }
    public final Handleable<RET> out_handleable() {
        return out.handleable();
    }
    public final REQ in_request() {
        return in.result();
    }
    public final RET out_result() {
        return out.result();
    }
}
