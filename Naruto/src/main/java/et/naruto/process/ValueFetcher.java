package et.naruto.process;

import java.nio.file.Paths;
import java.util.TimerTask;

import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;

import et.naruto.base.Util.DIAG;

public class ValueFetcher extends ZKProcesser<String,ValueFetcher.Result> {
    public class Result {
        public final byte[] data;
        public final String value;
        public final boolean ephemeral;
        public Result(final byte[] data,final String value,final boolean ephemeral) {
            this.data=data;
            this.value=value;
            this.ephemeral=ephemeral;
        }
    }
    public final boolean need_watch;
    public final String name;
    public ValueFetcher(final ZKProcess zkprocess,final String path,final boolean need_watch) {
        super(zkprocess,path);
        super.ReRequest();
        this.need_watch=need_watch;
        this.name=Paths.get(path).getFileName().toString();
    }
    public ValueFetcher(final ZKProcess zkprocess,final String name) {
        this(zkprocess,name,true);
    }
    public final String toString() {
        final Result re=this.result();
        if(re!=null) {
            if(re.data!=null) {
                return String.format("VF(name=%s,size=0)",this.name);
            } else {
                return String.format("VF(name=%s,size=%s)",this.name,re.data.length);
            }
        } else {
            return String.format("VF(name=%s,null)",this.name);
        }
    }
    public boolean DoDo(final String req) {
        final ValueFetcher value_fetcher_ref=this;
        zkprocess.zk.getData(
            req,
            need_watch?
                new Watcher() {
                    public void process(WatchedEvent event) {
                        value_fetcher_ref.ReRequest();
                    }
                }
                :null,
            new DataCallback() {
                public void processResult(
                            int rc,
                            String path,
                            Object ctx,
                            byte data[],
                            Stat stat) {
                    switch(rc) {
                    case 0:
                        try {
                            value_fetcher_ref.Done(
                                new Result(
                                    data,
                                    new String(data,"UTF-8"),
                                    stat.getEphemeralOwner()!=0
                                )
                            );
                        } catch (Exception e) {
                            DIAG.Get.d.dig_error("",e);
                            value_fetcher_ref.Done(
                                new Result(
                                    data,
                                    "",
                                    stat.getEphemeralOwner()!=0
                                )
                            );
                        } finally {
                        }
                        break;
                    default:
                        value_fetcher_ref.Done(new Result(
                            null,
                            "",
                            false
                        ));
                        zkprocess.tm.schedule(
                            new TimerTask(){
                                public void run() {
                                    value_fetcher_ref.ReRequest();
                                }
                            },
                            1000
                        );
                        break;
                    }
                }
            },
            null
        );
        return true;
    }
    public static ValueFetcher Change(final ZKProcess zkprocess,final ValueFetcher current,final String next) {
        boolean need_create=false;
        if(current!=null) {
            if(!current.request().equals(next)) {
                current.Close();
                need_create=true;
            }
        } else {
            need_create=true;
        }
        if(need_create) {
            ValueFetcher vf=new ValueFetcher(zkprocess,next,false);
            return vf;
        }
        return null;
    }
}
