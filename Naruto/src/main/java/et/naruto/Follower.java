package et.naruto;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;

class Resolution {
    public final String name;
    public final long seq;
    public final byte[] data;
    public final boolean closed;
    public Resolution(final String name,final byte[] data,final boolean closed) {
        this.name=name;
        this.seq=Long.valueOf(name);
        this.data=data;
        this.closed=closed;
    }
}


public class Follower {
    @Override
    public String toString() {
        return "Server [args=" + args + "]";
    }
    
    private final Args args;
    private final ZKProcess zkprocess;
    private final ChildsFetcher resolutions_fetcher;
    private final ChildsFetcher resolutions_closed_fetcher;
    private ValueFetcher current_resolution_fetcher=null;
    
    public final ValueFetcher leader_flag_fetcher;
    public final FlowOut<Resolution> resolution_out=new FlowOut();
    
    private Resolution GetCurrentResolution() {
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
                    return new Resolution(last,data,closed);
                } else {
                    return new Resolution("-1",null,true);
                }
            }
        }
        return null;
    }
    public Follower(final Args args,final ZKProcess zkprocess) {
        this.args=args;
        this.zkprocess=zkprocess;
        this.leader_flag_fetcher=new ValueFetcher(this.zkprocess,args.GetLeaderPath());
        this.resolutions_fetcher=new ChildsFetcher(this.zkprocess,args.GetResolutionsPath());
        this.resolutions_closed_fetcher=new ChildsFetcher(this.zkprocess,args.GetResolutionsClosedPath());
    }
    public void Follower_DoRun() {
        {
            final ChildsFetcher.Result resolutions=this.resolutions_fetcher.Fetch();
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
                resolution_out.AddIn();
            }
            final ChildsFetcher.Result resolutions_closed=this.resolutions_closed_fetcher.Fetch();
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
                resolution_out.AddIn();
            }
            if(current_resolution_fetcher!=null) {
                ValueFetcher.Result current_resolution=current_resolution_fetcher.Fetch();
                if(current_resolution!=null) {
                    resolution_out.AddIn();
                }
            }
        }
        if(resolution_out.Doing()) {
            Resolution resolution=GetCurrentResolution();
            if(resolution!=null) {
                resolution_out.Out(resolution);
            }
        }
    }
}
