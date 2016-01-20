package et.naruto.resolutionsurface;

import java.util.TimerTask;

import org.apache.zookeeper.CreateMode;

import et.naruto.process.base.Processer;
import et.naruto.process.zk.ValueRegister;
import et.naruto.process.zk.ZKProcess;
import et.naruto.versioner.Dealer;
import et.naruto.versioner.Flow;
import et.naruto.versioner.base.Handleable;
import et.naruto.versioner.base.Handler;

class RegistHandler {
    public final long seq;
    private final RSArgs args;
    private final ZKProcess zkprocess;
    private final Dealer<Boolean> dealer=new Dealer();
    
    private final ValueRegister closed;
    private final ValueRegister registed;
    private Handler<Boolean> closeing=null;
    private Handler<byte[]> registing=null;
    private boolean has_unknow=false;
    public RegistHandler(final ZKProcess zkprocess,final long seq,final RSArgs args,final RegistHandler old) {
        old.close();
        this.seq=seq;
        this.args=args;
        this.zkprocess=zkprocess;
        this.closed=new ValueRegister(zkprocess,null);
        this.registed=new ValueRegister(zkprocess,null);
    }
    public void close() {
    }
    public boolean Done(
        final Handleable<Resolution> current_resolution_handleable
    ) {
        boolean next=false;
        if(dealer.Watch(
            current_resolution_handleable.versionable,
            closed.result_versionable(),
            registed.result_versionable(),
            closeing==null?null:closeing.versionable(),
            registing==null?null:registing.versionable()
        )) {
            if(current_resolution_handleable.result.seq==this.seq) {
                Boolean result=null;
                if(registed!=null) {
                    if(registed.result()!=null) {
                        boolean is_unknow=false;
                        if(registed.result().succ!=null) {
                            if(registed.result().succ) {
                                result=true;
                            } else {
                                if(has_unknow) {
                                    is_unknow=true;
                                } else {
                                    result=false;
                                }
                            }
                        } else {
                            is_unknow=true;
                        }
                        if(is_unknow) {
                            //identify
                            if(current_resolution_handleable.result.data!=null) {
                            }
                        }
                    } else {
                        //wait
                    }
                } else {
                    result=false;
                }
                if(result!=null) {
                    dealer.Done(result);
                }
            } else {
                if(current_resolution_handleable.result.seq+1==this.seq) {
                    if(current_resolution_handleable.result.closed) {
                        if(registed.request()!=null) {
                            //go wait registed end or failed
                            if(registed.result()==null) {
                                if(registed.result().succ==null) {
                                    //exception
                                    if(!registed.doing()) {
                                        has_unknow=true;
                                        this.zkprocess.tm.schedule(
                                            new TimerTask() {
                                                public void run() {
                                                    registed.ReRequest();
                                                }
                                            },
                                            1000
                                        );
                                    }
                                } else {
                                    //failed or ok
                                }
                            }
                        } else {
                            if(registing!=null) {
                                if(this.registing.result()!=null) {
                                    this.registed.Request(new ValueRegister.Request(args.path,this.registing.result(),CreateMode.PERSISTENT));
                                }
                            } else {
                                this.registing=RegistingResolution(this.seq,current_resolution_handleable.result.data);
                            }
                        }
                    } else {
                        if(closed.request()!=null) {
                            //go wait closed end or failed
                            if(closed.result()==null) {
                                if(closed.result().succ==null) {
                                    //exception
                                    if(!closed.doing()) {
                                        this.zkprocess.tm.schedule(
                                            new TimerTask() {
                                                public void run() {
                                                    closed.ReRequest();
                                                }
                                            },
                                            1000
                                        );
                                    }
                                } else {
                                    //failed or ok
                                }
                            }
                        } else {
                            if(closeing!=null) {
                                if(this.closeing.result()!=null) {
                                    closed.Request(new ValueRegister.Request(args.closed_path(),args.token,CreateMode.PERSISTENT));
                                } else {
                                    //wait closeing end or failed
                                }
                            } else {
                                this.closeing=CloseingResolution(current_resolution_handleable.result.seq,current_resolution_handleable.result.data);
                            }
                        }
                    }
                } else {
                    //exception
                }
            }
        }
        return next;
    }
    public Handler<Boolean> CloseingResolution(long seq,byte[] data) {
        return null;
    }
    public Handler<byte[]> RegistingResolution(long seq,byte[] data) {
        return null;
    }
}

class RSArgs {
    public final String path;
    public final String token;
    public final String closed_path() {
        return path+"/Closed";
    }
    public RSArgs(final String path,final String token) {
        this.path=path;
        this.token=token;
    }
}

public class ResolutionSurface implements Processer ,AutoCloseable {
    private static class Request {
        public final byte[] data;
        public final long seq;
        public Request(final byte[] data,final long seq) {
            this.data=data;
            this.seq=seq;
        }
    }
    private final Flow<Request,Boolean> flow=new Flow();
    
    private final RSArgs args;
    private final ZKProcess zkprocess;
    private final ResolutionSync resolution_sync;
    private RegistHandler regist_handler=null;
    public ResolutionSurface(final ZKProcess zkprocess,final RSArgs args) {
        this.args=args;
        this.zkprocess=zkprocess;
        this.resolution_sync=new ResolutionSync(args.path,args.token,zkprocess);
        this.zkprocess.AddProcesser(this);
    }
    public void close() {
        this.zkprocess.DelProcesser(this);
        this.resolution_sync.close();
        if(this.regist_handler!=null) {
            this.regist_handler.close();
        }
    }
    public void Regist(final Request request) {
        this.flow.AddIn(request);
    }
    public boolean Do() {
        boolean next=false;
        
        final Request request=this.flow.NeedDoing();
        if(request!=null) {
            final Handleable<Resolution> current_resolution=resolution_sync.current_Resolution_handleable();
            if(current_resolution.result!=null) {
                if(current_resolution.result.seq+1==request.seq) {
                    boolean need_new_handler=false;
                    if(regist_handler!=null) {
                        if(regist_handler.seq!=request.seq) {
                            need_new_handler=true;
                        }
                    } else {
                        need_new_handler=true;
                    }
                    if(need_new_handler) {
                        regist_handler=new RegistHandler(this.zkprocess,request.seq,args,regist_handler);
                    }
                }
            }
            next=true;
        }
        
        //if regist_handler done
        //endif
        return next;
    }
}
