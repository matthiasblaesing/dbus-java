package at.yawk.dbus.protocol;

import at.yawk.dbus.protocol.auth.AuthClient;
import at.yawk.dbus.protocol.codec.DbusMainProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import java.io.StringReader;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class DbusConnector {
    private final Bootstrap bootstrap;
    /**
     * The consumer to use for initial messages.
     */
    @Setter private MessageConsumer initialConsumer = MessageConsumer.DISCARD;

    public DbusConnector() {
        bootstrap = new Bootstrap();
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.config().setAutoRead(false);
            }
        });
    }

    /**
     * Connect to the dbus server at the given {@link SocketAddress}.
     */
    public DbusChannel connect(SocketAddress address) throws Exception {
        Bootstrap localBootstrap = bootstrap.clone();
        if (Epoll.isAvailable()) {
            localBootstrap.group(new EpollEventLoopGroup());
            if (address instanceof DomainSocketAddress) {
                localBootstrap.channel(EpollDomainSocketChannel.class);
            } else {
                localBootstrap.channel(EpollSocketChannel.class);
            }
        } else {
            localBootstrap.group(new NioEventLoopGroup());
            localBootstrap.channel(NioSocketChannel.class);
        }

        Channel channel = localBootstrap.connect(address).sync().channel();

        AuthClient authClient = new AuthClient();
        if (LoggingInboundAdapter.isEnabled()) {
            channel.pipeline().addLast(new LoggingInboundAdapter());
        }

        channel.pipeline().addLast("auth", authClient);
        channel.config().setAutoRead(true);

        ChannelFuture completionPromise = authClient.startAuth(channel);
        completionPromise.sync();
        channel.pipeline().remove("auth");

        SwappableMessageConsumer swappableConsumer = new SwappableMessageConsumer(initialConsumer);
        channel.pipeline().addLast(new DbusMainProtocol(swappableConsumer));

        return new DbusChannelImpl(channel, swappableConsumer);
    }

    public DbusChannel connect(DbusAddress address) throws Exception {
        log.info("Connecting to dbus server at {}", address);

        switch (address.getProtocol()) {
        case "unix":
            if (address.hasProperty("path")) {
                return connect(new DomainSocketAddress(address.getProperty("path")));
            } else if (address.hasProperty("abstract")) {
                String path = address.getProperty("abstract");

                // replace leading slash with \0 for abstract socket
                if (!path.startsWith("/")) { throw new IllegalArgumentException("Illegal abstract path " + path); }
                path = '\0' + path;

                return connect(new DomainSocketAddress(path));
            } else {
                throw new IllegalArgumentException("Neither path nor abstract given in dbus url");
            }
        default:
            throw new UnsupportedOperationException("Unsupported protocol " + address.getProtocol());
        }
    }

    public DbusChannel connectUser() throws Exception {
        String machineId = new String(Files.readAllBytes(Paths.get("/etc/machine-id"))).trim();
        String response = DbusUtil.callCommand("dbus-launch", "--autolaunch", machineId);
        Properties properties = new Properties();
        properties.load(new StringReader(response));
        String address = properties.getProperty("DBUS_SESSION_BUS_ADDRESS");
        return connect(DbusAddress.parse(address));
    }

    public DbusChannel connectSystem() throws Exception {
        // this is the default system socket location defined in dbus
        return connect(DbusAddress.fromUnixSocket(Paths.get("/run/dbus/system_bus_socket")));
    }
}
