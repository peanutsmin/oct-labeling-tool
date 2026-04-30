public class I18n {
    private String lang = "en";

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getLang() {
        return lang;
    }

    public String t(String ko, String en, String de) {
        if (lang.equals("ko")) return ko;
        if (lang.equals("de")) return de;
        return en;
    }
}