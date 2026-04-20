package borgol.ui.desktop;

/**
 * Holds the currently logged-in user for the desktop session.
 * Simple static state — one user at a time on the desktop.
 */
public class AppSession {

    private static int    userId   = 0;
    private static String username = "";

    private AppSession() {}

    public static boolean loggedIn()    { return userId > 0; }
    public static int     userId()      { return userId; }
    public static String  username()    { return username; }

    public static void login(int id, String name) {
        userId   = id;
        username = name;
    }

    public static void logout() {
        userId   = 0;
        username = "";
    }
}
