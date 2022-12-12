package sample.azureredis.lettuce.nonclustered;

import sample.azureredis.lettuce.shared.Example;

public class LettuceExample extends Example
{
    private ConnectionProvider connectionProvider;

    public LettuceExample(String hostname, int port, String password, String secondaryPassword)
    {
        this.connectionProvider = new ConnectionProvider(hostname, port, password, secondaryPassword);
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
