package et.naruto;

import org.apache.zookeeper.AsyncCallback.StringCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;

public class ValueRegister extends Processer<ValueRegister.Request,Boolean> {
    public static class Request {
        public final String name;
        public final String want_value;
        public final CreateMode mode;
        public Request(final String name,final String want_value,final CreateMode mode) {
            this.name=name;
            this.want_value=want_value;
            this.mode=mode;
        }
    }
    private final Flow flow=new Flow(0);
    public ValueRegister(final Request req) {
        super(req);
        super.ReRequest();
    }
    public boolean Tick(final ZKProcess zk) {
        boolean changed=false;
        final ValueRegister value_register_ref=this;
        final Request req=value_register_ref.Do();
        if(req!=null) {
            zk.zk.create(
                value_register_ref.request.name,
                value_register_ref.request.want_value.getBytes(),
                Ids.OPEN_ACL_UNSAFE,
                value_register_ref.request.mode,
                new StringCallback() {
                    public void processResult(int rc, String path, Object ctx, String name) {
                        switch(rc) {
                        case 0:
                            value_register_ref.Done(true);
                            break;
                        default:
                            value_register_ref.Done(false);
                            break;
                        }
                    }
                },
                null
            );
            changed=true;
        }
        return changed;
    }
}
