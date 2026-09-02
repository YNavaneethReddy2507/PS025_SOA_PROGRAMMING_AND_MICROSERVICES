package com.auction.eureka;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = EurekaServerApplication.class)
class EurekaServiceRegistrationTest {

    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private PeerAwareInstanceRegistry instanceRegistry;

    @Autowired(required = false)
    private EurekaServerConfig eurekaServerConfig;

    @Test
    @DisplayName("Verify Eureka Server starts on port 8761 and self-preservation configuration is applied")
    void testEurekaServerConfiguration() {
        String serverPort = environment.getProperty("server.port");
        assertEquals("8761", serverPort, "Eureka server port must be 8761");

        String appName = environment.getProperty("spring.application.name");
        assertEquals("eureka-server", appName, "Eureka server application name must be eureka-server");

        assertNotNull(instanceRegistry, "PeerAwareInstanceRegistry bean must be loaded in Eureka context");
        assertNotNull(eurekaServerConfig, "EurekaServerConfig must be loaded");
        assertFalse(eurekaServerConfig.shouldEnableSelfPreservation(), "Self-preservation should be disabled for rapid eviction in development");
    }

    @Test
    @DisplayName("Verify dynamic registration and discovery of all required microservices")
    void testServiceRegistrationAndDiscovery() {
        assertNotNull(instanceRegistry, "Instance registry must be available");

        List<String> requiredServices = List.of(
                "API-GATEWAY",
                "AUTH-SERVICE",
                "AUCTION-SERVICE",
                "BIDDING-SERVICE",
                "PAYMENT-SERVICE"
        );

        int portOffset = 8080;
        for (String serviceName : requiredServices) {
            InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder()
                    .setAppName(serviceName)
                    .setInstanceId(serviceName.toLowerCase() + ":127.0.0.1:" + portOffset)
                    .setHostName("127.0.0.1")
                    .setIPAddr("127.0.0.1")
                    .setPort(portOffset++)
                    .setStatus(InstanceInfo.InstanceStatus.UP)
                    .setDataCenterInfo(new MyDataCenterInfo(DataCenterInfo.Name.MyOwn))
                    .build();

            instanceRegistry.register(instanceInfo, false);
        }

        for (String serviceName : requiredServices) {
            Application registeredApp = instanceRegistry.getApplication(serviceName);
            assertNotNull(registeredApp, "Application " + serviceName + " must be registered in Eureka");
            assertFalse(registeredApp.getInstances().isEmpty(), "Application " + serviceName + " must have active registered instances");

            InstanceInfo registeredInstance = registeredApp.getInstances().get(0);
            assertEquals(serviceName, registeredInstance.getAppName(), "AppName must match registered name");
            assertEquals(InstanceInfo.InstanceStatus.UP, registeredInstance.getStatus(), "Status must be UP");
        }
    }
}
