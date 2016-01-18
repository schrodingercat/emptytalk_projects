package et.naruto.versioner;

import et.naruto.base.Util.DIAG;
import et.naruto.versioner.base.Handleable;
import et.naruto.versioner.base.Handler;
import et.naruto.versioner.base.Versioner;


public class Flow<REQ,RET> {
    public final REQ request;
    private final Versioner in;
    private final Versioner doing;
    private final Handler<RET> out;
    private Flow(final REQ request,final Versioner in,final Versioner doing,final Handler<RET> out) {
        this.request=request;
        this.in=in;
        this.doing=doing;
        this.out=out;
    }
    public Flow(final REQ req) {
        this(req,new Versioner(),new Versioner(),new Handler());
    }
    public final Flow<REQ,RET> cont(final REQ req) {
        return new Flow(req,this.in,this.doing,this.out);
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
        if(in.version()>out.version()) {
            return true;
        }
        return false;
    }
    public final void Out(final RET ret) {
        if(!this.out.Assign(ret,doing.versionable())) {
            DIAG.Get.d.Error(this.toString());
        }
    }
    public final Handleable<RET> out_handleable() {
        return out.handleable();
    }
    public final RET out_result() {
        return out.result();
    }
}
