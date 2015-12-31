package et.naruto;


public class Flow<REQ,RET> extends FlowOut<RET> {
    public final REQ request;
    public Flow(final Flow flow,final REQ req) {
        super(flow);
        this.request=req;
    }
    public Flow(final REQ req) {
        super();
        this.request=req;
    }
    public final REQ NeedDoing() {
        if(super.Doing()) {
            return this.request;
        }
        return null;
    }
}
