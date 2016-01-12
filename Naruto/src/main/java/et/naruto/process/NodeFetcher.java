package et.naruto.process;

import java.util.TreeSet;

import org.apache.zookeeper.CreateMode;

import et.naruto.versioner.Dealer;

public class NodeFetcher implements Processer {
    public static class Result {
        public final TreeSet<String> childs;
        public Result(final TreeSet<String> childs) {
            this.childs=childs;
        }
        public final boolean exist() {
            return childs!=null;
        }
    }
    private final ZKProcess zkprocess;
    public final String path;
    private final ChildsFetcher childs_fetcher;
    public NodeFetcher(final ZKProcess zkprocess,final String path) {
        this.zkprocess=zkprocess;
        this.path=path;
        this.childs_fetcher=new ChildsFetcher(zkprocess,path);
        this.zkprocess.AddProcesser(this);
    }
    public void Close() {
        this.childs_fetcher.Close();
        this.zkprocess.DelProcesser(this);
    }
    
    private final Dealer<Result> dealer=new Dealer();
    public final boolean Do() {
        if(dealer.Watch(
            childs_fetcher.handler().versionable()
        )) {
            TreeSet<String> childs=null;
            if(this.childs_fetcher.result()!=null) {
                if(this.childs_fetcher.result().exist) {
                    if(this.childs_fetcher.result().childs!=null) {
                        childs=this.childs_fetcher.result().childs;
                    }
                }
            }
            this.dealer.Done(new Result(childs));
            return true;
        }
        return false;
    }
    
}

