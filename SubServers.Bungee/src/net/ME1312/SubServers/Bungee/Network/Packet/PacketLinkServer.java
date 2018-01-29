package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

/**
 * Link Server Packet
 */
public class PacketLinkServer implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private int response;
    private String message;
    private String name;

    /**
     * New PacketLinkServer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketLinkServer(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketLinkServer (Out)
     *
     * @param name The name that was determined
     * @param response Response ID
     * @param message Message
     */
    public PacketLinkServer(String name, int response, String message) {
        if (Util.isNull(response, message)) throw new NullPointerException();
        this.name = name;
        this.response = response;
        this.message = message;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("n", name);
        json.put("r", response);
        json.put("m", message);
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        try {
            Map<String, Server> servers = plugin.api.getServers();
            Server server;
            if (data.keySet().contains("name") && servers.keySet().contains(data.getString("name").toLowerCase())) {
                link(client, servers.get(data.getString("name").toLowerCase()));
            } else if ((server = searchIP(new InetSocketAddress(client.getAddress().getAddress(), data.getInt("port")))) != null) {
                link(client, server);
            } else if (data.keySet().contains("name")) {
                client.sendPacket(new PacketLinkServer(null, 2, "There is no server with that name"));
            } else {
                client.sendPacket(new PacketLinkServer(null, 2, "Could not find server with address: " + client.getAddress().getAddress().getHostAddress() + ':' + data.getInt("port")));
            }
        } catch (Exception e) {
            client.sendPacket(new PacketLinkServer(null, 1, e.getClass().getCanonicalName() + ": " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private void link(Client client, Server server) {
        if (server.getSubData() == null) {
            client.setHandler(server);
            System.out.println("SubData > " + client.getAddress().toString() + " has been defined as " + ((server instanceof SubServer) ? "SubServer" : "Server") + ": " + server.getName());
            client.sendPacket(new PacketLinkServer(server.getName(), 0, "Definition Successful"));
            if (server instanceof SubServer && !((SubServer) server).isRunning()) client.sendPacket(new PacketOutReset("Rogue SubServer Detected"));
        } else {
            client.sendPacket(new PacketLinkServer(null, 3, "Server already linked"));
        }
    }

    private Server searchIP(InetSocketAddress address) {
        Server server = null;
        for (Server s : plugin.api.getServers().values()) {
            if (s.getAddress().equals(address)) {
                if (server != null) throw new InvalidServerException("Multiple servers match address: " + address.getAddress().getHostAddress() + ':' + address.getPort());
                server = s;
            }
        }
        return server;
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
