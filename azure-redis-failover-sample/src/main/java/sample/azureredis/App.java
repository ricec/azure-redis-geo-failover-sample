package sample.azureredis;

import sample.azureredis.lettuce.nonclustered.LettuceExample;
import sample.azureredis.lettuce.shared.CacheSettings;
import sample.azureredis.lettuce.shared.Example;

public class App
{
    public static void main(String[] args)
    {
        String hostname = CacheSettings.Fqdn;
        int port = CacheSettings.Port;
        String password = CacheSettings.AccessKey;
        String secondaryPassword = CacheSettings.AlternateAccessKey;

        try (Example example = new LettuceExample(hostname, port, password, secondaryPassword))
        {
            example.run();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        finally {
            System.exit(0);
        }
    }
}
