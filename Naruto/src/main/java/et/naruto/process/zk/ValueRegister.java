package et.naruto.process.zk;

import java.nio.file.Paths;

import org.apache.zookeeper.AsyncCallback.StringCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;

import et.naruto.base.Util.DIAG;
import et.naruto.versioner.base.Handleable;

import static et.naruto.base.Util.Path2UnixStr;


public class ValueRegister extends ZKProcesser<ValueRegister.Request,ValueRegister.Result> {
    public static class Result {
        public final Boolean succ;
        public Result(final Boolean succ) {
            this.succ=succ;
        }
        public boolean ok() {
            if(succ!=null) {
                return succ;
            }
            return false;
        }
    }
    public static class Request {
        public final String name;
        public final byte[] want_value;
        public final CreateMode mode;
        public Request(final String name,final String want_value,final CreateMode mode) {
            this(name,want_value.getBytes(),mode);
        }
        public Request(final String name,final byte[] want_value,final CreateMode mode) {
            this.name=name;
            this.want_value=want_value;
            this.mode=mode;
        }
        public String GetRegisterName() {
            return Path2UnixStr(Paths.get(this.name).getFileName());
        }
    }
    public String toString() {
        return String.format("VR[name=%s]",this.request().GetRegisterName());
    }
    public ValueRegister(final ZKProcess zkprocess,final Request req) {
        super(zkprocess);
        if(req!=null) {
            super.Request(req);
        }
    }
    public void DoCallback(final Result result) {}
    public boolean DoDo(final Handleable<Request> req) {
        final ValueRegister value_register_ref=this;
        zkprocess.zk.create(
            req.result.name,
            req.result.want_value,
            Ids.OPEN_ACL_UNSAFE,
            req.result.mode,
            new StringCallback() {
                public void processResult(int rc, String path, Object ctx, String name) {
                    Result r=null;
                    switch(rc) {
                    case KeeperException.Code.Ok:
                        r=new Result(true);
                        break;
                    case KeeperException.Code.NodeExists:
                        r=new Result(false);
                        break;
                    default:
                        DIAG.Log.d.error(path+" : "+KeeperException.create(rc).getMessage());
                        r=new Result(null);
                        break;
                    }
                    value_register_ref.Done(new Handleable(r,req.versionable));
                    DoCallback(r);
                }
            },
            null
        );
        return true;
    }
}
