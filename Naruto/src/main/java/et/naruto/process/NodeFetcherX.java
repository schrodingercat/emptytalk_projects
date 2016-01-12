package et.naruto.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import et.naruto.versioner.Dealer;
import et.naruto.versioner.Versionable;

public abstract class NodeFetcherX<X extends NodeFetcherX.Target> implements Processer {
    public static interface Target {
        public String name();
        public void Close();
    }
    private final ZKProcess zkprocess;
    public final ChildsFetcher childs_fetcher;
    public NodeFetcherX(final ZKProcess zkprocess,final String path) {
        this.zkprocess=zkprocess;
        this.childs_fetcher=new ChildsFetcher(zkprocess,path);
        this.zkprocess.AddProcesser(this);
    }
    public void Close() {
        childs_fetcher.Close();
        if(dealer.result()!=null) {
            for(X x:dealer.result().values()) {
                x.Close();
            }
        }
        this.zkprocess.DelProcesser(this);
    }
    public final Versionable result_versionable() {
        return dealer.result_versionable();
    }
    
    public final Dealer<HashMap<String,X>> dealer=new Dealer();
    private ValueRegister registers_register=null;
    public boolean Do() {
        if(dealer.Watch(
            this.childs_fetcher.result_versionable()
        )) {
            if(this.childs_fetcher.result()!=null) {
                if(this.childs_fetcher.result().childs!=null) {
                    dealer.Done(MergeNodes(this.childs_fetcher.result().childs));
                }
            }
            return true;
        }
        return false;
    }
    private final HashMap<String,X> MergeNodes(final TreeSet<String> childs) {
        List<String> add=new ArrayList();
        List<X> remove=new ArrayList();
        HashMap<String,X> leave=new HashMap();
        for(final String child:childs) {
            if(dealer.result()!=null) {
                if(!dealer.result().containsKey(child)) {
                    add.add(child);
                }
            } else {
                add.add(child);
            }
        }
        if(dealer.result()!=null) {
            for(final X ns:dealer.result().values()) {
                if(!childs.contains(ns.name())) {
                    remove.add(ns);
                } else {
                    leave.put(ns.name(),ns);
                }
            }
        }
        for(final X ns:remove) {
            ns.Close();
        }
        for(final String child:add) {
            leave.put(child,DoCreateX(child));
        }
        return leave;
    }
    public abstract X DoCreateX(String child);
}
