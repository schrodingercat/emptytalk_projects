package et.naruto.versioner;

import et.naruto.versioner.base.Handleable;
import et.naruto.versioner.base.Handler;
import et.naruto.versioner.base.Versioner;


public class Outer<RET> {
    private final Handler<RET> set;
    private final Handler<RET> fetch;
    public Outer() {
        this.set=new Handler();
        this.fetch=new Handler();
    }
    public final String toString() {
        return String.format(
            "Outer(set=%s,fetch=%s,ret=%s)",
            set.versionable().toRawString(),
            fetch.versionable().toRawString(),
            set.result()
        );
    }
    public final Handleable<RET> set_handleable() {
        return this.set.handleable();
    }
    public final Handleable<RET> fetch_handleable() {
        return this.fetch.handleable();
    }
    public final void Add(RET ret) {
        set.Add(ret);
    }
    public final RET Fetch() {
        return fetch.Sync(set.handleable());
    }
}
