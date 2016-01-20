package et.naruto.resolutionsurface;

import et.naruto.base.Util;

public class Resolution {
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
