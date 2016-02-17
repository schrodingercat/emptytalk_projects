package et.naruto.versioner.base;

import java.util.concurrent.atomic.AtomicReference;




public class Handler<RET> {
    private final AtomicReference<Handleable<RET>> handleable_ref=new AtomicReference(new Handleable());
    public Handler() {
    }
    public final String toString() {
        return String.format("Handler(%s,ret=%s)",handleable_ref.get().versionable.toRawString(),handleable_ref.get().result);
    }
    public final Versionable versionable() {
        return handleable_ref.get().versionable;
    }
    /*public final long version() {
        return this.handleable_ref.get().version();
    }*/
    public final RET result() {
        return handleable_ref.get().result;
    }
    public final Handleable<RET> handleable() {
        return handleable_ref.get();
    }
    public final void Assign(final Handleable<RET> ret) {
        while(true) {
            Handleable current=this.handleable_ref.get();
            if(current==ret) {
                break;
            }
            if(current.versionable.Watch(ret.versionable)==null) {
                break;
            }
            this.handleable_ref.compareAndSet(current,ret);
        }
    }
    public final void Add(final RET ret) {
        Assign(Handleable.Add(ret,this.handleable_ref.get().versionable));
    }
    public final boolean Assign(final RET ret,final Versionable versionable) {
        Handleable h=Handleable.Watch(ret,this.handleable_ref.get().versionable,versionable);
        if(h!=null) {
            Assign(h);
            return true;
        }
        return false;
    }
    public final RET Sync(final Handleable<RET> handleable) {
        Handleable<RET> h=handleable.Output(this.handleable_ref.get().versionable);
        if(h!=null) {
            Assign(h);
            return h.result;
        }
        return null;
    }
}
