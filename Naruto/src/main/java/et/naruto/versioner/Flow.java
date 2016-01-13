package et.naruto.versioner;

import et.naruto.base.Util.DIAG;


public class Flow<REQ,RET> {
    public final REQ request;
    private final Versioner doing;
    private final Versioner in;
    public final Handler<RET> handler;
    public Flow(final Flow flow,final REQ req) {
        this.doing=new Versioner(flow.doing);
        this.in=new Versioner(flow.in);
        this.handler=new Handler(flow.handler);
        this.request=req;
    }
    public Flow(final REQ req) {
        this.in=new Versioner();
        this.doing=new Versioner();
        this.handler=new Handler();
        this.request=req;
    }
    public final REQ NeedDoing() {
        if(this.doing.Watch(this.in.versionable())) {
            return this.request;
        }
        return null;
    }
    public final boolean AddIn() {
        if(!doing()) {
            this.in.Add();
            return true;
        }
        return false;
    }
    public final boolean doing() {
        if(in.version()>handler.version()) {
            return true;
        }
        return false;
    }
    public final void Out(final RET ret) {
        if(!this.handler.Assign(ret,doing.versionable())) {
            DIAG.Get.d.Error(this.toString());
        }
    }
    public final RET result() {
        return handler.result();
    }
}
