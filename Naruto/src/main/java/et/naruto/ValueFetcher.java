package et.naruto;

import java.util.TimerTask;

import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;

import et.naruto.Util.DIAG;

public class ValueFetcher extends Processer<String,ValueFetcher.Result> {
    public class Result {
        public final String value;
        public final boolean ephemeral;
        public Result(final String value,final boolean ephemeral) {
            this.value=value;
            this.ephemeral=ephemeral;
        }
    }
    public ValueFetcher(final String name) {
        super(name);
        super.ReRequest();
    }
    public boolean Tick(final ZKProcess zk) {
        boolean changed=false;
        final ValueFetcher value_fetcher_ref=this;
        final String name=value_fetcher_ref.Do();
        if(name!=null) {
            zk.zk.getData(
                name,
                new Watcher() {
                    public void process(WatchedEvent event) {
                        value_fetcher_ref.ReRequest();
                    }
                },
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
                                        new String(data,"UTF-8"),
                                        stat.getEphemeralOwner()!=0
                                    )
                                );
                            } catch (Exception e) {
                                DIAG.Get.d.dig_error("",e);
                                value_fetcher_ref.Done(
                                    new Result(
                                        "",
                                        stat.getEphemeralOwner()!=0
                                    )
                                );
                            } finally {
                            }
                            break;
                        default:
                            value_fetcher_ref.Done(new Result(
                                "",
                                false
                            ));
                            zk.tm.schedule(
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
            changed=true;
        }
        return changed;
    }
}
