package et.naruto;

class Flow {
    private long tri=0;
    private long doing=0;
    private long done=0;
    private long fetch=0;
    public Flow(long tri) {
        this.tri=tri;
    }
    public void AddTri() {
        tri++;
    }
    public boolean NeedDoing() {
        long t_in=tri;
        if(t_in>doing) {
            doing=t_in;
            return true;
        }
        return false;
    }
    public void Done() {
        done=doing;
    }
    public boolean NeedFetch() {
        long t_done=done;
        if(t_done>fetch) {
            fetch=t_done;
            return true;
        }
        return false;
    }
}

public abstract class Processer<REQ,RET> {
    private final Flow flow=new Flow(0);
    public final REQ request;
    public volatile RET result=null;
    public abstract boolean Tick(final ZKProcess zk);
    public Processer(REQ req) {
        this.request=req;
    }
    public void ReRequest() {
        this.flow.AddTri();
    }
    public REQ Do() {
        if(flow.NeedDoing()) {
            return request;
        }
        return null;
    }
    public void Done(final RET ret) {
        result=ret;
        flow.Done();
    }
    public RET Fetch() {
        if(flow.NeedFetch()) {
            return result;
        }
        return null;
    }
}
