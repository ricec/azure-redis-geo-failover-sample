package sample.azureredis.lettuce.shared;

import org.apache.commons.lang3.StringUtils;

public class CacheSettings {
    public static String Fqdn = System.getProperty("CACHE_FQDN", StringUtils.trimToNull(System.getenv().get("CACHE_FQDN")));
    public static int Port = Integer.parseInt(System.getProperty("CACHE_PORT", StringUtils.trimToNull(System.getenv().get("CACHE_PORT"))));
    public static String AccessKey = System.getProperty("CACHE_ACCESS_KEY",
        StringUtils.trimToNull(System.getenv().get("CACHE_ACCESS_KEY")));
    public static String AlternateAccessKey = System.getProperty("CACHE_ALT_ACCESS_KEY",
        StringUtils.trimToNull(System.getenv().get("CACHE_ALT_ACCESS_KEY")));
}