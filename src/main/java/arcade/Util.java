package arcade;

/**
 * Created by wryan on 8/29/2015.
 */
public class Util {
    public static boolean isBlankOrNull(String s) {
        if (s==null) return true;
        if (s.equals("")) return true;
        return false;
    }
}
