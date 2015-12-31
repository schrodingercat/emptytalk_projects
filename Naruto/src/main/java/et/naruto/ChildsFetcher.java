package et.naruto;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.TreeSet;

import org.apache.zookeeper.AsyncCallback.ChildrenCallback;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

public class ChildsFetcher extends Processer<String,ChildsFetcher.Result> {
    public static class Result {
        public final TreeSet<String> childs;
        public final boolean exist;
        public Result(final List<String> childs,final boolean exist) {
            this.childs=new TreeSet(childs);
            this.exist=exist;
        }
    }
    public ChildsFetcher(final ZKProcess zkprocess,final String name) {
        super(zkprocess,name);
        super.ReRequest();
    }
    public boolean DoDo(final String req) {
        final ChildsFetcher childs_fetcher_ref=this;
        zkprocess.zk.getChildren(
            req,
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
                            ret.add(child);
                        }
                        childs_fetcher_ref.Done(new Result(ret,true));
                        break;
                    default:
                        childs_fetcher_ref.Done(new Result(new ArrayList(),false));
                        zkprocess.tm.schedule(
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
        return true;
    }
}
