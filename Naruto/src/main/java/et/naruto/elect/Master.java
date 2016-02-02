package et.naruto.elect;

import et.naruto.election.Args;
import et.naruto.process.base.Processer;
import et.naruto.process.zk.ZKProcess;
import et.naruto.resolutionsurface.RSArgs;
import et.naruto.resolutionsurface.ResolutionSurface;
import et.naruto.seat.Seat;
import et.naruto.versioner.Dealer;

public class Master implements Processer, AutoCloseable {
    private final Seat pre_master_seat;
    private final ResolutionSurface resolution_surface;
    private final ZKProcess zkprocess;
    private final Dealer<Boolean> dealer=new Dealer();
    public Master(final Args args,final ZKProcess zkprocess) {
        this.pre_master_seat=new Seat(args.GetLeaderPath(),args.GetServerId(),zkprocess);
        this.resolution_surface=new ResolutionSurface(zkprocess,new RSArgs(args.GetResolutionsPath(),args.GetServerId()));
        this.zkprocess=zkprocess;
        zkprocess.AddProcesser(this);
    }
    public final void close() {
        this.zkprocess.DelProcesser(this);
        this.resolution_surface.close();
        this.pre_master_seat.close();
    }
    public final boolean Do() {
        boolean next=false;
        if(dealer.Watch(
            pre_master_seat.result_handleable(),
            resolution_surface.out_handleable()
        )){
            boolean is_pre_leader=false;
            if(pre_master_seat.result_handleable().result!=null) {
                if(pre_master_seat.result_handleable().result) {
                    is_pre_leader=true;
                }
            }
            
            if(is_pre_leader) {
                Integer next_seq=null;
                if(resolution_surface.current_resolution_handleable().result!=null) {
                    resolution_surface.current_resolution_handleable().result.seq;
                }
            }
            next=true;
        }
        return next;
    }
}
