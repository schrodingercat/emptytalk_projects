package et.naruto.resolutionsurface;

public class RSArgs {
    public final String path;
    public final String token;
    public final String closed_path() {
        return path+"Closed";
    }
    public RSArgs(final String path,final String token) {
        this.path=path;
        this.token=token;
    }
    public final String toString() {
        return String.format("RSArgs(%s,%s)",path,token);
    }
}
