package et.naruto.process;

import java.nio.file.Paths;

import org.apache.zookeeper.AsyncCallback.StringCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;


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
            return Paths.get(this.name).getFileName().toString();
        }
    }
    public String toString() {
        return String.format("VR[name=%s]",this.request().GetRegisterName());
    }
    public ValueRegister(final ZKProcess zkprocess,final Request req) {
        super(zkprocess,req);
        super.ReRequest();
    }
    public boolean DoDo(final Request req) {
        final ValueRegister value_register_ref=this;
        zkprocess.zk.create(
            req.name,
            req.want_value,
            Ids.OPEN_ACL_UNSAFE,
            req.mode,
            new StringCallback() {
                public void processResult(int rc, String path, Object ctx, String name) {
                    switch(rc) {
                    case KeeperException.Code.Ok:
                        value_register_ref.Done(new Result(true));
                        break;
                    case KeeperException.Code.NodeExists:
                        value_register_ref.Done(new Result(false));
                        break;
                    default:
                        value_register_ref.Done(new Result(null));
                        break;
                    }
                }
            },
            null
        );
        return true;
    }
    public static ValueRegister Change(final ZKProcess zkprocess,final ValueRegister current,final String next,final byte[] data) {
        boolean need_create=false;
        if(current!=null) {
            if(!current.request().name.equals(next)) {
                current.Close();
                need_create=true;
            }
        } else {
            need_create=true;
        }
        if(need_create) {
            ValueRegister vf=new ValueRegister(zkprocess,new ValueRegister.Request(next,data,CreateMode.PERSISTENT));
            return vf;
        }
        return null;
    }
}
