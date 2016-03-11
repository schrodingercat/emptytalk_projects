package et.naruto.process.zk;

import java.nio.file.Paths;
import java.util.TimerTask;

import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;

import et.naruto.base.Util.DIAG;
import et.naruto.versioner.base.Handleable;

import static et.naruto.base.Util.Path2UnixStr;

public class ValueFetcher extends ZKProcesser<String,ValueFetcher.Result> {
    public class Result {
        public final String path;
        public final byte[] data;
        public final String value;
        public final boolean ephemeral;
        public Result(final String path,final byte[] data,final String value,final boolean ephemeral) {
            this.path=path;
            this.data=data;
            this.value=value;
            this.ephemeral=ephemeral;
        }
        public final String name() {
            return Path2UnixStr(Paths.get(path).getFileName());
        }
    }
    public final boolean need_watch;
    public ValueFetcher(final ZKProcess zkprocess,final String name) {
        this(zkprocess,name,true);
    }
    public ValueFetcher(final ZKProcess zkprocess,final String path,final boolean need_watch) {
        super(zkprocess);
        this.need_watch=need_watch;
        if(path!=null) {
            super.Request(path);
        }
    }
    public final String name() {
        if(this.request()!=null) {
            return Path2UnixStr(Paths.get(this.request()).getFileName());
        } else {
            return "";
        }
    }
    public final String toString() {
        final Result re=this.result();
        if(re!=null) {
            if(re.data!=null) {
                return String.format("VF(name=%s,size=0)",this.name());
            } else {
                return String.format("VF(name=%s,size=%s)",this.name(),re.data.length);
            }
        } else {
            return String.format("VF(name=%s,null)",this.name());
        }
    }
    public boolean DoDo(final Handleable<String> req) {
        final ValueFetcher value_fetcher_ref=this;
        zkprocess.zk.getData(
            req.result,
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
                                new Handleable(
                                    new Result(
                                        req.result,
                                        data,
                                        new String(data,"UTF-8"),
                                        stat.getEphemeralOwner()!=0
                                    ),
                                    req.versionable
                                )
                            );
                        } catch (Exception e) {
                            DIAG.Log.d.dig_error("",e);
                            value_fetcher_ref.Done(
                                new Handleable(
                                    new Result(
                                        req.result,
                                        data,
                                        "",
                                        stat.getEphemeralOwner()!=0
                                    ),
                                    req.versionable
                                )
                            );
                        } finally {
                        }
                        break;
                    default:
                        value_fetcher_ref.Done(
                            new Handleable(
                                new Result(
                                    req.result,
                                    null,
                                    "",
                                    false
                                ),
                                req.versionable
                            )
                        );
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
}
