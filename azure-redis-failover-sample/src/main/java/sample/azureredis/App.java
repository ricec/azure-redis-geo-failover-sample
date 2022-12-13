package sample.azureredis;

import sample.azureredis.lettuce.clustered.LettuceClusterExample;
import sample.azureredis.lettuce.nonclustered.LettuceExample;
import sample.azureredis.lettuce.shared.CacheSettings;
import sample.azureredis.lettuce.shared.Example;

public class App
{
    public static void main(String[] args)
    {
        try (Example example = getExample())
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

    private static Example getExample() {
        if (CacheSettings.Clustered) {
            return new LettuceClusterExample(CacheSettings.Fqdn, CacheSettings.Port, CacheSettings.AccessKey, CacheSettings.AlternateAccessKey);
        }

        return new LettuceExample(CacheSettings.Fqdn, CacheSettings.Port, CacheSettings.AccessKey, CacheSettings.AlternateAccessKey);
    }
}
