package sample.azureredis.lettuce.clustered;

import sample.azureredis.lettuce.shared.Example;

public class LettuceClusterExample extends Example
{
    private ClusterConnectionProvider connectionProvider;

    public LettuceClusterExample(String hostname, int port, String password, String secondaryPassword)
    {
        this.connectionProvider = new ClusterConnectionProvider(hostname, port, password, secondaryPassword);
    }

    @Override
    public void executeWrite() {
        connectionProvider.getConnection().sync().set("key", "Hello, Redis!");
    }

    @Override
    public void close()
    {
        try {
            if (this.connectionProvider != null) {
                this.connectionProvider.close();
            }
        }
        finally {
            this.connectionProvider = null;
        }
    }
}
