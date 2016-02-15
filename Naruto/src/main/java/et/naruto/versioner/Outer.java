package et.naruto.versioner;

import et.naruto.versioner.base.Handleable;
import et.naruto.versioner.base.Handler;
import et.naruto.versioner.base.Versionable;
import et.naruto.versioner.base.Versioner;


public class Outer<RET> {
    private final Handler<RET> set;
    private final Versioner fetch;
    public Outer() {
        this.set=new Handler();
        this.fetch=new Versioner();
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
    public final Versionable fetch_versionable() {
        return this.fetch.versionable();
    }
    public final void Add(RET ret) {
        set.Add(ret);
    }
    public final RET Fetch() {
        return fetch.Fetch(this.set.handleable());
    }
}
