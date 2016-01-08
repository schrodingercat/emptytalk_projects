package et.naruto.election;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;

import et.naruto.base.Util;
import et.naruto.process.ChildsFetcher;
import et.naruto.process.ValueFetcher;
import et.naruto.process.ZKProcess;
import et.naruto.versioner.Dealer;
import et.naruto.versioner.Handler;
import et.naruto.versioner.Versioner;

class Resolution {
    public final String toString() {
        return String.format("Resolution(seq=%s,data=%s,closed=%s)", seq,data==null?null:data.length,closed);
    }
    public final String name;
    public final long seq;
    public final byte[] data;
    public final boolean closed;
    public Resolution(final String name,final byte[] data,final boolean closed) {
        this.name=name;
        this.seq=Util.String2Long(name);
        this.data=data;
        this.closed=closed;
    }
}

class CurrentResolution {
    public final Dealer<Resolution> dealer=new Dealer();
    public boolean Done(
            final Handler<ChildsFetcher.Result> resolutions_fetcher,
            final Handler<ChildsFetcher.Result> resolutions_closed_fetcher,
            final ValueFetcher current_resolution_fetcher) {
        if(this.dealer.Watch(
                resolutions_fetcher.versionable(),
                resolutions_closed_fetcher.versionable(),
                current_resolution_fetcher==null?null:current_resolution_fetcher.handler().versionable())) {
            if(resolutions_fetcher.result()!=null) {
                if(resolutions_closed_fetcher.result()!=null) {
                    if(resolutions_fetcher.result().childs.size()>0) {
                        String last=resolutions_fetcher.result().childs.last();
                        boolean closed=resolutions_closed_fetcher.result().childs.contains(last);
                        byte[] data=null;
                        if(current_resolution_fetcher!=null) {
                            if(current_resolution_fetcher.name.equals(last)) {
                                if(current_resolution_fetcher.result()!=null) {
                                    data=current_resolution_fetcher.result().data;
                                }
                            }
                        }
                        this.dealer.Done(new Resolution(last,data,closed));
                    } else {
                        this.dealer.Done(new Resolution("-1",new byte[0],true));
                    }
                    return true;
                }
            }
        }
        return false;
    }
}

public class Follower {
    @Override
    public String toString() {
        return String.format("Follower(R=%s,RC=%s,current=%s)",resolutions_fetcher,resolutions_closed_fetcher,current_resolution_fetcher);
    }
    
    private final Args args;
    private final ZKProcess zkprocess;
    private final ChildsFetcher resolutions_fetcher;
    private final ChildsFetcher resolutions_closed_fetcher;
    private final Versioner resolutions_fetcher_versioner=new Versioner();
    private final Versioner resolutions_closed_fetcher_versioner=new Versioner();
    private ValueFetcher current_resolution_fetcher=null;
    
    public final ValueFetcher leader_flag_fetcher;
    public final CurrentResolution resolution_out=new CurrentResolution();
    
    public Follower(final Args args,final ZKProcess zkprocess) {
        this.args=args;
        this.zkprocess=zkprocess;
        this.leader_flag_fetcher=new ValueFetcher(this.zkprocess,args.GetLeaderPath());
        this.resolutions_fetcher=new ChildsFetcher(this.zkprocess,args.GetResolutionsPath());
        this.resolutions_closed_fetcher=new ChildsFetcher(this.zkprocess,args.GetResolutionsClosedPath());
    }
    public boolean Done() {
        boolean next=false;
        final ChildsFetcher.Result resolutions=this.resolutions_fetcher.handler().Output(resolutions_fetcher_versioner);
        if(resolutions!=null) {
            if(!resolutions.exist) {
                zkprocess.zk.create(
                    args.GetResolutionsPath(),
                    args.GetServerId().getBytes(),
                    Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT,
                    null,
                    null
                );
            } else {
                if(resolutions.childs.size()>0) {
                    ValueFetcher vf=ValueFetcher.Change(
                        this.zkprocess,
                        current_resolution_fetcher,
                        args.GetResolutionsPath()+"/"+resolutions.childs.last()
                    );
                    if(vf!=null) {
                        current_resolution_fetcher=vf;
                    }
                }
            }
            next=true;
        }
        final ChildsFetcher.Result resolutions_closed=this.resolutions_closed_fetcher.handler().Output(this.resolutions_closed_fetcher_versioner);
        if(resolutions_closed!=null) {
            if(!resolutions_closed.exist) {
                zkprocess.zk.create(
                    args.GetResolutionsClosedPath(),
                    args.GetServerId().getBytes(),
                    Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT,
                    null,
                    null
                );
            }
            next=true;
        }
        
        if(resolution_out.Done(
            resolutions_fetcher.handler(),
            resolutions_closed_fetcher.handler(),
            current_resolution_fetcher
        )) {
            next=true;
        }
        return next;
    }
}
