package et.naruto.versioner;


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
        if(this.doing.Watch(this.in)) {
            return this.request;
        }
        return null;
    }
    public final void AddIn() {
        this.in.Add();
    }
    public final void Out(final RET ret) {
        this.handler.Add(ret);
    }
    public final RET result() {
        return handler.result;
    }
}
