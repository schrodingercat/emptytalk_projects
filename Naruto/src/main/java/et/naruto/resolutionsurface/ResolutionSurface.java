package et.naruto.resolutionsurface;

import java.util.TimerTask;

import org.apache.zookeeper.CreateMode;

import et.naruto.base.Util;
import et.naruto.process.base.Processer;
import et.naruto.process.zk.ValueRegister;
import et.naruto.process.zk.ZKProcess;
import et.naruto.versioner.Dealer;
import et.naruto.versioner.Flow;
import et.naruto.versioner.base.Handleable;
import et.naruto.versioner.base.Handler;
import et.naruto.versioner.base.Versioner;

class RegistHandler {
    public final long seq;
    private final RSArgs args;
    private final ZKProcess zkprocess;
    private final Dealer<Boolean> dealer;
    
    private final ValueRegister closed;
    private final ValueRegister registed;
    private Handler<Boolean> closeing=null;
    private Handler<byte[]> registing=null;
    private boolean has_unknow=false;
    public RegistHandler(final ZKProcess zkprocess,final long seq,final RSArgs args,final RegistHandler old) {
        if(old!=null) {
            old.close();
            this.dealer=old.dealer;
        } else {
            this.dealer=new Dealer();
        }
        this.seq=seq;
        this.args=args;
        this.zkprocess=zkprocess;
        this.closed=new ValueRegister(zkprocess,null);
        this.registed=new ValueRegister(zkprocess,null);
    }
    public final Handleable<Boolean> result_handleable() {
        return dealer.result_handleable();
    }
    public void close() {
    }
    public boolean Done(
        final Handleable<Resolution> current_resolution_handleable
    ) {
        if(dealer.result()!=null) {
            return false;
        }
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
                                if(current_resolution_handleable.result.data.getToken().equals(this.args.token)) {
                                    result=true;
                                }
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
                                    this.registed.Request(new ValueRegister.Request(args.path+"/"+Util.Long2String(this.seq),new Data(this.registing.result(),this.args.token).data,CreateMode.PERSISTENT));
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
                                    closed.Request(new ValueRegister.Request(args.closed_path()+"/"+Util.Long2String(current_resolution_handleable.result.seq),args.token,CreateMode.PERSISTENT));
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
    public Handler<Boolean> CloseingResolution(final long seq,final Data data) {
        Handler<Boolean> r=new Handler();
        r.Add(true);
        return r;
    }
    public Handler<byte[]> RegistingResolution(final long seq,final Data data) {
        Handler<byte[]> r=new Handler();
        r.Add("test".getBytes());
        return r;
    }
}


public class ResolutionSurface implements Processer ,AutoCloseable {
    public static class Request {
        public final byte[] data;
        public final long seq;
        public Request(final byte[] data,final long seq) {
            this.data=data;
            this.seq=seq;
        }
    }
    private final Flow<Request,Boolean> flow=new Flow();
    private final Versioner regist_handler_check=new Versioner();
    
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
    public final Handleable<Boolean> out_handleable() {
        return flow.out_handleable();
    }
    public final Handleable<Resolution> current_resolution_handleable() {
        return this.resolution_sync.current_resolution_handleable();
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
            final Handleable<Resolution> current_resolution=resolution_sync.current_resolution_handleable();
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
                } else {
                    this.flow.Out(result);
                }
            }
            next=true;
        }
        if(regist_handler!=null) {
            if(regist_handler.Done(resolution_sync.current_resolution_handleable())) {
                next=true;
            }
            Boolean result=regist_handler_check.Fetch(regist_handler.result_handleable());
            if(result!=null) {
                this.flow.Out(result);
                next=true;
            }
        }
        return next;
    }
}
