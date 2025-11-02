package info.gabrielszabo.ublog;

import info.gabrielszabo.jdi.services.ServiceFactory;
import info.gabrielszabo.jdi.services.impl.ServiceFactoryImpl;
import info.gabrielszabo.ublog.config.ConfigServiceImpl;
import info.gabrielszabo.ublog.server.Server;

public class App {

    public static final ServiceFactory serviceFactory = new ServiceFactoryImpl(ConfigServiceImpl.INSTANCE);

    private App() {
        serviceFactory.getServiceImpl(Server.class);;
    }

    public static void main(String[] args) {
        new App();
    }
}