package et.naruto.resolutionsurface;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;

import et.naruto.process.base.Processer;
import et.naruto.process.zk.ChildsFetcher;
import et.naruto.process.zk.ValueFetcher;
import et.naruto.process.zk.ZKProcess;
import et.naruto.versioner.Dealer;
import et.naruto.versioner.base.Handleable;
import et.naruto.versioner.base.Versioner;

class CurrentResolutionGenerator {
    public final Dealer<Resolution> dealer=new Dealer();
    public boolean Done(
            final Handleable<ChildsFetcher.Result> resolutions_fetcher,
            final Handleable<ChildsFetcher.Result> resolutions_closed_fetcher,
            final ValueFetcher current_resolution_fetcher) {
        if(this.dealer.Watch(
                resolutions_fetcher.versionable,
                resolutions_closed_fetcher.versionable,
                current_resolution_fetcher==null?null:current_resolution_fetcher.result_versionable())) {
            if(resolutions_fetcher.result!=null) {
                if(resolutions_closed_fetcher.result!=null) {
                    if(resolutions_fetcher.result.childs.size()>0) {
                        String last=resolutions_fetcher.result.childs.last();
                        boolean closed=resolutions_closed_fetcher.result.childs.contains(last);
                        byte[] data=null;
                        if(current_resolution_fetcher!=null) {
                            if(current_resolution_fetcher.name().equals(last)) {
                                if(current_resolution_fetcher.result()!=null) {
                                    data=current_resolution_fetcher.result().data;
                                }
                            }
                        }
                        this.dealer.Done(new Resolution(last,data,closed));
                    } else {
                        this.dealer.Done(new Resolution("-1",new byte[0],true));
                    }
                }
            }
            return true;
        }
        return false;
    }
}

public class ResolutionSync implements Processer, AutoCloseable {
    @Override
    public String toString() {
        return String.format("ResolutionSeqSync(R=%s,RC=%s,current=%s)",resolutions_fetcher,resolutions_closed_fetcher,current_resolution_fetcher);
    }
    
    private final String resolutions_path;
    private final String resolutions_closed_path() {return resolutions_path+"Closed";}
    private final String token;
    private final ZKProcess zkprocess;
    private final ChildsFetcher resolutions_fetcher;
    private final ChildsFetcher resolutions_closed_fetcher;
    private final Versioner resolutions_fetcher_versioner=new Versioner();
    private final Versioner resolutions_closed_fetcher_versioner=new Versioner();
    private final ValueFetcher current_resolution_fetcher;
    
    private final CurrentResolutionGenerator current_resolution_generator=new CurrentResolutionGenerator();
    
    public final Handleable<Resolution> current_Resolution_handleable() {
        return this.current_resolution_generator.dealer.result_handleable();
    }
    
    public ResolutionSync(final String resolutions_path,final String token,final ZKProcess zkprocess) {
        this.resolutions_path=resolutions_path;
        this.token=token;
        this.zkprocess=zkprocess;
        this.resolutions_fetcher=new ChildsFetcher(this.zkprocess,resolutions_path);
        this.resolutions_closed_fetcher=new ChildsFetcher(this.zkprocess,resolutions_closed_path());
        this.current_resolution_fetcher=new ValueFetcher(this.zkprocess,null,false);
        this.zkprocess.AddProcesser(this);
    }
    public void close() {
        this.zkprocess.DelProcesser(this);
        this.resolutions_fetcher.Close();
        this.resolutions_closed_fetcher.Close();
        if(this.current_resolution_fetcher!=null) {
            this.current_resolution_fetcher.Close();
        }
    }
    public boolean Do() {
        boolean next=false;
        final ChildsFetcher.Result resolutions=this.resolutions_fetcher_versioner.Fetch(this.resolutions_fetcher.handleable());
        if(resolutions!=null) {
            if(!resolutions.exist) {
                zkprocess.zk.create(
                    this.resolutions_path,
                    this.token.getBytes(),
                    Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT,
                    null,
                    null
                );
            } else {
                if(resolutions.childs.size()>0) {
                    String last_path=this.resolutions_path+"/"+resolutions.childs.last();
                    if((current_resolution_fetcher.request()!=null)||(!current_resolution_fetcher.request().equals(last_path))) {
                        current_resolution_fetcher.Request(last_path);
                    }
                }
            }
            next=true;
        }
        final ChildsFetcher.Result resolutions_closed=this.resolutions_closed_fetcher_versioner.Fetch(this.resolutions_closed_fetcher.handleable());
        if(resolutions_closed!=null) {
            if(!resolutions_closed.exist) {
                zkprocess.zk.create(
                    this.resolutions_closed_path(),
                    this.token.getBytes(),
                    Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT,
                    null,
                    null
                );
            }
            next=true;
        }
        
        if(current_resolution_generator.Done(
            resolutions_fetcher.handleable(),
            resolutions_closed_fetcher.handleable(),
            current_resolution_fetcher
        )) {
            next=true;
        }
        return next;
    }
}
