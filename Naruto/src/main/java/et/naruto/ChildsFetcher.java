package et.naruto;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.apache.zookeeper.AsyncCallback.ChildrenCallback;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

public class ChildsFetcher extends Processer<String,List<String>> {
    public ChildsFetcher(final String name) {
        super(name);
        super.ReRequest();
    }
    public boolean Tick(final ZKProcess zk) {
        boolean changed=false;
        final ChildsFetcher childs_fetcher_ref=this;
        final String req=childs_fetcher_ref.Do();
        if(req!=null) {
            zk.zk.getChildren(
                childs_fetcher_ref.request,
                new Watcher() {
                    public void process(WatchedEvent event) {
                        childs_fetcher_ref.ReRequest();
                    }
                },
                new ChildrenCallback() {
                    public void processResult(
                            int rc,
                            String path,
                            Object ctx,
                            List<String> children) {
                        switch(rc) {
                        case 0:
                            ArrayList<String> ret=new ArrayList();
                            for(String child:children) {
                                ret.add(childs_fetcher_ref.request+"/"+child);
                            }
                            childs_fetcher_ref.Done(ret);
                            break;
                        default:
                            childs_fetcher_ref.Done(new ArrayList());
                            zk.tm.schedule(
                                new TimerTask(){
                                    public void run() {
                                        childs_fetcher_ref.ReRequest();
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
